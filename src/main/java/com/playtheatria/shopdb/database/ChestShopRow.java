package com.playtheatria.shopdb.database;

public class ChestShopRow {
    public String id;
    public String server;
    public int x;
    public int y;
    public int z;
    public String material;
    public Long ownerId;
    public Long townId;
    public Integer quantity;
    public Integer quantityAvailable;
    public Double buyPrice;
    public Double sellPrice;
    public Double buyPriceEach;
    public Double sellPriceEach;
    public Boolean isFull;
    public Boolean isHidden;
    public Boolean isBuySign;
    public Boolean isSellSign;

    // Filled by the joined list queries; null otherwise.
    public String ownerName;
    public String townName;
}
