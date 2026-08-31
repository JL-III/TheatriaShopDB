package com.playtheatria.shopdb.models;

public class ChestShopRegionDto {
    private String name;
    private ShopLocationType type;
    private String travelCommand;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ShopLocationType getType() {
        return type;
    }

    public void setType(ShopLocationType type) {
        this.type = type;
    }

    public String getTravelCommand() {
        return travelCommand;
    }

    public void setTravelCommand(String travelCommand) {
        this.travelCommand = travelCommand;
    }
}
