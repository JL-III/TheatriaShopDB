package com.playtheatria.shopdb.models;

public class PriceSnapshotDto {
    private String material;
    private Integer buySignCount;
    private Integer stockedBuySignCount;
    private Integer totalQuantityAvailable;
    private Double medianBuyPriceEach;
    private Double medianBuyPriceEachAll;
    private Double minBuyPriceEach;
    private Double maxBuyPriceEach;
    private Integer sellSignCount;
    private Double medianSellPriceEach;
    private Double communityValue;

    public String getMaterial() {
        return material;
    }

    public Integer getBuySignCount() {
        return buySignCount;
    }

    public Integer getStockedBuySignCount() {
        return stockedBuySignCount;
    }

    public Integer getTotalQuantityAvailable() {
        return totalQuantityAvailable;
    }

    public Double getMedianBuyPriceEach() {
        return medianBuyPriceEach;
    }

    public Double getMedianBuyPriceEachAll() {
        return medianBuyPriceEachAll;
    }

    public Double getMinBuyPriceEach() {
        return minBuyPriceEach;
    }

    public Double getMaxBuyPriceEach() {
        return maxBuyPriceEach;
    }

    public Integer getSellSignCount() {
        return sellSignCount;
    }

    public Double getMedianSellPriceEach() {
        return medianSellPriceEach;
    }

    public Double getCommunityValue() {
        return communityValue;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public void setBuySignCount(Integer buySignCount) {
        this.buySignCount = buySignCount;
    }

    public void setStockedBuySignCount(Integer stockedBuySignCount) {
        this.stockedBuySignCount = stockedBuySignCount;
    }

    public void setTotalQuantityAvailable(Integer totalQuantityAvailable) {
        this.totalQuantityAvailable = totalQuantityAvailable;
    }

    public void setMedianBuyPriceEach(Double medianBuyPriceEach) {
        this.medianBuyPriceEach = medianBuyPriceEach;
    }

    public void setMedianBuyPriceEachAll(Double medianBuyPriceEachAll) {
        this.medianBuyPriceEachAll = medianBuyPriceEachAll;
    }

    public void setMinBuyPriceEach(Double minBuyPriceEach) {
        this.minBuyPriceEach = minBuyPriceEach;
    }

    public void setMaxBuyPriceEach(Double maxBuyPriceEach) {
        this.maxBuyPriceEach = maxBuyPriceEach;
    }

    public void setSellSignCount(Integer sellSignCount) {
        this.sellSignCount = sellSignCount;
    }

    public void setMedianSellPriceEach(Double medianSellPriceEach) {
        this.medianSellPriceEach = medianSellPriceEach;
    }

    public void setCommunityValue(Double communityValue) {
        this.communityValue = communityValue;
    }
}
