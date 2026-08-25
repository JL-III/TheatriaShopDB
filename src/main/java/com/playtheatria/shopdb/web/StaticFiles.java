package com.playtheatria.shopdb.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Serves the frontend bundle from the classpath resource directory "frontend"
 * (the MC-Ledger pattern). Assets are loaded into memory once at startup.
 * Unknown paths fall back to index.html so the SPA's client-side routes
 * (e.g. /players/name) work on refresh.
 */
public class StaticFiles implements HttpHandler {
    private final Map<String, byte[]> assets = new HashMap<>(); // keyed by "/path/under/frontend"
    private final byte[] index;

    public StaticFiles(Logger logger) {
        byte[] indexBytes = null;
        try {
            URL url = StaticFiles.class.getClassLoader().getResource("frontend");
            if (url == null) {
                logger.warning("No 'frontend' resource found in the jar - serving the API only. " +
                        "Run 'make build' to bundle the website.");
            } else {
                loadAssets(url);
                indexBytes = assets.get("/index.html");
                logger.info("Loaded " + assets.size() + " frontend assets.");
            }
        } catch (Exception e) {
            logger.warning("Failed to load frontend assets: " + e);
        }
        this.index = indexBytes;
    }

    private void loadAssets(URL url) throws Exception {
        URI uri = url.toURI();
        if ("jar".equals(uri.getScheme())) {
            try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                walk(fs.getPath("frontend"));
            }
        } else {
            walk(Paths.get(uri));
        }
    }

    private void walk(Path base) throws IOException {
        try (Stream<Path> stream = Files.walk(base)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                if (Files.isDirectory(p)) continue;
                String key = "/" + base.relativize(p).toString().replace('\\', '/');
                assets.put(key, Files.readAllBytes(p));
            }
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        byte[] body = "/".equals(path) ? index : assets.get(path);
        if (body == null) body = index; // SPA fallback for client-side routes
        if (body == null) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        String servedPath = assets.containsKey(path) ? path : "/index.html";
        exchange.getResponseHeaders().set("Content-Type", contentType(servedPath));
        exchange.sendResponseHeaders(200, body.length);
        try (InputStream ignored = exchange.getRequestBody()) {
            exchange.getResponseBody().write(body);
        }
        exchange.close();
    }

    static String contentType(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js")) return "application/javascript";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".json") || p.endsWith(".map")) return "application/json";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".ico")) return "image/x-icon";
        if (p.endsWith(".txt")) return "text/plain";
        if (p.endsWith(".woff")) return "font/woff";
        if (p.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }
}
