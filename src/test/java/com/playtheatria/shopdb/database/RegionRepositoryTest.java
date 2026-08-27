package com.playtheatria.shopdb.database;

import com.playtheatria.shopdb.models.SortBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
