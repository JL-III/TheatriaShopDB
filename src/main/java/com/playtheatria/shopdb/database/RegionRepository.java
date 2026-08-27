package com.playtheatria.shopdb.database;

import com.playtheatria.shopdb.models.SortBy;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RegionRepository {
    private static final String SELECT =
            "SELECT r.id, r.name, r.server, r.i_x, r.i_y, r.i_z, r.o_x, r.o_y, r.o_z, r.active, r.last_updated FROM region r ";

    private final Db db;

    public RegionRepository(Db db) {
        this.db = db;
    }

    // Single-region lookup binds the enum name ("THE_ARK"), matching the old
    // `server = ?1` query with an enum parameter — this one works.
    public RegionRow findByServerEnumAndName(String serverEnumName, String name) throws SQLException {
        if (serverEnumName == null || name == null) return null;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(
                    SELECT + "WHERE r.server = ? AND r.name = ?")) {
                ps.setString(1, serverEnumName);
                ps.setString(2, name.toLowerCase(Locale.ROOT));
                List<RegionRow> rows = mapRows(ps.executeQuery());
                return rows.isEmpty() ? null : rows.get(0);
            }
        }
    }

    private static final String LIST_WHERE =
            "WHERE r.active = 1 " +
                    "AND (? = '' OR r.server = ?) " +
                    "AND (? = '' OR r.name = ?) ";

    public List<RegionRow> page(String serverStr, String name, SortBy sortBy, int limit, int offset) throws SQLException {
        String sql;
        if (sortBy == SortBy.NUM_PLAYERS) {
            sql = "SELECT r.id, r.name, r.server, r.i_x, r.i_y, r.i_z, r.o_x, r.o_y, r.o_z, r.active, r.last_updated " +
                    "FROM region r " +
                    "LEFT JOIN region_mayors rm ON rm.towns_id = r.id " +
                    "LEFT JOIN player m ON m.id = rm.mayors_id " +
                    LIST_WHERE + "GROUP BY r.id ORDER BY COUNT(m.id) DESC";
        } else if (sortBy == SortBy.NUM_CHEST_SHOPS) {
            sql = "SELECT r.id, r.name, r.server, r.i_x, r.i_y, r.i_z, r.o_x, r.o_y, r.o_z, r.active, r.last_updated " +
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
                return mapRows(ps.executeQuery());
            }
        }
    }

    public long count(String serverStr, String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM region r " + LIST_WHERE;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setString(1, serverStr);
                ps.setString(2, serverStr);
                ps.setString(3, name);
                ps.setString(4, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0;
                }
            }
        }
    }

    public List<String> names(String serverStr) throws SQLException {
        String sql = "SELECT DISTINCT name FROM region " +
                "WHERE active = 1 AND (? = '' OR server = ?) ORDER BY name";
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setString(1, serverStr);
                ps.setString(2, serverStr);
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
                        "INSERT INTO region (name, server, i_x, i_y, i_z, o_x, o_y, o_z, active, last_updated) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
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
                        "UPDATE region SET name = ?, server = ?, i_x = ?, i_y = ?, i_z = ?, " +
                                "o_x = ?, o_y = ?, o_z = ?, active = ?, last_updated = ? WHERE id = ?")) {
                    bind(ps, row);
                    ps.setLong(11, row.id);
                    ps.executeUpdate();
                    return row.id;
                }
            }
        }
    }

    private static void bind(PreparedStatement ps, RegionRow row) throws SQLException {
        ps.setString(1, row.name);
        ps.setString(2, row.server);
        ps.setInt(3, row.iX);
        ps.setInt(4, row.iY);
        ps.setInt(5, row.iZ);
        ps.setInt(6, row.oX);
        ps.setInt(7, row.oY);
        ps.setInt(8, row.oZ);
        ShopRepository.setNullableBool(ps, 9, row.active);
        ShopRepository.setNullableLong(ps, 10, row.lastUpdated);
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
}
