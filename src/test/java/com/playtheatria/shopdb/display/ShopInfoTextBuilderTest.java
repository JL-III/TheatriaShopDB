package com.playtheatria.shopdb.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopInfoTextBuilderTest {
    @Test
    void adminShopIsAlwaysInStock() {
        assertEquals(
                List.of(Component.text("Always in stock", NamedTextColor.GREEN)),
                ShopInfoTextBuilder.stockLines(true, null, null, false, false));
    }

    @Test
    void missingContainerHasNoStockLine() {
        assertEquals(
                List.of(),
                ShopInfoTextBuilder.stockLines(false, null, 1, true, false));
    }

    @Test
    void quantityBoundaryIsInStockAndIncludesGrayCount() {
        assertEquals(
                List.of(Component.text("In stock", NamedTextColor.GREEN)
                        .append(Component.text(" (128)", NamedTextColor.GRAY))),
                ShopInfoTextBuilder.stockLines(false, 128, 128, true, false));
    }

    @Test
    void insufficientBuyStockIsOutOfStock() {
        assertEquals(
                List.of(Component.text("Out of stock", NamedTextColor.RED)),
                ShopInfoTextBuilder.stockLines(false, 15, 16, true, false));
    }

    @Test
    void fullSellShopAddsLineAfterBuyStatus() {
        assertEquals(
                List.of(
                        Component.text("In stock", NamedTextColor.GREEN)
                                .append(Component.text(" (128)", NamedTextColor.GRAY)),
                        Component.text("Shop full", NamedTextColor.RED)),
                ShopInfoTextBuilder.stockLines(false, 128, 16, true, true));
    }

    @Test
    void sellOnlyShopCanShowFull() {
        assertEquals(
                List.of(Component.text("Shop full", NamedTextColor.RED)),
                ShopInfoTextBuilder.stockLines(false, 128, 16, false, true));
    }
}
