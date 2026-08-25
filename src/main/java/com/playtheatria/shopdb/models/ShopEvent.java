package com.playtheatria.shopdb.models;

import java.math.BigDecimal;
import java.util.List;

public class ShopEvent {
    private String id; // Serialized value of X,Y,Z coordinates to uniquely identify this chest shop
    private EventType eventType;
    private String world;
    private List<RegionRequest> regions;
    private Integer x;
    private Integer y;
    private Integer z;
    private String owner;
    private Integer quantity;
    private Integer count;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private String item;
    private Boolean full;
    private String baseMaterial;
    private ItemDetailsDto itemDetails;

    public String getId() {
        return id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getWorld() {
        return world;
    }

    public List<RegionRequest> getRegions() {
        return regions;
    }

    public Integer getX() {
        return x;
    }

    public Integer getY() {
        return y;
    }

    public Integer getZ() {
        return z;
    }

    public String getOwner() {
        return owner;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getCount() {
        return count;
    }

    public BigDecimal getBuyPrice() {
        return buyPrice;
    }

    public BigDecimal getSellPrice() {
        return sellPrice;
    }

    public String getItem() {
        return item;
    }

    public Boolean getFull() {
        return full;
    }

    public String getBaseMaterial() {
        return baseMaterial;
    }

    public ItemDetailsDto getItemDetails() {
        return itemDetails;
    }

    @Override
    public String toString() {
        return "ShopEvent{" +
                "id='" + id + '\'' +
                ", eventType=" + eventType +
                ", world='" + world + '\'' +
                ", regions=" + regions +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", owner='" + owner + '\'' +
                ", quantity=" + quantity +
                ", count=" + count +
                ", buyPrice=" + buyPrice +
                ", sellPrice=" + sellPrice +
                ", item='" + item + '\'' +
                ", full=" + full +
                '}';
    }
}
