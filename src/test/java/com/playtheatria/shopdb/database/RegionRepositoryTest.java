package com.playtheatria.shopdb.database;

import com.playtheatria.shopdb.models.SortBy;
import com.playtheatria.shopdb.models.ShopLocationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void browseQueriesOnlyIncludeListedRegions() throws SQLException {
        try (Db db = new Db(tempDir.resolve("regions.db").toFile())) {
            RegionRepository regions = new RegionRepository(db);

            regions.upsert(region("listed-alpha", true));
            regions.upsert(region("listed-beta", true));
            regions.upsert(region("unlisted", false));
            regions.upsert(region("unknown-status", null));

            assertEquals(List.of("listed-alpha", "listed-beta"),
                    names(regions.page("", "", SortBy.NAME, 10, 0)));
            assertOnlyListed(regions.page("", "", SortBy.NUM_PLAYERS, 10, 0));
            assertOnlyListed(regions.page("", "", SortBy.NUM_CHEST_SHOPS, 10, 0));

            assertEquals(2, regions.count("", ""));
            assertEquals(0, regions.count("", "unlisted"));
            assertEquals(List.of("listed-alpha", "listed-beta"), regions.names(""));
            assertEquals(List.of("listed-beta"), names(regions.page("", "", SortBy.NAME, 1, 1)));

            // Direct lookups remain available for owner/admin workflows.
            RegionRow unlisted = regions.findByServerEnumAndName("THE_ARK", "unlisted");
            assertNotNull(unlisted);
            assertFalse(unlisted.active);
        }
    }

    @Test
    void sourceTypeAndStableIdKeepMarketAndPlayerLocationsSeparate() throws SQLException {
        try (Db db = new Db(tempDir.resolve("location-types.db").toFile())) {
            RegionRepository regions = new RegionRepository(db);

            RegionRow market = region("shared-name", true);
            market.type = ShopLocationType.MARKET_STALL;
            market.externalId = "shared-name";
            regions.upsert(market);

            RegionRow playerOne = region("shared-name", true);
            playerOne.type = ShopLocationType.PLAYER_SHOP;
            playerOne.externalId = "01PLAYERLANDONE";
            regions.upsert(playerOne);

            RegionRow playerTwo = region("shared-name", true);
            playerTwo.type = ShopLocationType.PLAYER_SHOP;
            playerTwo.externalId = "01PLAYERLANDTWO";
            regions.upsert(playerTwo);

            assertEquals(List.of("shared-name"),
                    names(regions.page("", "", ShopLocationType.MARKET_STALL,
                            SortBy.NAME, 10, 0)));
            assertEquals(2, regions.count("", "", ShopLocationType.PLAYER_SHOP));
            assertEquals("01PLAYERLANDONE", regions.findByServerTypeAndExternalId(
                    "THE_ARK", ShopLocationType.PLAYER_SHOP, "01playerlandone").externalId);
            assertEquals(1, regions.count("", "", ShopLocationType.MARKET_STALL));
        }
    }

    @Test
    void identityUpsertSerializesConcurrentFirstObservation() throws Exception {
        try (Db db = new Db(tempDir.resolve("concurrent-identity.db").toFile())) {
            RegionRepository regions = new RegionRepository(db);
            int workers = 8;
            CountDownLatch ready = new CountDownLatch(workers);
            CountDownLatch start = new CountDownLatch(1);
            java.util.concurrent.ExecutorService executor = Executors.newFixedThreadPool(workers);
            List<Future<RegionRow>> futures = new ArrayList<>();
            try {
                for (int i = 0; i < workers; i++) {
                    final int worker = i;
                    futures.add(executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        RegionRow row = region("land-" + worker, null);
                        row.type = ShopLocationType.PLAYER_SHOP;
                        row.externalId = worker % 2 == 0 ? "01StableLand" : "01STABLELAND";
                        row.lastUpdated = (long) worker;
                        return regions.upsertByIdentity(row, null);
                    }));
                }
                ready.await();
                start.countDown();
                for (Future<RegionRow> future : futures) assertNotNull(future.get());
            } finally {
                executor.shutdownNow();
            }

            try (Statement statement = db.connection.createStatement();
                 java.sql.ResultSet rs = statement.executeQuery(
                         "SELECT COUNT(*) FROM region WHERE server = 'THE_ARK' " +
                                 "AND location_type = 'PLAYER_SHOP' " +
                                 "AND lower(external_id) = '01stableland'")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    private static void assertOnlyListed(List<RegionRow> rows) {
        assertEquals(Set.of("listed-alpha", "listed-beta"), new HashSet<>(names(rows)));
    }

    private static List<String> names(List<RegionRow> rows) {
        return rows.stream().map(row -> row.name).toList();
    }

    private static RegionRow region(String name, Boolean active) {
        RegionRow row = new RegionRow();
        row.name = name;
        row.server = "THE_ARK";
        row.active = active;
        return row;
    }
}
