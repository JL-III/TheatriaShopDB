package com.playtheatria.shopdb.database;

import com.playtheatria.shopdb.models.ItemType;
import com.playtheatria.shopdb.models.SortBy;
import com.playtheatria.shopdb.models.TradeType;
import com.playtheatria.shopdb.services.DtoMappers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShopRepositorySearchTest {
    @TempDir
    Path tempDir;

    @Test
    void freeTextQuerySearchesMaterialAndDisplayNameSubstrings() throws SQLException {
        try (Db db = new Db(tempDir.resolve("shop-search.db").toFile())) {
            ShopRepository shops = new ShopRepository(db);

            shops.upsert(shop("blue", "blue wool", null, true, false));
            shops.upsert(shop("white", "white wool", null, true, false));
            shops.upsert(shop("carpet", "wool carpet", null, true, false));
            shops.upsert(shop("custom", "stick", "Wool Wand", true, false));
            shops.upsert(shop("stone", "stone", null, true, false));
            shops.upsert(shop("hidden", "pink wool", null, true, true));
            shops.upsert(shop("sell-only", "red wool", null, false, false));

            List<ChestShopRow> matches = shops.find("", "", "WoOl", "", 0, ItemType.ALL, TradeType.BUY,
                    "", false, SortBy.MATERIAL, null, null);

            assertEquals(Set.of("blue", "white", "carpet", "custom"), ids(matches));
            assertEquals(4, shops.count("", "", "WoOl", "", 0, ItemType.ALL,
                    TradeType.BUY, "", false));

            // Selecting an existing suggestion remains an exact filter.
            assertEquals(Set.of("blue"), ids(shops.find("blue wool", "", "", "", 0,
                    ItemType.ALL, TradeType.BUY,
                    "", false, SortBy.MATERIAL, null, null)));
        }
    }

    @Test
    void freeTextQueryTreatsSqlWildcardCharactersLiterally() throws SQLException {
        try (Db db = new Db(tempDir.resolve("shop-search-literal.db").toFile())) {
            ShopRepository shops = new ShopRepository(db);
            shops.upsert(shop("percent", "odd%item", null, true, false));
            shops.upsert(shop("underscore", "odd_item", null, true, false));
            shops.upsert(shop("plain", "ordinary item", null, true, false));

            assertEquals(Set.of("percent"), ids(shops.find("", "", "%", "", 0,
                    ItemType.ALL, TradeType.BUY,
                    "", false, SortBy.MATERIAL, null, null)));
            assertEquals(Set.of("underscore"), ids(shops.find("", "", "_", "", 0,
                    ItemType.ALL, TradeType.BUY,
                    "", false, SortBy.MATERIAL, null, null)));
        }
    }

    @Test
    void searchesStructuredMetadataAndFiltersBooksAndEnchantments() throws SQLException {
        try (Db db = new Db(tempDir.resolve("shop-metadata-search.db").toFile())) {
            ShopRepository shops = new ShopRepository(db);

            ChestShopRow enchantedBook = shop("enchanted-book", "enchanted book#abc", null, true, false);
            enchantedBook.baseMaterial = "enchanted_book";
            enchantedBook.itemDetails = "{\"lore\":[\"§dA Lucky Find\"],\"enchants\":[" +
                    "{\"name\":\"mending\",\"level\":1}," +
                    "{\"name\":\"fire_aspect\",\"level\":2}]}";
            shops.upsert(enchantedBook);

            ChestShopRow strongSword = shop("strong-sword", "diamond sword#strong", null, true, false);
            strongSword.baseMaterial = "diamond_sword";
            strongSword.itemDetails = "{\"enchants\":[{\"name\":\"sharpness\",\"level\":5}]}";
            shops.upsert(strongSword);

            ChestShopRow weakSword = shop("weak-sword", "diamond sword#weak", null, true, false);
            weakSword.baseMaterial = "diamond_sword";
            weakSword.itemDetails = "{\"enchants\":[{\"name\":\"sharpness\",\"level\":2}]}";
            shops.upsert(weakSword);

            ChestShopRow writtenBook = shop("written-book", "signed story", null, true, false);
            writtenBook.baseMaterial = "written_book";
            shops.upsert(writtenBook);

            ChestShopRow plainPickaxe = shop("plain-pickaxe", "d pickaxe", null, true, false);
            plainPickaxe.baseMaterial = "diamond_pickaxe";
            shops.upsert(plainPickaxe);

            ChestShopRow malformed = shop("malformed", "bow", null, true, false);
            malformed.baseMaterial = "bow";
            malformed.itemDetails = "{not-json";
            shops.upsert(malformed);

            // A pre-metadata row must not be guessed into an exact item category.
            shops.upsert(shop("legacy-book", "enchanted book", null, true, false));

            assertEquals(Set.of("strong-sword", "weak-sword"),
                    ids(find(shops, "diamond sword", "", 0, ItemType.ALL)));
            assertEquals(Set.of("enchanted-book"),
                    ids(find(shops, "Fire Aspect", "", 0, ItemType.ALL)));
            assertEquals(Set.of("enchanted-book"),
                    ids(find(shops, "lucky find", "", 0, ItemType.ALL)));

            assertEquals(Set.of("strong-sword"),
                    ids(find(shops, "", "Sharpness", 3, ItemType.ALL)));
            assertEquals(1, shops.count("", "", "", "sharpness", 3, ItemType.ALL,
                    TradeType.BUY, "", false));

            assertEquals(Set.of("enchanted-book", "written-book"),
                    ids(find(shops, "", "", 0, ItemType.BOOKS)));
            assertEquals(Set.of("enchanted-book"),
                    ids(find(shops, "", "", 0, ItemType.ENCHANTED_BOOKS)));
            assertEquals(Set.of("enchanted-book", "strong-sword", "weak-sword"),
                    ids(find(shops, "", "", 0, ItemType.ENCHANTED_ITEMS)));
            assertEquals(Set.of("written-book", "plain-pickaxe"),
                    ids(find(shops, "", "", 0, ItemType.UNENCHANTED_ITEMS)));

            assertEquals(List.of("fire_aspect", "mending", "sharpness"),
                    shops.distinctEnchantmentNames(TradeType.BUY, ""));

            List<ChestShopRow> malformedResult = find(shops, "bow", "", 0, ItemType.ALL);
            assertEquals(Set.of("malformed"), ids(malformedResult));
            assertNull(DtoMappers.toChestShopDto(malformedResult.get(0)).getItemDetails());
        }
    }

    @Test
    void searchableEnchantmentsPreferExplicitMetadataAndSupportExactLevels() throws SQLException {
        try (Db db = new Db(tempDir.resolve("search-enchants.db").toFile())) {
            ShopRepository shops = new ShopRepository(db);

            ChestShopRow legacy = metadataShop("legacy-efficiency-v", "diamond_pickaxe",
                    "{\"enchants\":[{\"name\":\"efficiency\",\"level\":5}]}");
            shops.upsert(legacy);

            ChestShopRow titanSeven = metadataShop("titan-efficiency-vii", "netherite_pickaxe",
                    "{\"lore\":[\"§bEfficiency VII\",\"§6Ancient Power Ω\"]," +
                            "\"enchants\":[],\"searchEnchants\":[" +
                            "{\"name\":\"efficiency\",\"level\":7}]}");
            shops.upsert(titanSeven);

            ChestShopRow titanNine = metadataShop("titan-efficiency-ix", "netherite_pickaxe",
                    "{\"enchants\":[],\"searchEnchants\":[" +
                            "{\"name\":\"efficiency\",\"level\":9}]}");
            shops.upsert(titanNine);

            ChestShopRow visibleFortune = metadataShop("visible-fortune", "diamond_pickaxe",
                    "{\"enchants\":[{\"name\":\"fortune\",\"level\":3}]," +
                            "\"searchEnchants\":[{\"name\":\"fortune\",\"level\":3}]}");
            shops.upsert(visibleFortune);

            // searchEnchants is explicitly empty, so a cosmetic hidden enchant
            // must not leak into search, suggestions, or enchanted classification.
            ChestShopRow cosmeticSheen = metadataShop("cosmetic-sheen", "stick",
                    "{\"enchants\":[{\"name\":\"unbreaking\",\"level\":1}]," +
                            "\"searchEnchants\":[]}");
            shops.upsert(cosmeticSheen);

            assertEquals(Set.of("titan-efficiency-vii"),
                    ids(find(shops, "efficiency 7", "", 0, ItemType.ALL)));
            assertEquals(Set.of("legacy-efficiency-v", "titan-efficiency-vii", "titan-efficiency-ix"),
                    ids(find(shops, "efficiency", "", 0, ItemType.ALL)));
            assertEquals(Set.of(), ids(find(shops, "unbreaking", "", 0, ItemType.ALL)));

            assertEquals(Set.of("titan-efficiency-vii"),
                    ids(shops.find("", "", "", "efficiency", 0, 7,
                            ItemType.ALL, TradeType.BUY, "", false,
                            SortBy.MATERIAL, null, null)));
            assertEquals(1, shops.count("", "", "", "efficiency", 0, 7,
                    ItemType.ALL, TradeType.BUY, "", false));
            assertEquals(Set.of("titan-efficiency-vii", "titan-efficiency-ix"),
                    ids(shops.find("", "", "", "efficiency", 6, 0,
                            ItemType.ALL, TradeType.BUY, "", false,
                            SortBy.MATERIAL, null, null)));

            assertEquals(Set.of("legacy-efficiency-v", "titan-efficiency-vii",
                            "titan-efficiency-ix", "visible-fortune"),
                    ids(find(shops, "", "", 0, ItemType.ENCHANTED_ITEMS)));
            assertEquals(Set.of("cosmetic-sheen"),
                    ids(find(shops, "", "", 0, ItemType.UNENCHANTED_ITEMS)));

            assertEquals(List.of(
                            new ShopRepository.EnchantmentOption("efficiency", 5),
                            new ShopRepository.EnchantmentOption("efficiency", 7),
                            new ShopRepository.EnchantmentOption("efficiency", 9),
                            new ShopRepository.EnchantmentOption("fortune", 3)),
                    shops.distinctEnchantmentOptions(TradeType.BUY, ""));
            assertEquals(List.of("efficiency", "fortune"),
                    shops.distinctEnchantmentNames(TradeType.BUY, ""));

            // Search metadata stays internal; cards receive only the visible
            // tooltip fields (the Titan lore remains available for display).
            String titanJson = com.playtheatria.shopdb.web.Json.GSON.toJson(
                    DtoMappers.toChestShopDto(titanSeven));
            assertFalse(titanJson.contains("searchEnchants"));
        }
    }

    @Test
    void contextualEnchantmentOptionsComeFromTheCompleteFilteredResultSet() throws SQLException {
        try (Db db = new Db(tempDir.resolve("enchantment-facets.db").toFile())) {
            ShopRepository shops = new ShopRepository(db);

            ChestShopRow soulSpeedOne = metadataShop("soul-speed-i-book", "enchanted_book",
                    "{\"searchEnchants\":[{\"name\":\"soul_speed\",\"level\":1}]}");
            shops.upsert(soulSpeedOne);

            ChestShopRow soulSpeedThree = metadataShop("soul-speed-iii-boots", "diamond_boots",
                    "{\"searchEnchants\":[{\"name\":\"soul_speed\",\"level\":3}]}");
            shops.upsert(soulSpeedThree);

            ChestShopRow unavailableFive = metadataShop("soul-speed-v-unavailable", "netherite_boots",
                    "{\"searchEnchants\":[{\"name\":\"soul_speed\",\"level\":5}]}");
            unavailableFive.quantityAvailable = 0;
            shops.upsert(unavailableFive);

            ChestShopRow cosmeticMention = metadataShop("cosmetic-lore", "stick",
                    "{\"lore\":[\"Soul Speed display sample\"],\"searchEnchants\":[]}");
            shops.upsert(cosmeticMention);

            assertEquals(List.of(
                            new ShopRepository.EnchantmentOption("soul_speed", 1),
                            new ShopRepository.EnchantmentOption("soul_speed", 3)),
                    shops.distinctEnchantmentOptions("", "", "soul speed", "",
                            ItemType.ALL, TradeType.BUY, "", true));

            assertEquals(List.of(new ShopRepository.EnchantmentOption("soul_speed", 1)),
                    shops.distinctEnchantmentOptions("", "", "soul speed", "",
                            ItemType.BOOKS, TradeType.BUY, "", true));

            // Supplying the enchantment name (as an exact autocomplete search
            // does) still returns every available level; the level facet itself
            // is intentionally not applied to this query.
            assertEquals(List.of(
                            new ShopRepository.EnchantmentOption("soul_speed", 1),
                            new ShopRepository.EnchantmentOption("soul_speed", 3)),
                    shops.distinctEnchantmentOptions("", "", "", "soul_speed",
                            ItemType.ALL, TradeType.BUY, "", true));
        }
    }

    private static List<ChestShopRow> find(ShopRepository shops, String query, String enchantment,
                                           int minLevel, ItemType itemType) throws SQLException {
        return shops.find("", "", query, enchantment, minLevel, itemType, TradeType.BUY,
                "", false, SortBy.MATERIAL, null, null);
    }

    private static Set<String> ids(List<ChestShopRow> rows) {
        return rows.stream().map(row -> row.id).collect(Collectors.toSet());
    }

    private static ChestShopRow shop(String id, String material, String displayName,
                                     boolean buySign, boolean hidden) {
        ChestShopRow row = new ChestShopRow();
        row.id = id;
        row.server = "THE_ARK";
        row.material = material;
        row.displayNamePlain = displayName;
        row.quantity = 1;
        row.quantityAvailable = 1;
        row.buyPrice = 10.0;
        row.sellPrice = 10.0;
        row.buyPriceEach = 10.0;
        row.sellPriceEach = 10.0;
        row.isFull = false;
        row.isHidden = hidden;
        row.isBuySign = buySign;
        row.isSellSign = !buySign;
        return row;
    }

    private static ChestShopRow metadataShop(String id, String baseMaterial, String itemDetails) {
        ChestShopRow row = shop(id, baseMaterial.replace('_', ' '), null, true, false);
        row.baseMaterial = baseMaterial;
        row.itemDetails = itemDetails;
        return row;
    }
}
