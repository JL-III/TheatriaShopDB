package com.playtheatria.shopdb.updater;

/** Settings for the in-plugin updater (ported from ShopDB-Updater's Properties). */
public class UpdaterConfig {
    public final boolean enabled;
    public final int intervalMinutes;
    public final int cacheSize;
    public final String apiUri;   // e.g. http://127.0.0.1:8080/api/v3/
    public final String apiKey;
    public final boolean logHttp;

    public UpdaterConfig(boolean enabled, int intervalMinutes, int cacheSize, String apiUri, String apiKey, boolean logHttp) {
        this.enabled = enabled;
        this.intervalMinutes = intervalMinutes;
        this.cacheSize = cacheSize;
        this.apiUri = apiUri;
        this.apiKey = apiKey;
        this.logHttp = logHttp;
    }
}
