package com.playtheatria.shopdb.web;

import com.playtheatria.shopdb.models.ErrorResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RouteUtils {
    public static final String INVALID_PAGE = "Page cannot be less than 1.";
    public static final String INVALID_PAGE_SIZE = "Page size must be between 1 and 100.";

    @FunctionalInterface
    public interface ThrowingHandler {
        void handle(HttpExchange exchange) throws Exception;
    }

    /** Wraps a handler with CORS, OPTIONS preflight, and ErrorResponse rendering. */
    public static HttpHandler wrap(Logger logger, ThrowingHandler handler) {
        return exchange -> {
            cors(exchange);
            try {
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                    return;
                }
                handler.handle(exchange);
            } catch (ApiException e) {
                sendError(exchange, e.status, reasonPhrase(e.status), e.getMessage());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Request failed: " + exchange.getRequestURI(), e);
                sendError(exchange, 500, "Internal Server Error", e.getMessage());
            } finally {
                exchange.close();
            }
        };
    }

    private static void cors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "*");
    }

    static String reasonPhrase(int status) {
        switch (status) {
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            default: return "Internal Server Error";
        }
    }

    public static void sendError(HttpExchange exchange, int status, String reason, String message) throws IOException {
        if (message == null) message = "Unknown exception occurred.";
        String body = Json.GSON.toJson(new ErrorResponse(System.currentTimeMillis(), status, reason, message));
        sendJsonString(exchange, status, body);
    }

    public static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        sendJsonString(exchange, status, Json.GSON.toJson(body));
    }

    /** The previous backend returned plain strings from write endpoints, unquoted, as application/json. */
    public static void sendRawString(HttpExchange exchange, int status, String body) throws IOException {
        sendJsonString(exchange, status, body);
    }

    private static void sendJsonString(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    public static void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
    }

    public static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isEmpty()) return result;
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            result.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return result;
    }

    public static int intParam(Map<String, String> params, String key, int defaultValue) {
        String value = params.get(key);
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid value for parameter '" + key + "'.");
        }
    }

    public static boolean boolParam(Map<String, String> params, String key, boolean defaultValue) {
        String value = params.get(key);
        if (value == null || value.isEmpty()) return defaultValue;
        return Boolean.parseBoolean(value);
    }

    public static String stringParam(Map<String, String> params, String key, String defaultValue) {
        String value = params.get(key);
        return value == null ? defaultValue : value;
    }

    /** Path segments after the context path, URL-decoded. Empty array for the context root. */
    public static String[] subPath(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String context = exchange.getHttpContext().getPath();
        String rest = path.length() > context.length() ? path.substring(context.length()) : "";
        while (rest.startsWith("/")) rest = rest.substring(1);
        while (rest.endsWith("/")) rest = rest.substring(0, rest.length() - 1);
        if (rest.isEmpty()) return new String[0];
        String[] segments = rest.split("/");
        for (int i = 0; i < segments.length; i++) {
            segments[i] = URLDecoder.decode(segments[i], StandardCharsets.UTF_8);
        }
        return segments;
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static void validatePaging(int page, int pageSize) {
        if (page < 1) throw new ApiException(400, INVALID_PAGE);
        if (pageSize > 100 || pageSize < 1) throw new ApiException(400, INVALID_PAGE_SIZE);
    }

    private RouteUtils() {
    }
}
