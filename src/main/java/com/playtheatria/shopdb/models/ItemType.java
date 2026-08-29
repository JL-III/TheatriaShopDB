package com.playtheatria.shopdb.models;

import com.playtheatria.shopdb.web.ApiException;

/** Optional item-metadata category used by the chest-shop search. */
public enum ItemType {
    ALL(0),
    BOOKS(1),
    ENCHANTED_BOOKS(2),
    ENCHANTED_ITEMS(3),
    UNENCHANTED_ITEMS(4);

    public static final String INVALID_ITEM_TYPE =
            "Invalid item type. Must be one of: all, books, enchanted-books, " +
                    "enchanted-items, unenchanted-items";

    private final int queryCode;

    ItemType(int queryCode) {
        this.queryCode = queryCode;
    }

    public int queryCode() {
        return queryCode;
    }

    public static ItemType fromString(String value) {
        if (value == null || value.isEmpty() || "all".equals(value)) return ALL;
        switch (value) {
            case "books":
                return BOOKS;
            case "enchanted-books":
                return ENCHANTED_BOOKS;
            case "enchanted-items":
                return ENCHANTED_ITEMS;
            case "unenchanted-items":
                return UNENCHANTED_ITEMS;
            default:
                throw new ApiException(400, INVALID_ITEM_TYPE);
        }
    }
}
