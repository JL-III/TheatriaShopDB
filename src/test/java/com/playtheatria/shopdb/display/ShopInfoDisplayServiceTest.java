package com.playtheatria.shopdb.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopInfoDisplayServiceTest {
    @Test
    void keepsShortTextAtMaximumScale() {
        assertEquals(0.2f, ShopInfoDisplayService.textScaleForLineCount(6));
    }

    @Test
    void scalesLongTextByLineCount() {
        assertEquals(0.12f, ShopInfoDisplayService.textScaleForLineCount(10), 0.0001f);
    }

    @Test
    void keepsVeryLongTextAtMinimumReadableScale() {
        assertEquals(0.08f, ShopInfoDisplayService.textScaleForLineCount(20));
    }
}
