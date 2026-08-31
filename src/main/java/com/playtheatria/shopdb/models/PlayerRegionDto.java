package com.playtheatria.shopdb.models;

public class PlayerRegionDto {
    private String name;
    private Server server;
    private ShopLocationType type;
    private String travelCommand;

    public String getName() {
        return name;
    }

    public Server getServer() {
        return server;
    }

    public ShopLocationType getType() {
        return type;
    }

    public String getTravelCommand() {
        return travelCommand;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public void setType(ShopLocationType type) {
        this.type = type;
    }

    public void setTravelCommand(String travelCommand) {
        this.travelCommand = travelCommand;
    }
}
