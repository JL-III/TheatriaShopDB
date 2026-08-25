package com.playtheatria.shopdb.updater;

import com.playtheatria.shopdb.models.EventType;
import com.playtheatria.shopdb.models.ItemDetailsDto;

import java.math.BigDecimal;

/** The wire payload for POST /chest-shops (ShopDB-Updater's ShopEventDTO). */
public class ShopEventDTO {
    private String id;
    private EventType eventType;
    private UpdaterRegion[] regions;
    private String world;
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

    public void setId(String id) {
        this.id = id;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void setRegions(UpdaterRegion[] regions) {
        this.regions = regions;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public void setZ(Integer z) {
        this.z = z;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public void setBuyPrice(BigDecimal buyPrice) {
        this.buyPrice = buyPrice;
    }

    public void setSellPrice(BigDecimal sellPrice) {
        this.sellPrice = sellPrice;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public void setFull(Boolean full) {
        this.full = full;
    }

    public void setBaseMaterial(String baseMaterial) {
        this.baseMaterial = baseMaterial;
    }

    public void setItemDetails(ItemDetailsDto itemDetails) {
        this.itemDetails = itemDetails;
    }
}
