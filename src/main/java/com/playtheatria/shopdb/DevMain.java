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

import java.io.File;
import java.util.logging.Logger;

/**
 * Development entry point: runs the ShopDB web server without a Paper server,
 * for local testing and API parity diffing.
 *
 *   java -cp target/ShopDB-<version>.jar com.playtheatria.shopdb.DevMain [port] [dbFile] [apiUsername] [apiKey]
 */
public final class DevMain {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        File dbFile = new File(args.length > 1 ? args[1] : "shopdb.db");
        String apiUsername = args.length > 2 ? args[2] : "updater";
        String apiKey = args.length > 3 ? args[3] : "";

        Logger logger = Logger.getLogger("ShopDB");
        Db db = new Db(dbFile);

        ShopRepository shops = new ShopRepository(db);
        PlayerRepository players = new PlayerRepository(db);
        RegionRepository regions = new RegionRepository(db);
        UserRepository users = new UserRepository(db);

        ApiUserProvisioner.provision(users, apiUsername, apiKey, logger);

        ApiKeyValidator apiKeyValidator = new ApiKeyValidator(users, apiUsername);
        RegionLogicService regionLogic = new RegionLogicService(regions, players, logger);
        ChestShopIngestService ingest = new ChestShopIngestService(shops, players, regionLogic, logger);

        HttpServerManager server = new HttpServerManager(port, logger,
                new ChestShopsRoute(shops, ingest, apiKeyValidator, logger),
                new PlayersRoute(players, regions, shops, logger),
                new RegionsRoute(regions, players, shops, regionLogic, ingest, apiKeyValidator, logger));
        server.start();
        logger.info("ShopDB dev server on port " + port + " using " + dbFile.getAbsolutePath());
    }

    private DevMain() {
    }
}
