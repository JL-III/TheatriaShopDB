package com.playtheatria.shopdb.models;

import com.playtheatria.shopdb.web.ApiException;

public enum TradeType {
    BUY,
    SELL;

    public static final String INVALID_TRADE_TYPE = "Invalid trade type. Must be one of: buy, sell";

    public static TradeType fromString(String s) {
        switch (s) {
            case "buy":
                return BUY;
            case "sell":
                return SELL;
            default:
                throw new ApiException(400, INVALID_TRADE_TYPE);
        }
    }
}
