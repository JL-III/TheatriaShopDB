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
import com.playtheatria.shopdb.web.ChestShopsRoute;
import com.playtheatria.shopdb.web.HttpServerManager;
import com.playtheatria.shopdb.web.PlayersRoute;
import com.playtheatria.shopdb.web.RegionsRoute;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ShopDBPlugin extends JavaPlugin {
    private Db db;
    private HttpServerManager httpServer;

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
        } catch (Exception e) {
            getLogger().severe("Failed to start ShopDB: " + e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
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
