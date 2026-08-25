package com.playtheatria.shopdb.updater;

import org.bukkit.Location;

import java.util.UUID;

public final class IDGenerator {
    private static final String LOCATION = "%d|%d|%d|%s";

    public static String generateID(Location location) {
        return generateID(location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
    }

    public static String generateID(int x, int y, int z, String s) {
        String location = String.format(LOCATION, x, y, z, s);
        return UUID.nameUUIDFromBytes(location.getBytes()).toString();
    }
}
