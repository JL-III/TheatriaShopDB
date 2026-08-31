package com.playtheatria.shopdb.updater;

import org.bukkit.Location;

/** Optional claim-plugin bridge used by shop ingestion and player publishing. */
public interface PlayerShopResolver {
    PlayerShopClaim findClaim(Location location);

    default boolean isAvailable() {
        return true;
    }

    static PlayerShopResolver unavailable() {
        return new PlayerShopResolver() {
            @Override
            public PlayerShopClaim findClaim(Location location) {
                return null;
            }

            @Override
            public boolean isAvailable() {
                return false;
            }
        };
    }
}
