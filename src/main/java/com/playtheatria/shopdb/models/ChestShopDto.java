package com.playtheatria.shopdb.models;

// Field names and declaration order mirror the previous backend's JSON exactly
// (Jackson used bean names: isFull -> "full", isBuySign -> "buySign", isSellSign -> "sellSign").
public class ChestShopDto {
    private Server server;
    private Location location;
    private String material;
    private ChestShopPlayerDto owner;
    private ChestShopRegionDto town;
    private Integer quantity;
    private Integer quantityAvailable;
    private Double buyPrice;
    private Double sellPrice;
    private Double buyPriceEach;
    private Double sellPriceEach;
    private Boolean full;
    private Boolean buySign;
    private Boolean sellSign;

    public Server getServer() {
        return server;
    }

    public Location getLocation() {
        return location;
    }

    public String getMaterial() {
        return material;
    }

    public ChestShopPlayerDto getOwner() {
        return owner;
    }

    public ChestShopRegionDto getTown() {
        return town;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getQuantityAvailable() {
        return quantityAvailable;
    }

    public Double getBuyPrice() {
        return buyPrice;
    }

    public Double getSellPrice() {
        return sellPrice;
    }

    public Double getBuyPriceEach() {
        return buyPriceEach;
    }

    public Double getSellPriceEach() {
        return sellPriceEach;
    }

    public Boolean getFull() {
        return full;
    }

    public Boolean getBuySign() {
        return buySign;
    }

    public Boolean getSellSign() {
        return sellSign;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public void setOwner(ChestShopPlayerDto owner) {
        this.owner = owner;
    }

    public void setTown(ChestShopRegionDto town) {
        this.town = town;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setQuantityAvailable(Integer quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public void setBuyPrice(Double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public void setSellPrice(Double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public void setBuyPriceEach(Double buyPriceEach) {
        this.buyPriceEach = buyPriceEach;
    }

    public void setSellPriceEach(Double sellPriceEach) {
        this.sellPriceEach = sellPriceEach;
    }

    public void setFull(Boolean full) {
        this.full = full;
    }

    public void setBuySign(Boolean buySign) {
        this.buySign = buySign;
    }

    public void setSellSign(Boolean sellSign) {
        this.sellSign = sellSign;
    }
}
