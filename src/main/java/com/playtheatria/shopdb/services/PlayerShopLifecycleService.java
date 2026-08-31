package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.Db;
import com.playtheatria.shopdb.models.ShopLocationType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps persisted player-shop publication state safe when its external claim changes.
 *
 * <p>This class deliberately has no Lands or Bukkit dependency. The optional Lands
 * listener reduces events to a stable claim id and coordinates before calling it,
 * which leaves the database behavior small and directly testable.</p>
 */
public final class PlayerShopLifecycleService {
    private final Db db;

    public PlayerShopLifecycleService(Db db) {
        this.db = db;
    }

    /**
     * Deactivates a deleted land and removes every shop's association with it.
     */
    public Reconciliation landDeleted(String serverEnumName, String externalId) throws SQLException {
        return inTransaction(() -> {
            Long regionId = findPlayerShopId(serverEnumName, externalId);
            if (regionId == null) return Reconciliation.notFound();

            deactivate(regionId);
            int affected = hideAssigned(regionId, true);
            return new Reconciliation(true, affected);
        });
    }

    /**
     * Revokes publication when ownership of a whole land changes. Associations
     * remain so an explicit listing by the new owner can reveal the same shops
     * immediately; exact owner-only authorization is enforced by the command path.
     */
    public Reconciliation landOwnerChanged(String serverEnumName, String externalId) throws SQLException {
        return inTransaction(() -> {
            Long regionId = findPlayerShopId(serverEnumName, externalId);
            if (regionId == null) return Reconciliation.notFound();

            deactivate(regionId);
            int affected = hideAssigned(regionId, false);
            return new Reconciliation(true, affected);
        });
    }

    /**
     * Hides and detaches shops in a chunk that is no longer part of the land.
     * Other claimed chunks and the land's publication state are left untouched.
     */
    public Reconciliation chunkUnclaimed(String serverEnumName, String externalId,
                                          int chunkX, int chunkZ) throws SQLException {
        return inTransaction(() -> {
            Long regionId = findPlayerShopId(serverEnumName, externalId);
            if (regionId == null) return Reconciliation.notFound();

            List<String> affectedIds = new ArrayList<>();
            try (PreparedStatement ps = db.connection.prepareStatement(
                    "SELECT id, x, z FROM chest_shop_sign WHERE town_id = ? AND server = ?")) {
                ps.setLong(1, regionId);
                ps.setString(2, serverEnumName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int x = rs.getInt("x");
                        boolean xMissing = rs.wasNull();
                        int z = rs.getInt("z");
                        boolean zMissing = rs.wasNull();
                        // Old imported rows may not have usable coordinates. We
                        // cannot prove they are still claimed, so fail closed;
                        // the queued world rescan will restore valid shops.
                        if (xMissing || zMissing) {
                            affectedIds.add(rs.getString("id"));
                            continue;
                        }
                        if (Math.floorDiv(x, 16) == chunkX && Math.floorDiv(z, 16) == chunkZ) {
                            affectedIds.add(rs.getString("id"));
                        }
                    }
                }
            }

            try (PreparedStatement ps = db.connection.prepareStatement(
                    "UPDATE chest_shop_sign SET town_id = NULL, is_hidden = 1 WHERE id = ?")) {
                for (String id : affectedIds) {
                    ps.setString(1, id);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return new Reconciliation(true, affectedIds.size());
        });
    }

    private Long findPlayerShopId(String serverEnumName, String externalId) throws SQLException {
        if (serverEnumName == null || externalId == null) return null;
        try (PreparedStatement ps = db.connection.prepareStatement(
                "SELECT id FROM region WHERE server = ? AND location_type = ? " +
                        "AND lower(external_id) = lower(?)")) {
            ps.setString(1, serverEnumName);
            ps.setString(2, ShopLocationType.PLAYER_SHOP.name());
            ps.setString(3, externalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    private void deactivate(long regionId) throws SQLException {
        try (PreparedStatement ps = db.connection.prepareStatement(
                "UPDATE region SET active = 0, last_updated = ? WHERE id = ?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setLong(2, regionId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Player-shop row disappeared during lifecycle reconciliation.");
            }
        }
        try (PreparedStatement ps = db.connection.prepareStatement(
                "DELETE FROM region_mayors WHERE towns_id = ?")) {
            ps.setLong(1, regionId);
            ps.executeUpdate();
        }
    }

    private int hideAssigned(long regionId, boolean detach) throws SQLException {
        String sql = detach
                ? "UPDATE chest_shop_sign SET town_id = NULL, is_hidden = 1 WHERE town_id = ?"
                : "UPDATE chest_shop_sign SET is_hidden = 1 WHERE town_id = ?";
        try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
            ps.setLong(1, regionId);
            return ps.executeUpdate();
        }
    }

    private <T> T inTransaction(SqlOperation<T> operation) throws SQLException {
        synchronized (db.lock) {
            boolean ownsTransaction = db.connection.getAutoCommit();
            if (ownsTransaction) db.connection.setAutoCommit(false);
            try {
                T result = operation.run();
                if (ownsTransaction) db.connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                if (ownsTransaction) {
                    try {
                        db.connection.rollback();
                    } catch (SQLException rollbackFailure) {
                        e.addSuppressed(rollbackFailure);
                    }
                }
                throw e;
            } finally {
                if (ownsTransaction) db.connection.setAutoCommit(true);
            }
        }
    }

    public record Reconciliation(boolean regionFound, int affectedShops) {
        private static Reconciliation notFound() {
            return new Reconciliation(false, 0);
        }
    }

    @FunctionalInterface
    private interface SqlOperation<T> {
        T run() throws SQLException;
    }
}
