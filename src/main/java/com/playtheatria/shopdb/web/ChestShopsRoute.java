package com.playtheatria.shopdb.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.playtheatria.shopdb.database.ChestShopRow;
import com.playtheatria.shopdb.database.ShopRepository;
import com.playtheatria.shopdb.models.ChestShopDto;
import com.playtheatria.shopdb.models.ItemType;
import com.playtheatria.shopdb.models.PaginatedResponse;
import com.playtheatria.shopdb.models.Server;
import com.playtheatria.shopdb.models.ShopEvent;
import com.playtheatria.shopdb.models.SortBy;
import com.playtheatria.shopdb.models.TradeType;
import com.playtheatria.shopdb.services.ApiKeyValidator;
import com.playtheatria.shopdb.services.ChestShopIngestService;
import com.playtheatria.shopdb.services.DtoMappers;
import com.playtheatria.shopdb.services.Pagination;
import com.playtheatria.shopdb.services.PriceSnapshotCalculator;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ChestShopsRoute implements RouteUtils.ThrowingHandler {
    private final ShopRepository shops;
    private final ChestShopIngestService ingest;
    private final ApiKeyValidator apiKeyValidator;
    private final Logger logger;

    public ChestShopsRoute(ShopRepository shops, ChestShopIngestService ingest,
                           ApiKeyValidator apiKeyValidator, Logger logger) {
        this.shops = shops;
        this.ingest = ingest;
        this.apiKeyValidator = apiKeyValidator;
        this.logger = logger;
    }

    @Override
    public void handle(HttpExchange exchange) throws Exception {
        String[] seg = RouteUtils.subPath(exchange);
        String method = exchange.getRequestMethod();

        if (seg.length == 0 && "GET".equals(method)) {
            list(exchange);
        } else if (seg.length == 0 && "POST".equals(method)) {
            post(exchange);
        } else if (seg.length == 1 && "material-names".equals(seg[0]) && "GET".equals(method)) {
            materialNames(exchange);
        } else if (seg.length == 1 && "display-names".equals(seg[0]) && "GET".equals(method)) {
            displayNames(exchange);
        } else if (seg.length == 1 && "enchantment-names".equals(seg[0]) && "GET".equals(method)) {
            enchantmentNames(exchange);
        } else if (seg.length == 1 && "enchantment-options".equals(seg[0]) && "GET".equals(method)) {
            enchantmentOptions(exchange);
        } else if (seg.length == 1 && "price-snapshot".equals(seg[0]) && "GET".equals(method)) {
            priceSnapshot(exchange);
        } else {
            throw new ApiException(404, "Resource not found.");
        }
    }

    private void list(HttpExchange exchange) throws Exception {
        logger.info("GET /chest-shops");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        int page = RouteUtils.intParam(p, "page", 1);
        int pageSize = RouteUtils.intParam(p, "pageSize", 6);
        String material = RouteUtils.stringParam(p, "material", "");
        String name = RouteUtils.stringParam(p, "name", "");
        String query = RouteUtils.stringParam(p, "query", "").trim();
        String enchantment = RouteUtils.stringParam(p, "enchantment", "").trim();
        int minEnchantmentLevel = RouteUtils.intParam(p, "minEnchantmentLevel", 0);
        int enchantmentLevel = RouteUtils.intParam(p, "enchantmentLevel", 0);
        ItemType itemType = ItemType.fromString(RouteUtils.stringParam(p, "itemType", "all").trim());
        Server server = Server.fromString(p.get("server"));
        TradeType tradeType = TradeType.fromString(RouteUtils.stringParam(p, "tradeType", "buy"));
        boolean hideUnavailable = RouteUtils.boolParam(p, "hideUnavailable", false);
        SortBy sortBy = SortBy.fromString(RouteUtils.stringParam(p, "sortBy", "best-price"));
        boolean distinct = RouteUtils.boolParam(p, "distinct", false);

        RouteUtils.validatePaging(page, pageSize);
        if (minEnchantmentLevel < 0) {
            throw new ApiException(400, "Minimum enchantment level cannot be negative.");
        }
        if (enchantmentLevel < 0) {
            throw new ApiException(400, "Enchantment level cannot be negative.");
        }
        if (minEnchantmentLevel > 0 && enchantment.isEmpty()) {
            throw new ApiException(400, "An enchantment is required when filtering by minimum level.");
        }
        if (enchantmentLevel > 0 && enchantment.isEmpty()) {
            throw new ApiException(400, "An enchantment is required when filtering by exact level.");
        }
        String serverStr = Server.toString(server);

        if (!distinct) {
            long total = shops.count(material, name, query, enchantment,
                    minEnchantmentLevel, enchantmentLevel,
                    itemType, tradeType, serverStr, hideUnavailable);
            List<ChestShopRow> rows = shops.find(material, name, query, enchantment,
                    minEnchantmentLevel, enchantmentLevel,
                    itemType, tradeType, serverStr, hideUnavailable, sortBy, pageSize, (page - 1) * pageSize);
            List<ChestShopDto> results = rows.stream().map(DtoMappers::toChestShopDto).collect(Collectors.toList());
            RouteUtils.sendJson(exchange, 200, new PaginatedResponse<>(
                    page, Pagination.getNumPages(pageSize, total), total, shuffle(results, tradeType, sortBy)));
            return;
        }

        List<ChestShopRow> all = shops.find(material, name, query, enchantment,
                minEnchantmentLevel, enchantmentLevel,
                itemType, tradeType, serverStr, hideUnavailable, sortBy, null, null);
        List<ChestShopRow> distinctRows = distinctRows(all);
        long total = distinctRows.size();
        List<ChestShopDto> results = Pagination.getPage(distinctRows, page, pageSize)
                .stream().map(DtoMappers::toChestShopDto).collect(Collectors.toList());
        RouteUtils.sendJson(exchange, 200, new PaginatedResponse<>(
                page, Pagination.getNumPages(pageSize, total), total, shuffle(results, tradeType, sortBy)));
    }

    // Mirrors the old first-seen behavior while treating captured item metadata
    // as part of the traded variant's identity.
    static List<ChestShopRow> distinctRows(List<ChestShopRow> rows) {
        LinkedHashMap<DistinctKey, ChestShopRow> distinct = new LinkedHashMap<>();
        for (ChestShopRow row : rows) {
            // Metadata is part of an item's identity: two books with different
            // enchantments must not disappear behind "Unique Shops Only".
            DistinctKey key = new DistinctKey(row.material, row.baseMaterial,
                    canonicalItemDetails(row.itemDetails), row.ownerId, row.townId, row.quantity);
            distinct.putIfAbsent(key, row);
        }
        return new ArrayList<>(distinct.values());
    }

    private static String canonicalItemDetails(String itemDetails) {
        if (itemDetails == null) return null;
        try {
            return canonicalJson(JsonParser.parseString(itemDetails), false);
        } catch (RuntimeException ignored) {
            return "malformed:" + itemDetails;
        }
    }

    private static String canonicalJson(JsonElement element, boolean sortArray) {
        if (element == null || element.isJsonNull() || element.isJsonPrimitive()) {
            return element == null ? "null" : element.toString();
        }
        if (element.isJsonArray()) {
            List<String> values = new ArrayList<>();
            for (JsonElement value : element.getAsJsonArray()) {
                values.add(canonicalJson(value, false));
            }
            if (sortArray) Collections.sort(values);
            return "[" + String.join(",", values) + "]";
        }

        TreeMap<String, String> fields = new TreeMap<>();
        for (Map.Entry<String, JsonElement> field : element.getAsJsonObject().entrySet()) {
            fields.put(field.getKey(), canonicalJson(field.getValue(),
                    "enchants".equals(field.getKey()) || "searchEnchants".equals(field.getKey())));
        }
        StringBuilder result = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (!first) result.append(',');
            result.append(Json.GSON.toJson(field.getKey())).append(':').append(field.getValue());
            first = false;
        }
        return result.append('}').toString();
    }

    private record DistinctKey(String material, String baseMaterial, String itemDetails,
                               Long ownerId, Long townId, Integer quantity) {
    }

    // Verbatim port of the old controller's shuffle: randomizes order within equal
    // prices, then re-sorts by price.
    private List<ChestShopDto> shuffle(List<ChestShopDto> dtos, TradeType tradeType, SortBy sortBy) {
        if (sortBy != SortBy.BEST_PRICE) return dtos;
        List<ChestShopDto> results = new ArrayList<>();

        HashMap<Double, List<ChestShopDto>> priceMap = new HashMap<>();
        for (ChestShopDto dto : dtos) {
            Double price = tradeType == TradeType.BUY ? dto.getBuyPriceEach() : dto.getSellPriceEach();
            priceMap.computeIfAbsent(price, k -> new ArrayList<>()).add(dto);
        }

        for (List<ChestShopDto> samePrices : priceMap.values()) {
            Collections.shuffle(samePrices);
            results.addAll(samePrices);
        }

        Comparator<ChestShopDto> comparator = tradeType == TradeType.BUY
                ? (a, b) -> Double.compare(a.getBuyPriceEach(), b.getBuyPriceEach())
                : (a, b) -> Double.compare(b.getSellPriceEach(), a.getSellPriceEach());
        return results.stream().sorted(comparator).collect(Collectors.toList());
    }

    private void materialNames(HttpExchange exchange) throws Exception {
        logger.info("GET /chest-shops/material-names");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        Server server = Server.fromString(p.get("server"));
        TradeType tradeType = TradeType.fromString(RouteUtils.stringParam(p, "tradeType", "buy"));
        RouteUtils.sendJson(exchange, 200, shops.distinctMaterialNames(tradeType, Server.toString(server)));
    }

    private void displayNames(HttpExchange exchange) throws Exception {
        logger.info("GET /chest-shops/display-names");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        Server server = Server.fromString(p.get("server"));
        TradeType tradeType = TradeType.fromString(RouteUtils.stringParam(p, "tradeType", "buy"));
        RouteUtils.sendJson(exchange, 200, shops.distinctDisplayNames(tradeType, Server.toString(server)));
    }

    private void enchantmentNames(HttpExchange exchange) throws Exception {
        logger.info("GET /chest-shops/enchantment-names");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        Server server = Server.fromString(p.get("server"));
        TradeType tradeType = TradeType.fromString(RouteUtils.stringParam(p, "tradeType", "buy"));
        RouteUtils.sendJson(exchange, 200,
                shops.distinctEnchantmentNames(tradeType, Server.toString(server)));
    }

    private void enchantmentOptions(HttpExchange exchange) throws Exception {
        logger.info("GET /chest-shops/enchantment-options");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        String material = RouteUtils.stringParam(p, "material", "");
        String name = RouteUtils.stringParam(p, "name", "");
        String query = RouteUtils.stringParam(p, "query", "").trim();
        String enchantment = RouteUtils.stringParam(p, "enchantment", "").trim();
        ItemType itemType = ItemType.fromString(RouteUtils.stringParam(p, "itemType", "all").trim());
        Server server = Server.fromString(p.get("server"));
        TradeType tradeType = TradeType.fromString(RouteUtils.stringParam(p, "tradeType", "buy"));
        boolean hideUnavailable = RouteUtils.boolParam(p, "hideUnavailable", false);
        RouteUtils.sendJson(exchange, 200,
                shops.distinctEnchantmentOptions(material, name, query, enchantment,
                        itemType, tradeType, Server.toString(server), hideUnavailable));
    }

    private void priceSnapshot(HttpExchange exchange) throws Exception {
        logger.info("GET /chest-shops/price-snapshot");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        Server server = Server.fromString(p.get("server"));
        RouteUtils.sendJson(exchange, 200,
                PriceSnapshotCalculator.calculate(shops.findVisible(Server.toString(server))));
    }

    private void post(HttpExchange exchange) throws Exception {
        apiKeyValidator.validateAPIKey(exchange.getRequestHeaders().getFirst("Authorization"));
        String body = RouteUtils.readBody(exchange);
        List<ShopEvent> events = Json.GSON.fromJson(body, new TypeToken<List<ShopEvent>>() {
        }.getType());
        RouteUtils.sendRawString(exchange, 200, ingest.createChestShopSigns(events));
    }
}
