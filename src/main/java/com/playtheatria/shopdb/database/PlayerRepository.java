package com.playtheatria.shopdb.database;

import com.playtheatria.shopdb.models.SortBy;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PlayerRepository {
    private static final String HAS_CHEST_SHOPS =
            "EXISTS (SELECT 1 FROM chest_shop_sign owned_shop WHERE owned_shop.owner_id = p.id) ";

    public static class PlayerRow {
        public long id;
        public String name;
    }

    private final Db db;

    public PlayerRepository(Db db) {
        this.db = db;
    }

    public PlayerRow findByName(String name) throws SQLException {
        if (name == null) return null;
        name = name.toLowerCase(Locale.ROOT);
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement("SELECT id, name FROM player WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    PlayerRow row = new PlayerRow();
                    row.id = rs.getLong(1);
                    row.name = rs.getString(2);
                    return row;
                }
            }
        }
    }

    // Mirrors Player.getOrAddPlayers: the returned map is keyed by the name as passed in
    // (lookups and inserts are lowercased).
    public HashMap<String, PlayerRow> getOrAdd(Set<String> playerNames) throws SQLException {
        HashMap<String, PlayerRow> players = new HashMap<>();
        for (String name : playerNames) {
            PlayerRow player = findByName(name);
            if (player == null) {
                player = insert(name.toLowerCase(Locale.ROOT));
            }
            players.put(name, player);
        }
        return players;
    }

    private PlayerRow insert(String lowerName) throws SQLException {
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(
                    "INSERT INTO player (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, lowerName);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    PlayerRow row = new PlayerRow();
                    row.id = rs.getLong(1);
                    row.name = lowerName;
                    return row;
                }
            }
        }
    }

    // Mirrors Player.find(name, sortBy): the name filter is lowercased before comparing.
    public List<PlayerRow> page(String name, SortBy sortBy, int limit, int offset) throws SQLException {
        name = name.toLowerCase(Locale.ROOT);
        String sql;
        if (sortBy == SortBy.NUM_CHEST_SHOPS) {
            sql = "SELECT p.id, p.name FROM player p LEFT JOIN chest_shop_sign c ON c.owner_id = p.id " +
                    "WHERE (? = '' OR p.name = ?) AND " + HAS_CHEST_SHOPS +
                    "GROUP BY p.id ORDER BY COUNT(c.id) DESC, p.name ASC";
        } else if (sortBy == SortBy.NUM_REGIONS) {
            sql = "SELECT p.id, p.name FROM player p " +
                    "LEFT JOIN region_mayors rm ON rm.mayors_id = p.id " +
                    "LEFT JOIN region t ON t.id = rm.towns_id " +
                    "WHERE (? = '' OR p.name = ?) AND " + HAS_CHEST_SHOPS +
                    "GROUP BY p.id ORDER BY COUNT(t.id) DESC, p.name ASC";
        } else {
            sql = "SELECT p.id, p.name FROM player p WHERE (? = '' OR p.name = ?) AND " +
                    HAS_CHEST_SHOPS + "ORDER BY p.name ASC";
        }
        sql += " LIMIT " + limit + " OFFSET " + offset;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, name);
                List<PlayerRow> result = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PlayerRow row = new PlayerRow();
                        row.id = rs.getLong(1);
                        row.name = rs.getString(2);
                        result.add(row);
                    }
                }
                return result;
            }
        }
    }

    public long count(String name) throws SQLException {
        name = name.toLowerCase(Locale.ROOT);
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(
                    "SELECT COUNT(*) FROM player p WHERE (? = '' OR p.name = ?) AND " + HAS_CHEST_SHOPS)) {
                ps.setString(1, name);
                ps.setString(2, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0;
                }
            }
        }
    }

    public List<String> names() throws SQLException {
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(
                    "SELECT p.name FROM player p WHERE " + HAS_CHEST_SHOPS + "ORDER BY p.name")) {
                List<String> result = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(rs.getString(1));
                }
                return result;
            }
        }
    }

    // player.chestShops.size() in the old backend counted every owned row, hidden included.
    public int numChestShops(long playerId) throws SQLException {
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(
                    "SELECT COUNT(*) FROM chest_shop_sign WHERE owner_id = ?")) {
                ps.setLong(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        }
    }

    public List<RegionRow> townsOf(long playerId) throws SQLException {
        String sql = "SELECT r.id, r.name, r.server, r.i_x, r.i_y, r.i_z, r.o_x, r.o_y, r.o_z, r.active, r.last_updated " +
                "FROM region_mayors rm JOIN region r ON r.id = rm.towns_id WHERE rm.mayors_id = ?";
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setLong(1, playerId);
                return RegionRepository.mapRows(ps.executeQuery());
            }
        }
    }
}
