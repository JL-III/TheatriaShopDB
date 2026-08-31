package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.ChestShopRow;
import com.playtheatria.shopdb.database.Db;
import com.playtheatria.shopdb.database.RegionRepository;
import com.playtheatria.shopdb.database.RegionRow;
import com.playtheatria.shopdb.database.ShopRepository;
import com.playtheatria.shopdb.models.ShopLocationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChestShopPublicationTest {
    @TempDir
    Path tempDir;

    @Test
    void publishingIrregularPlayerLandNeverUsesItsBoundingBox() throws SQLException {
        try (Db db = new Db(tempDir.resolve("publication.db").toFile())) {
            RegionRepository regions = new RegionRepository(db);
            ShopRepository shops = new ShopRepository(db);
            ChestShopIngestService ingest = new ChestShopIngestService(
                    shops, null, null, Logger.getAnonymousLogger());

            RegionRow land = location(ShopLocationType.PLAYER_SHOP, "player-land");
            regions.upsert(land);

            ChestShopRow claimed = shop("claimed", 5, 5);
            claimed.townId = land.id;
            shops.upsert(claimed);

            // Geometrically inside the broad AABB, but deliberately not associated by the Lands resolver.
            ChestShopRow inUnclaimedHole = shop("hole", 10, 10);
            shops.upsert(inUnclaimedHole);

            ingest.linkAndShowChestShops(land);

            assertEquals(List.of("claimed"),
                    shops.findVisible("").stream().map(row -> row.id).toList());
        }
    }

    private static RegionRow location(ShopLocationType type, String name) {
        RegionRow row = new RegionRow();
        row.name = name;
        row.server = "THE_ARK";
        row.type = type;
        row.externalId = name;
        row.active = true;
        row.iX = 0;
        row.iY = 0;
        row.iZ = 0;
        row.oX = 31;
        row.oY = 255;
        row.oZ = 31;
        return row;
    }

    private static ChestShopRow shop(String id, int x, int z) {
        ChestShopRow row = new ChestShopRow();
        row.id = id;
        row.server = "THE_ARK";
        row.x = x;
        row.y = 64;
        row.z = z;
        row.material = "dirt";
        row.quantity = 1;
        row.quantityAvailable = 1;
        row.buyPrice = 1.0;
        row.buyPriceEach = 1.0;
        row.isBuySign = true;
        row.isSellSign = false;
        row.isFull = false;
        row.isHidden = true;
        return row;
    }
}
