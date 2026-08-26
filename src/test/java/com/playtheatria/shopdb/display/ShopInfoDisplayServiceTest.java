package com.playtheatria.shopdb.display;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopInfoDisplayServiceTest {
    @Test
    void keepsShortTextAtMaximumScale() {
        assertEquals(0.4f, ShopInfoDisplayService.textScaleForLineCount(6));
    }

    @Test
    void scalesLongTextByLineCount() {
        assertEquals(0.24f, ShopInfoDisplayService.textScaleForLineCount(10), 0.0001f);
    }

    @Test
    void keepsVeryLongTextAtMinimumReadableScale() {
        assertEquals(0.16f, ShopInfoDisplayService.textScaleForLineCount(20));
    }

    @Test
    void recognizesOnlyPositionsInFrontOfTheSignFace() {
        assertTrue(ShopInfoDisplayService.isPositionInFrontOfSign(
                0.5, 0.5, BlockFace.NORTH, 0.5, -2.0));
        assertFalse(ShopInfoDisplayService.isPositionInFrontOfSign(
                0.5, 0.5, BlockFace.NORTH, 0.5, 2.0));
        assertFalse(ShopInfoDisplayService.isPositionInFrontOfSign(
                0.5, 0.5, BlockFace.NORTH, 2.0, 0.5));
    }

    @Test
    void toleratesOneMissButNotTwo() {
        assertTrue(ShopInfoDisplayService.isWithinTargetMissGrace(1));
        assertFalse(ShopInfoDisplayService.isWithinTargetMissGrace(2));
    }

    @Test
    void confirmsAChangedTargetOnTheFirstObservation() {
        assertFalse(ShopInfoDisplayService.isTargetChangeConfirmed(0));
        assertTrue(ShopInfoDisplayService.isTargetChangeConfirmed(1));
    }
}
