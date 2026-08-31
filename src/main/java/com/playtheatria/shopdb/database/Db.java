package com.playtheatria.shopdb.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the single SQLite connection. SQLite serializes writes internally; all
 * repository access additionally synchronizes on {@link #lock} so one plugin-wide
 * connection is safe across the HTTP server's worker threads.
 *
 * Table and column names are identical to the schema the previous (Hibernate)
 * backend used, so a dump of the old database imports 1:1.
 */
public class Db implements AutoCloseable {
    public final Connection connection;
    public final Object lock = new Object();

    public Db(File file) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        createTables();
    }

    private void createTables() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS player (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name VARCHAR NOT NULL UNIQUE)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS region (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name VARCHAR NOT NULL, " +
                    "server VARCHAR NOT NULL, " +
                    "location_type VARCHAR NOT NULL DEFAULT 'MARKET_STALL', " +
                    "external_id VARCHAR, " +
                    "i_x INTEGER, i_y INTEGER, i_z INTEGER, " +
                    "o_x INTEGER, o_y INTEGER, o_z INTEGER, " +
                    "active INTEGER, " +
                    "last_updated INTEGER)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS region_mayors (" +
                    "towns_id INTEGER NOT NULL, " +
                    "mayors_id INTEGER NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS chest_shop_sign (" +
                    "id VARCHAR PRIMARY KEY, " +
                    "server VARCHAR NOT NULL, " +
                    "x INTEGER, y INTEGER, z INTEGER, " +
                    "material VARCHAR NOT NULL, " +
                    "owner_id INTEGER, " +
                    "town_id INTEGER, " +
                    "quantity INTEGER, " +
                    "quantity_available INTEGER, " +
                    "buy_price REAL, " +
                    "sell_price REAL, " +
                    "buy_price_each REAL, " +
                    "sell_price_each REAL, " +
                    "is_full INTEGER, " +
                    "is_hidden INTEGER, " +
                    "is_buy_sign INTEGER, " +
                    "is_sell_sign INTEGER, " +
                    "base_material VARCHAR, " +
                    "item_details VARCHAR, " +
                    "display_name_plain VARCHAR)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username VARCHAR, " +
                    "password VARCHAR)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_css_material ON chest_shop_sign (material)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_css_owner ON chest_shop_sign (owner_id)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_css_town ON chest_shop_sign (town_id)");
        }
        // Columns introduced after 4.0.0; CREATE TABLE IF NOT EXISTS won't add
        // them to a database created by an earlier version.
        addColumnIfMissing("chest_shop_sign", "base_material", "VARCHAR");
        addColumnIfMissing("chest_shop_sign", "item_details", "VARCHAR");
        if (addColumnIfMissing("chest_shop_sign", "display_name_plain", "VARCHAR")) {
            backfillDisplayNames();
        }
        addColumnIfMissing("region", "location_type", "VARCHAR NOT NULL DEFAULT 'MARKET_STALL'");
        addColumnIfMissing("region", "external_id", "VARCHAR");
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("UPDATE region SET external_id = name " +
                    "WHERE external_id IS NULL OR external_id = ''");
        }
        deduplicateRegionIdentities();
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_css_display_name " +
                    "ON chest_shop_sign (display_name_plain)");
        }
    }

    /**
     * Collapses duplicate identities that may have been created before the
     * identity index became unique. The lowest row id remains canonical so
     * existing references stay as stable as possible. Mutable location data
     * comes from the most recently updated row, publication is preserved if
     * any duplicate was active, and all shop/owner relationships are merged.
     */
    private void deduplicateRegionIdentities() throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        if (previousAutoCommit) connection.setAutoCommit(false);

        try {
            java.util.List<RegionIdentity> duplicates = new java.util.ArrayList<>();
            try (java.sql.PreparedStatement ps = connection.prepareStatement(
                    "SELECT server, location_type, lower(external_id) AS identity " +
                            "FROM region GROUP BY server, location_type, lower(external_id) " +
                            "HAVING COUNT(*) > 1");
                 java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    duplicates.add(new RegionIdentity(
                            rs.getString("server"),
                            rs.getString("location_type"),
                            rs.getString("identity")));
                }
            }

            for (RegionIdentity identity : duplicates) {
                java.util.List<DuplicateRegion> rows = new java.util.ArrayList<>();
                try (java.sql.PreparedStatement ps = connection.prepareStatement(
                        "SELECT id, name, external_id, i_x, i_y, i_z, o_x, o_y, o_z, " +
                                "active, last_updated FROM region " +
                                "WHERE server = ? AND location_type = ? " +
                                "AND lower(external_id) = ? " +
                                "ORDER BY CASE WHEN last_updated IS NULL THEN 1 ELSE 0 END, " +
                                "last_updated DESC, id ASC")) {
                    ps.setString(1, identity.server());
                    ps.setString(2, identity.locationType());
                    ps.setString(3, identity.externalIdLower());
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) rows.add(DuplicateRegion.from(rs));
                    }
                }
                if (rows.size() < 2) continue;

                long canonicalId = rows.stream().mapToLong(DuplicateRegion::id).min().orElseThrow();
                DuplicateRegion freshest = rows.get(0);
                Boolean mergedActive = rows.stream().anyMatch(r -> Boolean.TRUE.equals(r.active()))
                        ? Boolean.TRUE
                        : rows.stream().anyMatch(r -> Boolean.FALSE.equals(r.active()))
                        ? Boolean.FALSE : null;

                try (java.sql.PreparedStatement ps = connection.prepareStatement(
                        "UPDATE region SET name = ?, external_id = ?, i_x = ?, i_y = ?, i_z = ?, " +
                                "o_x = ?, o_y = ?, o_z = ?, active = ?, last_updated = ? WHERE id = ?")) {
                    ps.setString(1, freshest.name());
                    ps.setString(2, freshest.externalId());
                    ps.setInt(3, freshest.iX());
                    ps.setInt(4, freshest.iY());
                    ps.setInt(5, freshest.iZ());
                    ps.setInt(6, freshest.oX());
                    ps.setInt(7, freshest.oY());
                    ps.setInt(8, freshest.oZ());
                    setNullableBoolean(ps, 9, mergedActive);
                    setNullableLong(ps, 10, freshest.lastUpdated());
                    ps.setLong(11, canonicalId);
                    ps.executeUpdate();
                }

                for (DuplicateRegion row : rows) {
                    if (row.id() == canonicalId) continue;
                    try (java.sql.PreparedStatement ps = connection.prepareStatement(
                            "UPDATE chest_shop_sign SET town_id = ? WHERE town_id = ?")) {
                        ps.setLong(1, canonicalId);
                        ps.setLong(2, row.id());
                        ps.executeUpdate();
                    }
                    try (java.sql.PreparedStatement ps = connection.prepareStatement(
                            "UPDATE region_mayors SET towns_id = ? WHERE towns_id = ?")) {
                        ps.setLong(1, canonicalId);
                        ps.setLong(2, row.id());
                        ps.executeUpdate();
                    }
                    try (java.sql.PreparedStatement ps = connection.prepareStatement(
                            "DELETE FROM region WHERE id = ?")) {
                        ps.setLong(1, row.id());
                        ps.executeUpdate();
                    }
                }

                // Merging relationship rows can produce duplicates when the
                // same owner was present on more than one duplicate region.
                try (java.sql.PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM region_mayors WHERE towns_id = ? AND rowid NOT IN (" +
                                "SELECT MIN(rowid) FROM region_mayors WHERE towns_id = ? GROUP BY mayors_id)")) {
                    ps.setLong(1, canonicalId);
                    ps.setLong(2, canonicalId);
                    ps.executeUpdate();
                }
            }

            // A previous development build created this as a non-unique index.
            // Recreate it in the same transaction as deduplication so a failed
            // migration cannot leave a half-applied identity invariant.
            try (Statement s = connection.createStatement()) {
                s.executeUpdate("DROP INDEX IF EXISTS idx_region_identity");
                s.executeUpdate("CREATE UNIQUE INDEX idx_region_identity " +
                        "ON region (server, location_type, external_id COLLATE NOCASE)");
            }

            if (previousAutoCommit) connection.commit();
        } catch (SQLException | RuntimeException e) {
            if (previousAutoCommit) connection.rollback();
            throw e;
        } finally {
            if (previousAutoCommit) connection.setAutoCommit(true);
        }
    }

    private static void setNullableBoolean(java.sql.PreparedStatement ps, int index, Boolean value)
            throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.INTEGER);
        else ps.setInt(index, value ? 1 : 0);
    }

    private static void setNullableLong(java.sql.PreparedStatement ps, int index, Long value)
            throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.INTEGER);
        else ps.setLong(index, value);
    }

    private record RegionIdentity(String server, String locationType, String externalIdLower) {
    }

    private record DuplicateRegion(long id, String name, String externalId,
                                   int iX, int iY, int iZ, int oX, int oY, int oZ,
                                   Boolean active, Long lastUpdated) {
        private static DuplicateRegion from(java.sql.ResultSet rs) throws SQLException {
            int activeValue = rs.getInt("active");
            Boolean active = rs.wasNull() ? null : activeValue != 0;
            long updatedValue = rs.getLong("last_updated");
            Long lastUpdated = rs.wasNull() ? null : updatedValue;
            return new DuplicateRegion(
                    rs.getLong("id"), rs.getString("name"), rs.getString("external_id"),
                    rs.getInt("i_x"), rs.getInt("i_y"), rs.getInt("i_z"),
                    rs.getInt("o_x"), rs.getInt("o_y"), rs.getInt("o_z"),
                    active, lastUpdated);
        }
    }

    /** Returns true when the column was added by this call. */
    private boolean addColumnIfMissing(String table, String column, String type) throws SQLException {
        try (Statement s = connection.createStatement();
             java.sql.ResultSet rs = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return false;
            }
        }
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
        return true;
    }

    /** Derives display_name_plain from any item_details captured before the column existed. */
    private void backfillDisplayNames() throws SQLException {
        java.util.Map<String, String> names = new java.util.HashMap<>();
        try (Statement s = connection.createStatement();
             java.sql.ResultSet rs = s.executeQuery(
                     "SELECT id, item_details FROM chest_shop_sign WHERE item_details IS NOT NULL")) {
            while (rs.next()) {
                try {
                    com.google.gson.JsonElement name = com.google.gson.JsonParser
                            .parseString(rs.getString("item_details"))
                            .getAsJsonObject().get("displayName");
                    if (name != null && name.isJsonPrimitive()) {
                        String plain = com.playtheatria.shopdb.services.LegacyText.stripCodes(name.getAsString());
                        if (plain != null) names.put(rs.getString("id"), plain);
                    }
                } catch (RuntimeException e) {
                    // Malformed JSON in one row shouldn't block startup.
                }
            }
        }
        try (java.sql.PreparedStatement ps = connection.prepareStatement(
                "UPDATE chest_shop_sign SET display_name_plain = ? WHERE id = ?")) {
            for (java.util.Map.Entry<String, String> e : names.entrySet()) {
                ps.setString(1, e.getValue());
                ps.setString(2, e.getKey());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
