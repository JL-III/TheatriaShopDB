package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.ChestShopRow;
import com.playtheatria.shopdb.models.PriceSnapshotDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class PriceSnapshotCalculator {
    public static List<PriceSnapshotDto> calculate(List<ChestShopRow> shops) {
        TreeMap<String, List<ChestShopRow>> byMaterial = new TreeMap<>();
        for (ChestShopRow shop : shops) {
            if (isExcludedMaterial(shop.material)) continue;
            byMaterial.computeIfAbsent(shop.material, k -> new ArrayList<>()).add(shop);
        }

        List<PriceSnapshotDto> results = new ArrayList<>();
        for (Map.Entry<String, List<ChestShopRow>> entry : byMaterial.entrySet()) {
            List<ChestShopRow> buySigns = new ArrayList<>();
            List<ChestShopRow> sellSigns = new ArrayList<>();
            for (ChestShopRow shop : entry.getValue()) {
                if (Boolean.TRUE.equals(shop.isBuySign) && shop.buyPriceEach != null) buySigns.add(shop);
                if (Boolean.TRUE.equals(shop.isSellSign) && shop.sellPriceEach != null) sellSigns.add(shop);
            }
            if (buySigns.isEmpty() && sellSigns.isEmpty()) continue;

            int stockedCount = 0;
            int totalQuantityAvailable = 0;
            List<Double> allBuyPrices = new ArrayList<>();
            List<Double> stockedBuyPrices = new ArrayList<>();
            for (ChestShopRow shop : buySigns) {
                allBuyPrices.add(shop.buyPriceEach);
                if (shop.quantityAvailable != null) {
                    totalQuantityAvailable += shop.quantityAvailable;
                    if (shop.quantityAvailable > 0) {
                        stockedCount++;
                        stockedBuyPrices.add(shop.buyPriceEach);
                    }
                }
            }

            List<Double> sellPrices = new ArrayList<>();
            for (ChestShopRow shop : sellSigns) {
                sellPrices.add(shop.sellPriceEach);
            }

            PriceSnapshotDto dto = new PriceSnapshotDto();
            dto.setMaterial(entry.getKey());
            dto.setBuySignCount(buySigns.size());
            dto.setStockedBuySignCount(stockedCount);
            dto.setTotalQuantityAvailable(totalQuantityAvailable);
            dto.setMedianBuyPriceEach(median(stockedBuyPrices));
            dto.setMedianBuyPriceEachAll(median(allBuyPrices));
            dto.setMinBuyPriceEach(stockedBuyPrices.isEmpty() ? null : Collections.min(stockedBuyPrices));
            dto.setMaxBuyPriceEach(stockedBuyPrices.isEmpty() ? null : Collections.max(stockedBuyPrices));
            dto.setSellSignCount(sellSigns.size());
            dto.setMedianSellPriceEach(median(sellPrices));
            dto.setCommunityValue(stockedBuyPrices.isEmpty() ? dto.getMedianBuyPriceEachAll() : dto.getMedianBuyPriceEach());
            results.add(dto);
        }

        return results;
    }

    static Double median(List<Double> values) {
        if (values == null || values.isEmpty()) return null;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    // '#' marks ChestShop's metadata hash suffix (enchanted/renamed/custom items);
    // a bare enchanted book is excluded because its material alone cannot say which enchantment it carries.
    static boolean isExcludedMaterial(String material) {
        if (material == null) return true;
        if (material.contains("#")) return true;
        return material.replace(' ', '_').equals("enchanted_book");
    }
}
