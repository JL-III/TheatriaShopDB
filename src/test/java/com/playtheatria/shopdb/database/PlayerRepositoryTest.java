package com.playtheatria.shopdb.database;

import com.playtheatria.shopdb.models.SortBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void browseQueriesOnlyIncludePlayersWhoOwnShops() throws SQLException {
        try (Db db = new Db(tempDir.resolve("players.db").toFile())) {
            PlayerRepository players = new PlayerRepository(db);
            ShopRepository shops = new ShopRepository(db);
            RegionRepository regions = new RegionRepository(db);

            HashMap<String, PlayerRepository.PlayerRow> rows = players.getOrAdd(
                    Set.of("alpha", "beta", "hidden", "mayor_only"));

            shops.upsert(shop("alpha-1", rows.get("alpha").id, false));
            shops.upsert(shop("alpha-2", rows.get("alpha").id, false));
            shops.upsert(shop("beta-1", rows.get("beta").id, false));
            // Player cards count hidden shops, so a hidden-only owner is still a shop owner.
            shops.upsert(shop("hidden-1", rows.get("hidden").id, true));

            long firstTown = regions.upsert(region("first-town"));
            long secondTown = regions.upsert(region("second-town"));
            long thirdTown = regions.upsert(region("third-town"));
            regions.setMayors(firstTown, List.of(rows.get("alpha").id, rows.get("beta").id,
                    rows.get("mayor_only").id));
            regions.setMayors(secondTown, List.of(rows.get("beta").id, rows.get("mayor_only").id));
            regions.setMayors(thirdTown, List.of(rows.get("mayor_only").id));

            assertEquals(List.of("alpha", "beta", "hidden"),
                    names(players.page("", SortBy.NAME, 10, 0)));
            assertEquals(List.of("alpha", "beta", "hidden"),
                    names(players.page("", SortBy.NUM_CHEST_SHOPS, 10, 0)));
            assertEquals(List.of("beta", "alpha", "hidden"),
                    names(players.page("", SortBy.NUM_REGIONS, 10, 0)));

            assertEquals(3, players.count(""));
            assertEquals(0, players.count("mayor_only"));
            assertEquals(1, players.count("ALPHA"));
            assertEquals(List.of("alpha"), names(players.page("ALPHA", SortBy.NAME, 10, 0)));
            assertEquals(List.of("beta"), names(players.page("", SortBy.NAME, 1, 1)));
            assertEquals(List.of("alpha", "beta", "hidden"), players.names());

            shops.deleteById("alpha-1");
            shops.deleteById("alpha-2");
            assertEquals(List.of("beta", "hidden"), names(players.page("", SortBy.NAME, 10, 0)));
            assertEquals(2, players.count(""));
            assertEquals(List.of("beta", "hidden"), players.names());
        }
    }

    private static List<String> names(List<PlayerRepository.PlayerRow> rows) {
        return rows.stream().map(row -> row.name).toList();
    }

    private static ChestShopRow shop(String id, long ownerId, boolean hidden) {
        ChestShopRow row = new ChestShopRow();
        row.id = id;
        row.server = "THE_ARK";
        row.material = "dirt";
        row.ownerId = ownerId;
        row.isHidden = hidden;
        return row;
    }

    private static RegionRow region(String name) {
        RegionRow row = new RegionRow();
        row.name = name;
        row.server = "THE_ARK";
        row.active = true;
        return row;
    }
}
