package com.playtheatria.shopdb.database;

import com.playtheatria.shopdb.models.ItemType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers the 4.1.0 schema additions: migrating a 4.0.0 database gains the
 * base_material/item_details columns without touching existing rows, and the
 * upsert keeps known details when an event couldn't resolve the item.
 */
class ItemDetailsPersistenceTest {
    @TempDir
    Path tempDir;

    private static final String DETAILS_JSON =
            "{\"displayName\":\"§6Golden Rod\",\"enchants\":[{\"name\":\"lure\",\"level\":3}]}";

    @Test
    void migratesOldDatabaseAndRoundTripsDetails() throws SQLException {
        File file = tempDir.resolve("old.db").toFile();
        createOldSchemaWithRow(file);

        try (Db db = new Db(file)) {
            ShopRepository shops = new ShopRepository(db);

            // The pre-existing row survives the migration with null details.
            ChestShopRow existing = findOnly(shops);
            assertEquals("fishing rod#1m", existing.material);
            assertNull(existing.baseMaterial);
            assertNull(existing.itemDetails);

            // A resolved event fills them in.
            existing.baseMaterial = "fishing_rod";
            existing.itemDetails = DETAILS_JSON;
            shops.upsert(existing);
            ChestShopRow updated = findOnly(shops);
            assertEquals("fishing_rod", updated.baseMaterial);
            assertEquals(DETAILS_JSON, updated.itemDetails);

            // An unresolved event (no base material) must not wipe known details.
            ChestShopRow unresolved = findOnly(shops);
            unresolved.baseMaterial = null;
            unresolved.itemDetails = null;
            shops.upsert(unresolved);
            ChestShopRow afterUnresolved = findOnly(shops);
            assertEquals("fishing_rod", afterUnresolved.baseMaterial);
            assertEquals(DETAILS_JSON, afterUnresolved.itemDetails);

            // A resolved event with no details is authoritative and clears them.
            ChestShopRow plain = findOnly(shops);
            plain.baseMaterial = "fishing_rod";
            plain.itemDetails = null;
            shops.upsert(plain);
            ChestShopRow afterPlain = findOnly(shops);
            assertEquals("fishing_rod", afterPlain.baseMaterial);
            assertNull(afterPlain.itemDetails);
        }
    }

    @Test
    void searchesByPlainDisplayNameCaseInsensitively() throws SQLException {
        File file = tempDir.resolve("search.db").toFile();
        try (Db db = new Db(file)) {
            ShopRepository shops = new ShopRepository(db);

            ChestShopRow named = row("shop-named", "fishing rod#1m");
            named.baseMaterial = "fishing_rod";
            named.itemDetails = DETAILS_JSON;
            named.displayNamePlain = "Golden Rod";
            shops.upsert(named);
            shops.upsert(row("shop-plain", "dirt"));

            assertEquals(List.of("Golden Rod"),
                    shops.distinctDisplayNames(com.playtheatria.shopdb.models.TradeType.BUY, ""));

            List<ChestShopRow> byName = shops.find("", "golden rod", "", "", 0, ItemType.ALL,
                    com.playtheatria.shopdb.models.TradeType.BUY, "", false,
                    com.playtheatria.shopdb.models.SortBy.MATERIAL, null, null);
            assertEquals(1, byName.size());
            assertEquals("shop-named", byName.get(0).id);

            // No name filter still returns everything.
            assertEquals(2, shops.find("", "", "", "", 0, ItemType.ALL,
                    com.playtheatria.shopdb.models.TradeType.BUY,
                    "", false, com.playtheatria.shopdb.models.SortBy.MATERIAL, null, null).size());
        }
    }

    private static ChestShopRow row(String id, String material) {
        ChestShopRow row = new ChestShopRow();
        row.id = id;
        row.server = "THE_ARK";
        row.x = 1;
        row.y = 64;
        row.z = 1;
        row.material = material;
        row.quantity = 1;
        row.buyPrice = 100.0;
        row.buyPriceEach = 100.0;
        row.isBuySign = true;
        row.isSellSign = false;
        row.isHidden = false;
        row.isFull = false;
        row.quantityAvailable = 1;
        return row;
    }

    private static ChestShopRow findOnly(ShopRepository shops) throws SQLException {
        List<ChestShopRow> rows = shops.findVisible("");
        assertEquals(1, rows.size());
        return rows.get(0);
    }

    /** Builds the exact chest_shop_sign schema 4.0.0 shipped, with one row. */
    private static void createOldSchemaWithRow(File file) throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE chest_shop_sign (" +
                    "id VARCHAR PRIMARY KEY, server VARCHAR NOT NULL, " +
                    "x INTEGER, y INTEGER, z INTEGER, material VARCHAR NOT NULL, " +
                    "owner_id INTEGER, town_id INTEGER, quantity INTEGER, quantity_available INTEGER, " +
                    "buy_price REAL, sell_price REAL, buy_price_each REAL, sell_price_each REAL, " +
                    "is_full INTEGER, is_hidden INTEGER, is_buy_sign INTEGER, is_sell_sign INTEGER)");
            s.executeUpdate("INSERT INTO chest_shop_sign (id, server, x, y, z, material, quantity, is_hidden) " +
                    "VALUES ('shop-1', 'THE_ARK', 1, 64, 1, 'fishing rod#1m', 1, 0)");
        }
    }
}
