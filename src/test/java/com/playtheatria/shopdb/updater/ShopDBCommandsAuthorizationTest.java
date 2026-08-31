package com.playtheatria.shopdb.updater;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopDBCommandsAuthorizationTest {

    @ParameterizedTest(name = "owner={0}, admin permission={1} => allowed={2}")
    @CsvSource({
            "false, true,  true",
            "true,  true,  true",
            "false, false, false",
            "true,  false, false"
    })
    void regionModificationIsControlledOnlyByAdminPermission(
            boolean ownsRegion,
            boolean hasAdminPermission,
            boolean expected
    ) {
        assertEquals(expected, ShopDBCommands.canModifyRegion(ownsRegion, hasAdminPermission));
    }

    @org.junit.jupiter.api.Test
    void playerShopModificationRequiresExactLandOwnerUuid() {
        UUID owner = UUID.randomUUID();

        assertTrue(ShopDBCommands.canModifyLand(owner, owner));
        assertFalse(ShopDBCommands.canModifyLand(UUID.randomUUID(), owner));
        assertFalse(ShopDBCommands.canModifyLand(null, owner));
        assertFalse(ShopDBCommands.canModifyLand(owner, null));
    }
}
