package com.playtheatria.shopdb.models;

import java.sql.Timestamp;
import java.util.List;

public class RegionDto {
    private String name;
    private Server server;
    private ShopLocationType type;
    private String externalId;
    private String travelCommand;
    private Location iBounds;
    private Location oBounds;
    private int numChestShops;
    private Boolean active;
    private List<RegionPlayerDto> mayors;
    private Timestamp lastUpdated;

    public String getName() {
        return name;
    }

    public Server getServer() {
        return server;
    }

    public ShopLocationType getType() {
        return type;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTravelCommand() {
        return travelCommand;
    }

    public Location getiBounds() {
        return iBounds;
    }

    public Location getoBounds() {
        return oBounds;
    }

    public int getNumChestShops() {
        return numChestShops;
    }

    public Boolean getActive() {
        return active;
    }

    public List<RegionPlayerDto> getMayors() {
        return mayors;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
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

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setTravelCommand(String travelCommand) {
        this.travelCommand = travelCommand;
    }

    public void setiBounds(Location iBounds) {
        this.iBounds = iBounds;
    }

    public void setoBounds(Location oBounds) {
        this.oBounds = oBounds;
    }

    public void setNumChestShops(int numChestShops) {
        this.numChestShops = numChestShops;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setMayors(List<RegionPlayerDto> mayors) {
        this.mayors = mayors;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
