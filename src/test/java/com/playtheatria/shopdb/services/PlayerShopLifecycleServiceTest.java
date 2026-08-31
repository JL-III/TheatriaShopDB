package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.ChestShopRow;
import com.playtheatria.shopdb.database.Db;
import com.playtheatria.shopdb.database.PlayerRepository;
import com.playtheatria.shopdb.database.RegionRepository;
import com.playtheatria.shopdb.database.RegionRow;
import com.playtheatria.shopdb.database.ShopRepository;
import com.playtheatria.shopdb.models.ShopLocationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerShopLifecycleServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void deletingLandDeactivatesStableRowAndDetachesItsShops() throws SQLException {
        try (Fixture fixture = fixture("deleted.db")) {
            RegionRow land = fixture.playerShop("stable-ulid");
            fixture.addOwner(land, "oldowner");
            fixture.shop("inside", land.id, 12, 20);

            PlayerShopLifecycleService.Reconciliation result =
                    fixture.lifecycle.landDeleted("THE_ARK", "stable-ulid");

            RegionRow stored = fixture.find("stable-ulid");
            assertTrue(result.regionFound());
            assertEquals(1, result.affectedShops());
            assertFalse(Boolean.TRUE.equals(stored.active));
            assertNotNull(stored.lastUpdated);
            assertEquals(List.of(), fixture.regions.mayorNamesOf(stored.id));
            assertEquals(List.of(), fixture.shops.findAssignedToRegion(stored.id));
            assertEquals(List.of(), fixture.shops.findVisible(""));
            assertTrue(fixture.shops.exists("inside"));
        }
    }

    @Test
    void wholeLandOwnerChangeRevokesPublicationButKeepsAssociationsForRelisting() throws SQLException {
        try (Fixture fixture = fixture("owner-change.db")) {
            RegionRow land = fixture.playerShop("stable-ulid");
            fixture.addOwner(land, "oldowner");
            fixture.shop("inside", land.id, 12, 20);

            PlayerShopLifecycleService.Reconciliation result =
                    fixture.lifecycle.landOwnerChanged("THE_ARK", "stable-ulid");

            RegionRow stored = fixture.find("stable-ulid");
            assertTrue(result.regionFound());
            assertEquals(1, result.affectedShops());
            assertFalse(Boolean.TRUE.equals(stored.active));
            assertEquals(List.of(), fixture.regions.mayorNamesOf(stored.id));
            List<ChestShopRow> assigned = fixture.shops.findAssignedToRegion(stored.id);
            assertEquals(List.of("inside"), assigned.stream().map(shop -> shop.id).toList());
            assertTrue(Boolean.TRUE.equals(assigned.get(0).isHidden));
        }
    }

    @Test
    void unclaimOnlyDetachesShopsInThatChunkIncludingNegativeCoordinates() throws SQLException {
        try (Fixture fixture = fixture("unclaim.db")) {
            RegionRow land = fixture.playerShop("stable-ulid");
            fixture.shop("removed", land.id, -1, -17);  // chunk -1,-2
            fixture.shop("remaining", land.id, -17, -17); // chunk -2,-2
            fixture.legacyShopWithoutCoordinates("legacy-unknown", land.id);

            PlayerShopLifecycleService.Reconciliation result =
                    fixture.lifecycle.chunkUnclaimed("THE_ARK", "stable-ulid", -1, -2);

            assertTrue(result.regionFound());
            assertEquals(2, result.affectedShops());
            assertTrue(Boolean.TRUE.equals(fixture.find("stable-ulid").active));
            assertEquals(List.of("remaining"), fixture.shops.findAssignedToRegion(land.id)
                    .stream().map(shop -> shop.id).toList());
            assertEquals(List.of("remaining"), fixture.shops.findVisible("")
                    .stream().map(shop -> shop.id).toList());
            assertTrue(fixture.shops.exists("removed"));
            assertTrue(fixture.shops.exists("legacy-unknown"));
        }
    }

    @Test
    void unknownStableIdentityIsANoOp() throws SQLException {
        try (Fixture fixture = fixture("missing.db")) {
            PlayerShopLifecycleService.Reconciliation result =
                    fixture.lifecycle.landDeleted("THE_ARK", "missing-ulid");

            assertFalse(result.regionFound());
            assertEquals(0, result.affectedShops());
        }
    }

    @Test
    void reconciliationRollsBackAllChangesWhenAnyWriteFails() throws SQLException {
        try (Fixture fixture = fixture("rollback.db")) {
            RegionRow land = fixture.playerShop("stable-ulid");
            fixture.addOwner(land, "oldowner");
            fixture.shop("inside", land.id, 12, 20);
            synchronized (fixture.db.lock) {
                try (java.sql.Statement statement = fixture.db.connection.createStatement()) {
                    statement.executeUpdate("CREATE TRIGGER reject_lifecycle_hide " +
                            "BEFORE UPDATE OF is_hidden ON chest_shop_sign " +
                            "BEGIN SELECT RAISE(ABORT, 'injected test failure'); END");
                }
            }

            assertThrows(SQLException.class,
                    () -> fixture.lifecycle.landDeleted("THE_ARK", "stable-ulid"));

            RegionRow stored = fixture.find("stable-ulid");
            assertTrue(Boolean.TRUE.equals(stored.active));
            assertEquals(List.of("oldowner"), fixture.regions.mayorNamesOf(stored.id));
            assertEquals(List.of("inside"), fixture.shops.findAssignedToRegion(stored.id)
                    .stream().map(shop -> shop.id).toList());
            assertEquals(List.of("inside"), fixture.shops.findVisible("")
                    .stream().map(shop -> shop.id).toList());
        }
    }

    private Fixture fixture(String fileName) throws SQLException {
        return new Fixture(new Db(tempDir.resolve(fileName).toFile()));
    }

    private static final class Fixture implements AutoCloseable {
        private final Db db;
        private final RegionRepository regions;
        private final ShopRepository shops;
        private final PlayerRepository players;
        private final PlayerShopLifecycleService lifecycle;

        private Fixture(Db db) {
            this.db = db;
            this.regions = new RegionRepository(db);
            this.shops = new ShopRepository(db);
            this.players = new PlayerRepository(db);
            this.lifecycle = new PlayerShopLifecycleService(db);
        }

        private RegionRow playerShop(String externalId) throws SQLException {
            RegionRow row = new RegionRow();
            row.name = "player-shop";
            row.server = "THE_ARK";
            row.type = ShopLocationType.PLAYER_SHOP;
            row.externalId = externalId;
            row.iX = -32;
            row.iY = 0;
            row.iZ = -32;
            row.oX = 31;
            row.oY = 255;
            row.oZ = 31;
            row.active = true;
            regions.upsert(row);
            return row;
        }

        private void addOwner(RegionRow region, String name) throws SQLException {
            PlayerRepository.PlayerRow player = players.getOrAdd(Set.of(name)).get(name);
            regions.setMayors(region.id, List.of(player.id));
        }

        private void shop(String id, long regionId, int x, int z) throws SQLException {
            ChestShopRow row = new ChestShopRow();
            row.id = id;
            row.server = "THE_ARK";
            row.x = x;
            row.y = 64;
            row.z = z;
            row.material = "dirt";
            row.townId = regionId;
            row.isHidden = false;
            row.isBuySign = true;
            row.isSellSign = false;
            shops.upsert(row);
        }

        private void legacyShopWithoutCoordinates(String id, long regionId) throws SQLException {
            synchronized (db.lock) {
                try (java.sql.PreparedStatement ps = db.connection.prepareStatement(
                        "INSERT INTO chest_shop_sign " +
                                "(id, server, x, y, z, material, town_id, is_hidden) " +
                                "VALUES (?, 'THE_ARK', NULL, NULL, NULL, 'dirt', ?, 0)")) {
                    ps.setString(1, id);
                    ps.setLong(2, regionId);
                    ps.executeUpdate();
                }
            }
        }

        private RegionRow find(String externalId) throws SQLException {
            return regions.findByServerTypeAndExternalId(
                    "THE_ARK", ShopLocationType.PLAYER_SHOP, externalId);
        }

        @Override
        public void close() throws SQLException {
            db.close();
        }
    }
}
