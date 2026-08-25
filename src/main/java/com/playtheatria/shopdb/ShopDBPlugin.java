package com.playtheatria.shopdb;

import com.playtheatria.shopdb.database.Db;
import com.playtheatria.shopdb.database.PlayerRepository;
import com.playtheatria.shopdb.database.RegionRepository;
import com.playtheatria.shopdb.database.ShopRepository;
import com.playtheatria.shopdb.database.UserRepository;
import com.playtheatria.shopdb.services.ApiKeyValidator;
import com.playtheatria.shopdb.services.ApiUserProvisioner;
import com.playtheatria.shopdb.services.ChestShopIngestService;
import com.playtheatria.shopdb.services.RegionLogicService;
import com.playtheatria.shopdb.updater.EventBuffer;
import com.playtheatria.shopdb.updater.ShopDBClient;
import com.playtheatria.shopdb.updater.ShopDBCommands;
import com.playtheatria.shopdb.updater.ShopDBEditCommands;
import com.playtheatria.shopdb.updater.ShopEventsListener;
import com.playtheatria.shopdb.updater.ShopUpdater;
import com.playtheatria.shopdb.updater.UpdaterConfig;
import com.playtheatria.shopdb.web.ChestShopsRoute;
import com.playtheatria.shopdb.web.HttpServerManager;
import com.playtheatria.shopdb.web.PlayersRoute;
import com.playtheatria.shopdb.web.RegionsRoute;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ShopDBPlugin extends JavaPlugin {
    private Db db;
    private HttpServerManager httpServer;
    private EventBuffer eventBuffer;
    private ShopUpdater shopUpdater;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        int port = getConfig().getInt("port", 8080);
        String apiUsername = getConfig().getString("api-username", "updater");
        String apiKey = getConfig().getString("api-key", "");
        String databaseFile = getConfig().getString("database-file", "shopdb.db");

        try {
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                throw new IllegalStateException("Could not create plugin data folder.");
            }
            db = new Db(new File(getDataFolder(), databaseFile));

            ShopRepository shops = new ShopRepository(db);
            PlayerRepository players = new PlayerRepository(db);
            RegionRepository regions = new RegionRepository(db);
            UserRepository users = new UserRepository(db);

            ApiUserProvisioner.provision(users, apiUsername, apiKey, getLogger());

            ApiKeyValidator apiKeyValidator = new ApiKeyValidator(users, apiUsername);
            RegionLogicService regionLogic = new RegionLogicService(regions, players, getLogger());
            ChestShopIngestService ingest = new ChestShopIngestService(shops, players, regionLogic, getLogger());

            httpServer = new HttpServerManager(port, getLogger(),
                    new ChestShopsRoute(shops, ingest, apiKeyValidator, getLogger()),
                    new PlayersRoute(players, regions, shops, getLogger()),
                    new RegionsRoute(regions, players, shops, regionLogic, ingest, apiKeyValidator, getLogger()));
            httpServer.start();
            getLogger().info("ShopDB listening on port " + port + " (website at /, API at /api/v3).");

            if (getConfig().getBoolean("updater.enabled", true)) {
                startUpdater(port, apiKey);
            }
        } catch (Exception e) {
            getLogger().severe("Failed to start ShopDB: " + e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void startUpdater(int port, String apiKey) {
        if (getServer().getPluginManager().getPlugin("ChestShop") == null
                || getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().warning("ChestShop and/or WorldGuard not found - shop event updater disabled. " +
                    "The website and API keep running.");
            return;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            getLogger().warning("updater.enabled is true but api-key is empty - the updater cannot " +
                    "authenticate against the API. Set api-key in config.yml.");
        }

        try {
            UpdaterConfig config = new UpdaterConfig(true,
                    getConfig().getInt("updater.interval-minutes", 10),
                    getConfig().getInt("updater.cache-size", 1000),
                    "http://127.0.0.1:" + port + "/api/v3/",
                    apiKey,
                    getConfig().getBoolean("updater.log-http", true));

            eventBuffer = new EventBuffer(new File(getDataFolder(), "shop_events.db"), config.cacheSize, getLogger());
            ShopDBClient client = new ShopDBClient(config, getLogger());

            getServer().getPluginManager().registerEvents(new ShopEventsListener(eventBuffer), this);
            getCommand("shopdb").setExecutor(new ShopDBCommands(client));
            getCommand("shopdbedit").setExecutor(new ShopDBEditCommands(eventBuffer));

            shopUpdater = new ShopUpdater(this, eventBuffer, client, config, getLogger());
            shopUpdater.startSubmitting();
            getLogger().info("Shop event updater started (posting every " + config.intervalMinutes + " minute(s)).");
        } catch (Exception e) {
            getLogger().severe("Failed to start shop event updater: " + e);
        }
    }

    @Override
    public void onDisable() {
        // Flush buffered shop events while our own HTTP server is still up.
        if (shopUpdater != null) {
            shopUpdater.flushNow();
            shopUpdater.stop();
            shopUpdater = null;
        }
        if (eventBuffer != null) {
            eventBuffer.close();
            eventBuffer = null;
        }
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }
        if (db != null) {
            try {
                db.close();
            } catch (Exception e) {
                getLogger().warning("Error closing database: " + e);
            }
            db = null;
        }
    }
}
