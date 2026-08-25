package com.playtheatria.shopdb.web;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Starts the embedded HTTP server: the website at "/" and the API under
 * "/api/v3" — same one-process model as MC-Ledger, one context per route family.
 */
public class HttpServerManager {
    private final HttpServer server;
    private final ExecutorService executor;

    public HttpServerManager(int port, Logger logger,
                             ChestShopsRoute chestShops,
                             PlayersRoute players,
                             RegionsRoute regions) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.executor = Executors.newCachedThreadPool();

        server.createContext("/", new StaticFiles(logger));
        server.createContext("/api/v3/chest-shops", RouteUtils.wrap(logger, chestShops));
        server.createContext("/api/v3/players", RouteUtils.wrap(logger, players));
        server.createContext("/api/v3/regions", RouteUtils.wrap(logger, regions));
        server.setExecutor(executor);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
        executor.shutdownNow();
    }
}
