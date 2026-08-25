package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.ChestShopRow;
import com.playtheatria.shopdb.models.PriceSnapshotDto;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PriceSnapshotCalculatorTest {

    private ChestShopRow buySign(String material, Double priceEach, Integer stock) {
        ChestShopRow shop = new ChestShopRow();
        shop.material = material;
        shop.isBuySign = Boolean.TRUE;
        shop.buyPriceEach = priceEach;
        shop.quantityAvailable = stock;
        return shop;
    }

    private ChestShopRow sellSign(String material, Double priceEach) {
        ChestShopRow shop = new ChestShopRow();
        shop.material = material;
        shop.isSellSign = Boolean.TRUE;
        shop.sellPriceEach = priceEach;
        return shop;
    }

    @Test
    public void medianOddCountReturnsMiddleValue() {
        assertEquals(2.0, PriceSnapshotCalculator.median(Arrays.asList(1.0, 3.0, 2.0)));
    }

    @Test
    public void medianEvenCountReturnsMeanOfMiddleTwo() {
        assertEquals(2.5, PriceSnapshotCalculator.median(Arrays.asList(1.0, 2.0, 3.0, 10.0)));
    }

    @Test
    public void medianEmptyReturnsNull() {
        assertNull(PriceSnapshotCalculator.median(Collections.emptyList()));
        assertNull(PriceSnapshotCalculator.median(null));
    }

    @Test
    public void hashMaterialIsExcluded() {
        List<PriceSnapshotDto> results = PriceSnapshotCalculator.calculate(
                Collections.singletonList(buySign("diamond_sword#ab12", 5.0, 1)));
        assertTrue(results.isEmpty());
    }

    @Test
    public void enchantedBookIsExcluded() {
        List<PriceSnapshotDto> results = PriceSnapshotCalculator.calculate(Arrays.asList(
                buySign("enchanted_book", 5.0, 1),
                buySign("enchanted book", 5.0, 1)));
        assertTrue(results.isEmpty());
    }

    @Test
    public void enchantingTableIsNotExcluded() {
        List<PriceSnapshotDto> results = PriceSnapshotCalculator.calculate(
                Collections.singletonList(buySign("enchanting_table", 5.0, 1)));
        assertEquals(1, results.size());
        assertEquals("enchanting_table", results.get(0).getMaterial());
    }

    @Test
    public void outlierDoesNotSkewMedian() {
        List<PriceSnapshotDto> results = PriceSnapshotCalculator.calculate(Arrays.asList(
                buySign("stone", 10.0, 5),
                buySign("stone", 10.0, 5),
                buySign("stone", 10.0, 5),
                buySign("stone", 10.0, 5),
                buySign("stone", 10.0, 5),
                buySign("stone", 10000.0, 5)));
        assertEquals(1, results.size());
        assertEquals(10.0, results.get(0).getMedianBuyPriceEach());
    }

    @Test
    public void communityValuePrefersStockedMedian() {
        List<PriceSnapshotDto> results = PriceSnapshotCalculator.calculate(Arrays.asList(
                buySign("stone", 10.0, 5),
                buySign("stone", 10.0, 3),
                buySign("stone", 50.0, 0)));
        assertEquals(1, results.size());
        assertEquals(10.0, results.get(0).getCommunityValue());
    }

    @Test
    public void communityValueFallsBackToAllMedianWhenNothingStocked() {
        List<PriceSnapshotDto> results = PriceSnapshotCalculator.calculate(
                Collections.singletonList(buySign("stone", 50.0, 0)));
        assertEquals(1, results.size());
        PriceSnapshotDto dto = results.get(0);
        assertEquals(0, dto.getStockedBuySignCount());
        assertNull(dto.getMedianBuyPriceEach());
        assertEquals(50.0, dto.getCommunityValue());
    }

    @Test
    public void sellOnlyMaterialHasNullBuyFields() {
        List<PriceSnapshotDto> results = PriceSnapshotCalculator.calculate(
                Collections.singletonList(sellSign("stone", 2.5)));
        assertEquals(1, results.size());
        PriceSnapshotDto dto = results.get(0);
        assertEquals(0, dto.getBuySignCount());
        assertNull(dto.getCommunityValue());
        assertEquals(1, dto.getSellSignCount());
        assertEquals(2.5, dto.getMedianSellPriceEach());
    }

    @Test
    public void buySignWithoutPriceIsIgnored() {
        List<PriceSnapshotDto> results = PriceSnapshotCalculator.calculate(
                Collections.singletonList(buySign("stone", null, 5)));
        assertTrue(results.isEmpty());
    }

    @Test
    public void totalQuantityTreatsNullAsZero() {
        List<PriceSnapshotDto> results = PriceSnapshotCalculator.calculate(Arrays.asList(
                buySign("stone", 10.0, 5),
                buySign("stone", 10.0, null)));
        assertEquals(1, results.size());
        assertEquals(5, results.get(0).getTotalQuantityAvailable());
    }

    @Test
    public void resultsSortedByMaterialAscending() {
        List<PriceSnapshotDto> results = PriceSnapshotCalculator.calculate(Arrays.asList(
                buySign("stone", 10.0, 5),
                buySign("dirt", 1.0, 5)));
        assertEquals(2, results.size());
        assertEquals("dirt", results.get(0).getMaterial());
        assertEquals("stone", results.get(1).getMaterial());
    }
}
