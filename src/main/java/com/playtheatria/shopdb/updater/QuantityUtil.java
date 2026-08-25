package com.playtheatria.shopdb.updater;

public final class QuantityUtil {
    public static int parseQuantity(String quantityLine) {
        return Integer.parseInt(quantityLine.split(" : ")[0].replace("Q ", ""));
    }
}
