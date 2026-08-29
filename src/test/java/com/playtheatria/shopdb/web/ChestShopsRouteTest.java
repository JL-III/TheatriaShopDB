package com.playtheatria.shopdb.web;

import com.playtheatria.shopdb.database.ChestShopRow;
import com.playtheatria.shopdb.database.ShopRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChestShopsRouteTest {
    @Test
    void distinctResultsKeepDifferentEnchantedVariants() {
        ChestShopRow mending = variant("{\"lore\":[\"a|b\"],\"enchants\":[" +
                "{\"name\":\"mending\",\"level\":1}," +
                "{\"name\":\"unbreaking\",\"level\":3}]}");
        ChestShopRow sharpness = variant("{\"enchants\":[{\"name\":\"sharpness\",\"level\":5}]}");
        ChestShopRow duplicateMending = variant("{ \"enchants\": [" +
                "{\"level\":3,\"name\":\"unbreaking\"}," +
                "{\"level\":1,\"name\":\"mending\"}], \"lore\": [\"a|b\"] }");

        List<ChestShopRow> result = ChestShopsRoute.distinctRows(
                List.of(mending, sharpness, duplicateMending));

        assertEquals(List.of(mending, sharpness), result);
    }

    @Test
    void distinctResultsTreatSearchEnchantOrderAsMetadataOrderIndependent() {
        ChestShopRow titan = variant("{\"searchEnchants\":[" +
                "{\"name\":\"efficiency\",\"level\":7}," +
                "{\"name\":\"unbreaking\",\"level\":5}]," +
                "\"lore\":[\"Efficiency VII\",\"Ancient Power Ω\"]}");
        ChestShopRow equivalentTitan = variant("{\"lore\":[\"Efficiency VII\",\"Ancient Power Ω\"]," +
                "\"searchEnchants\":[" +
                "{\"level\":5,\"name\":\"unbreaking\"}," +
                "{\"level\":7,\"name\":\"efficiency\"}]}");

        assertEquals(List.of(titan), ChestShopsRoute.distinctRows(List.of(titan, equivalentTitan)));
    }

    @Test
    void enchantmentOptionsHaveStableNameAndLevelJsonShape() {
        assertEquals("[{\"name\":\"efficiency\",\"level\":7}]", Json.GSON.toJson(List.of(
                new ShopRepository.EnchantmentOption("efficiency", 7))));
    }

    private static ChestShopRow variant(String itemDetails) {
        ChestShopRow row = new ChestShopRow();
        row.material = "enchanted book";
        row.baseMaterial = "enchanted_book";
        row.itemDetails = itemDetails;
        row.ownerId = 1L;
        row.townId = 2L;
        row.quantity = 1;
        return row;
    }
}
