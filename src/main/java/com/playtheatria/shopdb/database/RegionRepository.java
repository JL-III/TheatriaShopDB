package com.playtheatria.shopdb.database;

import com.playtheatria.shopdb.models.SortBy;
import com.playtheatria.shopdb.models.ShopLocationType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RegionRepository {
    private static final String ROW_COLUMNS =
            "r.id, r.name, r.server, r.location_type, r.external_id, " +
                    "r.i_x, r.i_y, r.i_z, r.o_x, r.o_y, r.o_z, r.active, r.last_updated ";
    private static final String SELECT =
            "SELECT " + ROW_COLUMNS + "FROM region r ";

    private final Db db;

    public RegionRepository(Db db) {
        this.db = db;
    }

    // Single-region lookup binds the enum name ("THE_ARK"), matching the old
    // `server = ?1` query with an enum parameter — this one works.
    public RegionRow findByServerEnumAndName(String serverEnumName, String name) throws SQLException {
        return findByServerEnumTypeAndName(serverEnumName, ShopLocationType.MARKET_STALL, name);
    }

    public RegionRow findByServerEnumTypeAndName(String serverEnumName, ShopLocationType type,
                                                  String name) throws SQLException {
        if (serverEnumName == null || name == null) return null;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(
                    SELECT + "WHERE r.server = ? AND r.location_type = ? AND r.name = ?")) {
                ps.setString(1, serverEnumName);
                ps.setString(2, effectiveType(type).name());
                ps.setString(3, name.toLowerCase(Locale.ROOT));
                List<RegionRow> rows = mapRows(ps.executeQuery());
                return rows.isEmpty() ? null : rows.get(0);
            }
        }
    }

    public RegionRow findByServerTypeAndExternalId(String serverEnumName, ShopLocationType type,
                                                    String externalId) throws SQLException {
        if (serverEnumName == null || externalId == null) return null;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(
                    SELECT + "WHERE r.server = ? AND r.location_type = ? " +
                            "AND lower(r.external_id) = lower(?)")) {
                ps.setString(1, serverEnumName);
                ps.setString(2, effectiveType(type).name());
                ps.setString(3, externalId);
                List<RegionRow> rows = mapRows(ps.executeQuery());
                return rows.isEmpty() ? null : rows.get(0);
            }
        }
    }

    private static final String LIST_WHERE =
            "WHERE r.active = 1 " +
                    "AND (? = '' OR r.server = ?) " +
                    "AND (? = '' OR r.name = ?) " +
                    "AND (? = '' OR r.location_type = ?) ";

    public List<RegionRow> page(String serverStr, String name, SortBy sortBy, int limit, int offset) throws SQLException {
        return page(serverStr, name, null, sortBy, limit, offset);
    }

    public List<RegionRow> page(String serverStr, String name, ShopLocationType type,
                                SortBy sortBy, int limit, int offset) throws SQLException {
        String sql;
        if (sortBy == SortBy.NUM_PLAYERS) {
            sql = "SELECT " + ROW_COLUMNS +
                    "FROM region r " +
                    "LEFT JOIN region_mayors rm ON rm.towns_id = r.id " +
                    "LEFT JOIN player m ON m.id = rm.mayors_id " +
                    LIST_WHERE + "GROUP BY r.id ORDER BY COUNT(m.id) DESC";
        } else if (sortBy == SortBy.NUM_CHEST_SHOPS) {
            sql = "SELECT " + ROW_COLUMNS +
                    "FROM region r " +
                    "LEFT JOIN chest_shop_sign c ON c.town_id = r.id " +
                    LIST_WHERE + "GROUP BY r.id ORDER BY COUNT(c.id) DESC";
        } else {
            sql = SELECT + LIST_WHERE + "ORDER BY r.name ASC";
        }
        sql += " LIMIT " + limit + " OFFSET " + offset;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setString(1, serverStr);
                ps.setString(2, serverStr);
                ps.setString(3, name);
                ps.setString(4, name);
                String typeName = type == null ? "" : type.name();
                ps.setString(5, typeName);
                ps.setString(6, typeName);
                return mapRows(ps.executeQuery());
            }
        }
    }

    public long count(String serverStr, String name) throws SQLException {
        return count(serverStr, name, null);
    }

    public long count(String serverStr, String name, ShopLocationType type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM region r " + LIST_WHERE;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setString(1, serverStr);
                ps.setString(2, serverStr);
                ps.setString(3, name);
                ps.setString(4, name);
                String typeName = type == null ? "" : type.name();
                ps.setString(5, typeName);
                ps.setString(6, typeName);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0;
                }
            }
        }
    }

    public List<String> names(String serverStr) throws SQLException {
        return names(serverStr, null);
    }

    public List<String> names(String serverStr, ShopLocationType type) throws SQLException {
        String sql = "SELECT DISTINCT name FROM region " +
                "WHERE active = 1 AND (? = '' OR server = ?) " +
                "AND (? = '' OR location_type = ?) ORDER BY name";
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setString(1, serverStr);
                ps.setString(2, serverStr);
                String typeName = type == null ? "" : type.name();
                ps.setString(3, typeName);
                ps.setString(4, typeName);
                List<String> result = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(rs.getString(1));
                }
                return result;
            }
        }
    }

    public List<PlayerRepository.PlayerRow> mayorRowsOf(long regionId) throws SQLException {
        String sql = "SELECT p.id, p.name FROM region_mayors rm JOIN player p ON p.id = rm.mayors_id WHERE rm.towns_id = ?";
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setLong(1, regionId);
                List<PlayerRepository.PlayerRow> result = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PlayerRepository.PlayerRow row = new PlayerRepository.PlayerRow();
                        row.id = rs.getLong(1);
                        row.name = rs.getString(2);
                        result.add(row);
                    }
                }
                return result;
            }
        }
    }

    public List<String> mayorNamesOf(long regionId) throws SQLException {
        String sql = "SELECT p.name FROM region_mayors rm JOIN player p ON p.id = rm.mayors_id WHERE rm.towns_id = ?";
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setLong(1, regionId);
                List<String> result = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(rs.getString(1));
                }
                return result;
            }
        }
    }

    // region.chestShops.size() in the old backend counted every row, hidden included.
    public int numChestShops(long regionId) throws SQLException {
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(
                    "SELECT COUNT(*) FROM chest_shop_sign WHERE town_id = ?")) {
                ps.setLong(1, regionId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        }
    }

    /** Insert (id == null) or update; returns the row id. */
    public long upsert(RegionRow row) throws SQLException {
        synchronized (db.lock) {
            if (row.id == null) {
                try (PreparedStatement ps = db.connection.prepareStatement(
                        "INSERT INTO region (name, server, location_type, external_id, i_x, i_y, i_z, " +
                                "o_x, o_y, o_z, active, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps, row);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        row.id = rs.getLong(1);
                        return row.id;
                    }
                }
            } else {
                try (PreparedStatement ps = db.connection.prepareStatement(
                        "UPDATE region SET name = ?, server = ?, location_type = ?, external_id = ?, " +
                                "i_x = ?, i_y = ?, i_z = ?, o_x = ?, o_y = ?, o_z = ?, " +
                                "active = ?, last_updated = ? WHERE id = ?")) {
                    bind(ps, row);
                    ps.setLong(13, row.id);
                    ps.executeUpdate();
                    return row.id;
                }
            }
        }
    }

    /**
     * Atomically updates or creates a row by its stable source identity.
     * Keeping lookup and insert under the repository lock prevents concurrent
     * publication and shop-ingest requests from both observing a missing row.
     * A null active override preserves an existing publication state and makes
     * a newly observed location inactive by default.
     */
    public RegionRow upsertByIdentity(RegionRow incoming, Boolean activeOverride) throws SQLException {
        if (incoming == null || incoming.server == null || incoming.externalId == null) {
            throw new IllegalArgumentException("Region identity fields cannot be null.");
        }

        synchronized (db.lock) {
            RegionRow stored = findByServerTypeAndExternalId(
                    incoming.server, incoming.type, incoming.externalId);
            if (stored == null) {
                stored = new RegionRow();
                stored.active = activeOverride == null ? Boolean.FALSE : activeOverride;
            } else if (activeOverride != null) {
                stored.active = activeOverride;
            }

            stored.name = incoming.name;
            stored.server = incoming.server;
            stored.type = effectiveType(incoming.type);
            stored.externalId = incoming.externalId;
            stored.iX = incoming.iX;
            stored.iY = incoming.iY;
            stored.iZ = incoming.iZ;
            stored.oX = incoming.oX;
            stored.oY = incoming.oY;
            stored.oZ = incoming.oZ;
            stored.lastUpdated = incoming.lastUpdated;
            upsert(stored);
            return stored;
        }
    }

    private static void bind(PreparedStatement ps, RegionRow row) throws SQLException {
        ps.setString(1, row.name);
        ps.setString(2, row.server);
        ps.setString(3, effectiveType(row.type).name());
        ps.setString(4, row.externalId == null ? row.name : row.externalId);
        ps.setInt(5, row.iX);
        ps.setInt(6, row.iY);
        ps.setInt(7, row.iZ);
        ps.setInt(8, row.oX);
        ps.setInt(9, row.oY);
        ps.setInt(10, row.oZ);
        ShopRepository.setNullableBool(ps, 11, row.active);
        ShopRepository.setNullableLong(ps, 12, row.lastUpdated);
    }

    public void setMayors(long regionId, List<Long> playerIds) throws SQLException {
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement("DELETE FROM region_mayors WHERE towns_id = ?")) {
                ps.setLong(1, regionId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = db.connection.prepareStatement(
                    "INSERT INTO region_mayors (towns_id, mayors_id) VALUES (?, ?)")) {
                for (Long playerId : playerIds) {
                    if (playerId == null) continue;
                    ps.setLong(1, regionId);
                    ps.setLong(2, playerId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    static List<RegionRow> mapRows(ResultSet rs) throws SQLException {
        List<RegionRow> result = new ArrayList<>();
        try (rs) {
            while (rs.next()) {
                RegionRow row = new RegionRow();
                row.id = rs.getLong("id");
                row.name = rs.getString("name");
                row.server = rs.getString("server");
                row.type = effectiveType(ShopLocationType.fromString(rs.getString("location_type")));
                row.externalId = rs.getString("external_id");
                row.iX = rs.getInt("i_x");
                row.iY = rs.getInt("i_y");
                row.iZ = rs.getInt("i_z");
                row.oX = rs.getInt("o_x");
                row.oY = rs.getInt("o_y");
                row.oZ = rs.getInt("o_z");
                row.active = ShopRepository.getNullableBool(rs, "active");
                row.lastUpdated = ShopRepository.getNullableLong(rs, "last_updated");
                result.add(row);
            }
        }
        return result;
    }

    private static ShopLocationType effectiveType(ShopLocationType type) {
        return type == null ? ShopLocationType.MARKET_STALL : type;
    }
}
