package com.playtheatria.shopdb.models;

import java.sql.Timestamp;
import java.util.List;

public class PlayerDto {
    private String name;
    private Timestamp lastSeen;
    private Timestamp lastUpdated;
    private int numChestShops;
    private List<PlayerRegionDto> towns;

    public String getName() {
        return name;
    }

    public Timestamp getLastSeen() {
        return lastSeen;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public int getNumChestShops() {
        return numChestShops;
    }

    public List<PlayerRegionDto> getTowns() {
        return towns;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLastSeen(Timestamp lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setNumChestShops(int numChestShops) {
        this.numChestShops = numChestShops;
    }

    public void setTowns(List<PlayerRegionDto> towns) {
        this.towns = towns;
    }
}
