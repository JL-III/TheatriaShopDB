package com.playtheatria.shopdb.updater;

import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * HTTP client for the ShopDB API (ShopDB-Updater's ShopDBAPI): gzip request
 * bodies, raw API key in the Authorization header. Now points at this plugin's
 * own embedded server over localhost.
 */
public class ShopDBClient {
    private final UpdaterConfig config;
    private final Logger logger;

    public ShopDBClient(UpdaterConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public int sendData(String data, String endpoint, String method) throws Exception {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null!");
        }

        if (Bukkit.isPrimaryThread()) {
            throw new IllegalAccessException("This method must not be called from the main thread!");
        }

        if (config.logHttp) {
            logger.info("Sending " + endpoint + " data to ShopDB.");
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(config.apiUri + endpoint).openConnection();

        byte[] compressedData = compress(data);

        connection.setRequestMethod(method);
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Connection", "close");
        connection.addRequestProperty("Content-Encoding", "gzip");
        connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
        connection.addRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "ShopDB");
        connection.setRequestProperty("Authorization", config.apiKey);

        connection.setDoOutput(true);

        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
            outputStream.write(compressedData);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
        }

        if (config.logHttp) {
            logger.info("Sent data to ShopDB and received response [" + connection.getResponseCode() + "]: " + response);
        }

        return connection.getResponseCode();
    }

    private static byte[] compress(final String str) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
        }
        return outputStream.toByteArray();
    }
}
