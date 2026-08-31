package com.playtheatria.shopdb.database;

import com.playtheatria.shopdb.models.ShopLocationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionSchemaMigrationTest {
    @TempDir
    Path tempDir;

    @Test
    void legacyRegionsBecomeMarketStallsWithoutLosingPublicationState() throws SQLException {
        File file = tempDir.resolve("legacy-regions.db").toFile();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE region (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR NOT NULL, server VARCHAR NOT NULL, " +
                    "i_x INTEGER, i_y INTEGER, i_z INTEGER, o_x INTEGER, o_y INTEGER, o_z INTEGER, " +
                    "active INTEGER, last_updated INTEGER)");
            statement.executeUpdate("INSERT INTO region " +
                    "(name, server, i_x, i_y, i_z, o_x, o_y, o_z, active) VALUES " +
                    "('bshop1', 'THE_ARK', 1, 2, 3, 4, 5, 6, 1)");
        }

        try (Db db = new Db(file)) {
            RegionRow migrated = new RegionRepository(db)
                    .findByServerTypeAndExternalId("THE_ARK", ShopLocationType.MARKET_STALL, "bshop1");

            assertNotNull(migrated);
            assertEquals(ShopLocationType.MARKET_STALL, migrated.type);
            assertEquals("bshop1", migrated.externalId);
            assertTrue(migrated.active);
            assertEquals(1, migrated.iX);
            assertEquals(6, migrated.oZ);
        }
    }

    @Test
    void duplicateStableIdentitiesAreMergedWithoutLosingRelationships() throws SQLException {
        File file = tempDir.resolve("duplicate-identities.db").toFile();
        // Create the current schema, then emulate a database written by the
        // earlier development build whose identity index was not unique.
        try (Db ignored = new Db(file)) {
            // Schema creation only.
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP INDEX idx_region_identity");
            statement.executeUpdate("INSERT INTO player (id, name) VALUES (1, 'owner_one'), (2, 'owner_two')");
            statement.executeUpdate("INSERT INTO region " +
                    "(id, name, server, location_type, external_id, i_x, i_y, i_z, " +
                    "o_x, o_y, o_z, active, last_updated) VALUES " +
                    "(10, 'old-name', 'THE_ARK', 'PLAYER_SHOP', '01StableLand', " +
                    "1, 2, 3, 4, 5, 6, 0, 100), " +
                    "(20, 'new-name', 'THE_ARK', 'PLAYER_SHOP', '01STABLELAND', " +
                    "11, 12, 13, 14, 15, 16, 1, 200)");
            statement.executeUpdate("INSERT INTO region_mayors (towns_id, mayors_id) VALUES " +
                    "(10, 1), (20, 1), (20, 2)");
            statement.executeUpdate("INSERT INTO chest_shop_sign " +
                    "(id, server, material, town_id, is_hidden) VALUES " +
                    "('shop-old', 'THE_ARK', 'dirt', 10, 1), " +
                    "('shop-new', 'THE_ARK', 'stone', 20, 0)");
        }

        try (Db db = new Db(file)) {
            RegionRepository regions = new RegionRepository(db);
            RegionRow merged = regions.findByServerTypeAndExternalId(
                    "THE_ARK", ShopLocationType.PLAYER_SHOP, "01stableland");

            assertNotNull(merged);
            assertEquals(10L, merged.id);
            assertEquals("new-name", merged.name);
            assertTrue(merged.active);
            assertEquals(11, merged.iX);
            assertEquals(16, merged.oZ);
            assertEquals(200L, merged.lastUpdated);
            java.util.List<String> mergedOwners = regions.mayorNamesOf(merged.id);
            assertEquals(2, mergedOwners.size());
            assertEquals(Set.of("owner_one", "owner_two"),
                    mergedOwners.stream().collect(java.util.stream.Collectors.toSet()));

            try (Statement statement = db.connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT COUNT(*), COUNT(DISTINCT town_id) FROM chest_shop_sign " +
                                 "WHERE town_id = 10")) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
                assertEquals(1, rs.getInt(2));
            }

            assertThrows(SQLException.class, () -> {
                try (Statement statement = db.connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO region " +
                            "(name, server, location_type, external_id) VALUES " +
                            "('duplicate', 'THE_ARK', 'PLAYER_SHOP', '01stableland')");
                }
            });
        }
    }
}
