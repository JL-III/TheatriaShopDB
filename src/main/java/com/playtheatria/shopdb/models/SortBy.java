package com.playtheatria.shopdb.models;

import com.playtheatria.shopdb.web.ApiException;

public enum SortBy {
    BEST_PRICE,
    QUANTITY,
    MATERIAL,
    QUANTITY_AVAILABLE,
    NUM_CHEST_SHOPS,
    NUM_REGIONS,
    NUM_PLAYERS,
    NAME;

    public static final String INVALID_SORT_BY = "Invalid sort by. Must be one of: best-price, quantity, material, quantity-available";

    public static SortBy fromString(String s) {
        switch (s) {
            case "best-price":
                return BEST_PRICE;
            case "quantity":
                return QUANTITY;
            case "material":
                return MATERIAL;
            case "quantity-available":
                return QUANTITY_AVAILABLE;
            case "num-chest-shops":
                return NUM_CHEST_SHOPS;
            case "num-regions":
                return NUM_REGIONS;
            case "num-players":
                return NUM_PLAYERS;
            case "name":
                return NAME;
            default:
                throw new ApiException(400, INVALID_SORT_BY);
        }
    }
}
