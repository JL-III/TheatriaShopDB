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
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_css_display_name " +
                    "ON chest_shop_sign (display_name_plain)");
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
