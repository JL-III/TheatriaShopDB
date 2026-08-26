package com.playtheatria.shopdb.display;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;

final class ActiveDisplay {
    final Location signLocation;
    final String itemLine;
    final ItemStack item;
    final ItemDisplay itemEntity;
    final TextDisplay textEntity;
    float spinAngleDeg;
    int ticksSinceStockRefresh;
    int missedTargetScans;

    ActiveDisplay(Location signLocation, String itemLine, ItemStack item,
                  ItemDisplay itemEntity, TextDisplay textEntity) {
        this.signLocation = signLocation;
        this.itemLine = itemLine;
        this.item = item;
        this.itemEntity = itemEntity;
        this.textEntity = textEntity;
    }
}
