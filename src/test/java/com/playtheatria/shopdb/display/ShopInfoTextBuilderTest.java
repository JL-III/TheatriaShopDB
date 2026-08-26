package com.playtheatria.shopdb.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopInfoTextBuilderTest {
    @Test
    void putsCustomNameAndItemTypeOnSeparateLines() {
        Component displayName = Component.text("Daily Reward Crate Key", NamedTextColor.AQUA);
        Component realName = Component.translatable("item.minecraft.tripwire_hook");

        assertEquals(
                List.of(
                        displayName,
                        Component.text("(item:", NamedTextColor.GRAY)
                                .append(Component.translatable(
                                        "item.minecraft.tripwire_hook", NamedTextColor.GRAY))
                                .append(Component.text(")", NamedTextColor.GRAY))),
                ShopInfoTextBuilder.identityLines(displayName, realName));
    }

    @Test
    void plainItemKeepsOneWhiteLocalizedNameLine() {
        Component realName = Component.translatable("item.minecraft.bowl");

        assertEquals(
                List.of(Component.translatable("item.minecraft.bowl", NamedTextColor.WHITE)),
                ShopInfoTextBuilder.identityLines(null, realName));
    }

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

    @Test
    void estimatesExplicitLinesAndClientWrapping() {
        Component component = Component.text("Short line\n" + "x".repeat(33));

        assertEquals(3, ShopInfoTextBuilder.estimatedRenderedLineCount(component));
    }

    @Test
    void keepsEveryLoreLine() {
        List<Component> lore = IntStream.rangeClosed(1, 13)
                .<Component>mapToObj(line -> Component.text("Lore " + line))
                .toList();

        List<Component> styled = ShopInfoTextBuilder.styledLoreLines(lore);

        assertEquals(13, styled.size());
        assertEquals(
                Component.text("Lore 13", NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.ITALIC),
                styled.get(12));
    }
}
