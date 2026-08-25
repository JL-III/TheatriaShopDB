package com.playtheatria.shopdb.updater;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.playtheatria.shopdb.models.EventType;

import java.math.BigDecimal;

/** The buffered event row (ShopDB-Updater's ShopEvent entity, table and columns unchanged). */
@DatabaseTable(tableName = "shop_events")
public class BufferedShopEvent {
    @DatabaseField(id = true)
    public String id; // Serialized value of X,Y,Z coordinates + world to uniquely identify this chest shop

    @DatabaseField
    public EventType eventType;

    @DatabaseField
    public String regions; // JSON array of UpdaterRegion

    @DatabaseField
    public String world;

    @DatabaseField
    public Integer x;

    @DatabaseField
    public Integer y;

    @DatabaseField
    public Integer z;

    @DatabaseField
    public String owner;

    @DatabaseField
    public Integer quantity;

    @DatabaseField
    public Integer count;

    @DatabaseField
    public BigDecimal buyPrice;

    @DatabaseField
    public BigDecimal sellPrice;

    @DatabaseField
    public String item;

    @DatabaseField
    public Boolean full;

    @DatabaseField
    public String baseMaterial;

    @DatabaseField
    public String itemDetails; // JSON of ItemDetailsDto, null when the item has none
}
