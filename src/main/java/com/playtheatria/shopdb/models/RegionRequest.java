package com.playtheatria.shopdb.models;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;
import java.util.Set;

public class RegionRequest {
    private String name;
    private String server;
    private ShopLocationType type = ShopLocationType.MARKET_STALL;
    private String externalId;
    private Location iBounds;
    private Location oBounds;
    @SerializedName("owners")
    private Set<String> mayorNames;
    private Boolean active = false;

    public String getName() {
        return name;
    }

    public String getServer() {
        return server;
    }

    public ShopLocationType getType() {
        return type == null ? ShopLocationType.MARKET_STALL : type;
    }

    public String getExternalId() {
        return externalId == null || externalId.isBlank() ? name : externalId;
    }

    public Location getiBounds() {
        return iBounds;
    }

    public Location getoBounds() {
        return oBounds;
    }

    public Set<String> getMayorNames() {
        return mayorNames;
    }

    public Boolean getActive() {
        return active;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setType(ShopLocationType type) {
        this.type = type;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setiBounds(Location iBounds) {
        this.iBounds = iBounds;
    }

    public void setoBounds(Location oBounds) {
        this.oBounds = oBounds;
    }

    public void setMayorNames(Set<String> mayorNames) {
        this.mayorNames = mayorNames;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegionRequest that = (RegionRequest) o;
        return Objects.equals(server, that.server)
                && getType() == that.getType()
                && Objects.equals(getExternalId(), that.getExternalId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(server, getType(), getExternalId());
    }

    @Override
    public String toString() {
        return "RegionRequest{name='" + name + "', server='" + server + "', type=" + getType()
                + ", externalId='" + getExternalId() + "'}";
    }
}
