package com.playtheatria.shopdb.models;

import com.google.gson.annotations.SerializedName;
import com.playtheatria.shopdb.web.ApiException;

public enum Server {
    @SerializedName("The_Ark")
    THE_ARK;

    public static final String INVALID_SERVER = "Invalid server. Must be one of: 'the_ark'.";

    public static Server fromString(String s) {
        if (s == null || s.isEmpty()) return null;
        if ("The_Ark".equals(s)) {
            return THE_ARK;
        }
        throw new ApiException(400, INVALID_SERVER);
    }

    public static String toString(Server server) {
        if (server == null) return "";
        if (server == Server.THE_ARK) {
            return "The_Ark";
        }
        throw new ApiException(400, INVALID_SERVER);
    }
}
