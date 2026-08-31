package com.playtheatria.shopdb.models;

import java.util.Locale;

/** Player-facing category for a group of chest shops. */
public enum ShopLocationType {
    MARKET_STALL,
    PLAYER_SHOP;

    public static ShopLocationType fromString(String value) {
        if (value == null || value.isBlank()) return null;

        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        if ("MARKET".equals(normalized) || "WORLDGUARD".equals(normalized)) {
            return MARKET_STALL;
        }
        if ("PLAYER".equals(normalized) || "LAND".equals(normalized) || "LANDS".equals(normalized)) {
            return PLAYER_SHOP;
        }

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
