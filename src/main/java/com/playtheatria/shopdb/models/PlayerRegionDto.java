package com.playtheatria.shopdb.models;

public class PlayerRegionDto {
    private String name;
    private Server server;

    public String getName() {
        return name;
    }

    public Server getServer() {
        return server;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServer(Server server) {
        this.server = server;
    }
}
