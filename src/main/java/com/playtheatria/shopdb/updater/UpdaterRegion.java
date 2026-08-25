package com.playtheatria.shopdb.updater;

import java.util.Set;

/**
 * Wire model for regions attached to shop events (ShopDB-Updater's Region).
 * Serialized with Gson; field names are part of the ingest protocol.
 */
public class UpdaterRegion {
    private String name;
    private String server;
    private Set<String> owners;
    private UpdaterBounds iBounds;
    private UpdaterBounds oBounds;

    public String getName() {
        return name;
    }

    public UpdaterBounds getiBounds() {
        if (iBounds == null) iBounds = new UpdaterBounds();
        return iBounds;
    }

    public UpdaterBounds getoBounds() {
        if (oBounds == null) oBounds = new UpdaterBounds();
        return oBounds;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setOwners(Set<String> owners) {
        this.owners = owners;
    }
}
