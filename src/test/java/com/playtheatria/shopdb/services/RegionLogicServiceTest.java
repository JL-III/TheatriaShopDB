package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.Db;
import com.playtheatria.shopdb.database.PlayerRepository;
import com.playtheatria.shopdb.database.RegionRepository;
import com.playtheatria.shopdb.database.RegionRow;
import com.playtheatria.shopdb.models.Location;
import com.playtheatria.shopdb.models.RegionRequest;
import com.playtheatria.shopdb.models.ShopLocationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionLogicServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void landsUlidSurvivesRenameWithoutDuplicatingOrUnlisting() throws SQLException {
        try (Db db = new Db(tempDir.resolve("rename.db").toFile())) {
            RegionRepository regions = new RegionRepository(db);
            RegionLogicService service = new RegionLogicService(
                    regions, new PlayerRepository(db), Logger.getAnonymousLogger());

            RegionRow listed = service.listRegion(playerLand("Old Name", "01STABLELAND"));
            RegionRequest renamed = playerLand("New Name", "01STABLELAND");
            service.upsertRegions(Set.of(renamed), new HashMap<>());

            RegionRow stored = regions.findByServerTypeAndExternalId(
                    "THE_ARK", ShopLocationType.PLAYER_SHOP, "01STABLELAND");
            assertNotNull(stored);
            assertEquals(listed.id, stored.id);
            assertEquals("new name", stored.name);
            assertTrue(stored.active);
            assertEquals(1, regions.count("", "", ShopLocationType.PLAYER_SHOP));
        }
    }

    @Test
    void listedMarketWinsOverlapThenListedPlayerThenUnlistedPlayer() {
        RegionLogicService service = new RegionLogicService(null, null, Logger.getAnonymousLogger());
        RegionRow market = row(ShopLocationType.MARKET_STALL, true, "market");
        RegionRow player = row(ShopLocationType.PLAYER_SHOP, true, "player");
        RegionRow hiddenPlayer = row(ShopLocationType.PLAYER_SHOP, false, "hidden-player");
        RegionRow hiddenMarket = row(ShopLocationType.MARKET_STALL, false, "hidden-market");

        assertEquals(market, service.findActiveOrSmallest(List.of(player, market)));
        assertEquals(player, service.findActiveOrSmallest(List.of(hiddenMarket, player)));
        assertEquals(hiddenPlayer, service.findActiveOrSmallest(List.of(hiddenMarket, hiddenPlayer)));
    }

    private static RegionRequest playerLand(String name, String externalId) {
        RegionRequest request = new RegionRequest();
        request.setName(name);
        request.setServer("The_Ark");
        request.setType(ShopLocationType.PLAYER_SHOP);
        request.setExternalId(externalId);
        request.setiBounds(new Location(0, 0, 0));
        request.setoBounds(new Location(31, 255, 31));
        request.setMayorNames(Set.of());
        return request;
    }

    private static RegionRow row(ShopLocationType type, boolean active, String id) {
        RegionRow row = new RegionRow();
        row.type = type;
        row.active = active;
        row.externalId = id;
        row.name = id;
        row.oX = 15;
        row.oY = 255;
        row.oZ = 15;
        return row;
    }
}
