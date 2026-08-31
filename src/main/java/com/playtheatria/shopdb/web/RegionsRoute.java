package com.playtheatria.shopdb.web;

import com.playtheatria.shopdb.database.ChestShopRow;
import com.playtheatria.shopdb.database.PlayerRepository;
import com.playtheatria.shopdb.database.RegionRepository;
import com.playtheatria.shopdb.database.RegionRow;
import com.playtheatria.shopdb.database.ShopRepository;
import com.playtheatria.shopdb.models.ChestShopDto;
import com.playtheatria.shopdb.models.PaginatedResponse;
import com.playtheatria.shopdb.models.PlayerDto;
import com.playtheatria.shopdb.models.RegionDto;
import com.playtheatria.shopdb.models.RegionRequest;
import com.playtheatria.shopdb.models.Server;
import com.playtheatria.shopdb.models.SortBy;
import com.playtheatria.shopdb.models.ShopLocationType;
import com.playtheatria.shopdb.models.TradeType;
import com.playtheatria.shopdb.services.ApiKeyValidator;
import com.playtheatria.shopdb.services.ChestShopIngestService;
import com.playtheatria.shopdb.services.DtoMappers;
import com.playtheatria.shopdb.services.Pagination;
import com.playtheatria.shopdb.services.RegionLogicService;
import com.sun.net.httpserver.HttpExchange;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RegionsRoute implements RouteUtils.ThrowingHandler {
    public static final String REGION_NOT_FOUND = "Region '%s' on server '%s' was not found.";

    private final RegionRepository regions;
    private final PlayerRepository players;
    private final ShopRepository shops;
    private final RegionLogicService regionLogic;
    private final ChestShopIngestService ingest;
    private final ApiKeyValidator apiKeyValidator;
    private final Logger logger;

    public RegionsRoute(RegionRepository regions, PlayerRepository players, ShopRepository shops,
                        RegionLogicService regionLogic, ChestShopIngestService ingest,
                        ApiKeyValidator apiKeyValidator, Logger logger) {
        this.regions = regions;
        this.players = players;
        this.shops = shops;
        this.regionLogic = regionLogic;
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
        } else if (seg.length == 0 && "PUT".equals(method)) {
            addRegion(exchange);
        } else if (seg.length == 0 && "DELETE".equals(method)) {
            removeRegion(exchange);
        } else if (seg.length == 1 && "region-names".equals(seg[0]) && "GET".equals(method)) {
            regionNames(exchange);
        } else if (seg.length == 2 && "GET".equals(method)) {
            region(exchange, seg[0], seg[1]);
        } else if (seg.length == 3 && "players".equals(seg[2]) && "GET".equals(method)) {
            regionPlayers(exchange, seg[0], seg[1]);
        } else if (seg.length == 3 && "chest-shops".equals(seg[2]) && "GET".equals(method)) {
            regionChestShops(exchange, seg[0], seg[1]);
        } else {
            throw new ApiException(404, "Resource not found.");
        }
    }

    private void list(HttpExchange exchange) throws Exception {
        logger.info("GET /regions");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        int page = RouteUtils.intParam(p, "page", 1);
        int pageSize = RouteUtils.intParam(p, "pageSize", 6);
        Server server = Server.fromString(p.get("server"));
        String name = RouteUtils.stringParam(p, "name", "");
        SortBy sortBy = SortBy.fromString(RouteUtils.stringParam(p, "sortBy", "name"));
        ShopLocationType type = locationType(p);

        RouteUtils.validatePaging(page, pageSize);
        String serverStr = Server.toString(server);

        long total = regions.count(serverStr, name, type);
        List<RegionDto> results = new ArrayList<>();
        for (RegionRow row : regions.page(serverStr, name, type, sortBy, pageSize, (page - 1) * pageSize)) {
            results.add(toRegionDto(row));
        }
        RouteUtils.sendJson(exchange, 200,
                new PaginatedResponse<>(page, Pagination.getNumPages(pageSize, total), total, results));
    }

    private void regionNames(HttpExchange exchange) throws Exception {
        logger.info("GET /region-names");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        Server server = Server.fromString(p.get("server"));
        RouteUtils.sendJson(exchange, 200, regions.names(Server.toString(server), locationType(p)));
    }

    private RegionRow findOr404(String serverSeg, String nameSeg, ShopLocationType type) throws SQLException {
        Server server = Server.fromString(serverSeg);
        RegionRow region = server == null ? null
                : regions.findByServerEnumTypeAndName(server.name(), type, nameSeg);
        if (region == null || !Boolean.TRUE.equals(region.active)) {
            // The old handler formatted the enum, so the message shows 'THE_ARK'.
            throw new ApiException(404, String.format(REGION_NOT_FOUND, nameSeg,
                    server == null ? serverSeg : server.name()));
        }
        return region;
    }

    private void region(HttpExchange exchange, String serverSeg, String nameSeg) throws Exception {
        logger.info("GET /regions/" + serverSeg + "/" + nameSeg);
        RouteUtils.sendJson(exchange, 200,
                toRegionDto(findOr404(serverSeg, nameSeg, locationType(RouteUtils.queryParams(exchange)))));
    }

    private void regionPlayers(HttpExchange exchange, String serverSeg, String nameSeg) throws Exception {
        logger.info("GET /regions/" + serverSeg + "/" + nameSeg + "/players");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        int page = RouteUtils.intParam(p, "page", 1);
        int pageSize = RouteUtils.intParam(p, "pageSize", 6);
        RouteUtils.validatePaging(page, pageSize);

        RegionRow region = findOr404(serverSeg, nameSeg, locationType(p));

        // Mirrors the old handler exactly: totalElements is the size of the current
        // page, not the full mayor list.
        List<PlayerRepository.PlayerRow> pageRows =
                Pagination.getPage(regions.mayorRowsOf(region.id), page, pageSize);
        int total = pageRows.size();
        List<PlayerDto> results = new ArrayList<>();
        for (PlayerRepository.PlayerRow row : pageRows) {
            results.add(DtoMappers.toPlayerDto(row, players.numChestShops(row.id), players.townsOf(row.id)));
        }
        RouteUtils.sendJson(exchange, 200,
                new PaginatedResponse<>(page, Pagination.getNumPages(pageSize, total), total, results));
    }

    private void regionChestShops(HttpExchange exchange, String serverSeg, String nameSeg) throws Exception {
        logger.info("GET /regions/" + serverSeg + "/" + nameSeg + "/chest-shops");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        int page = RouteUtils.intParam(p, "page", 1);
        int pageSize = RouteUtils.intParam(p, "pageSize", 6);
        TradeType tradeType = TradeType.fromString(RouteUtils.stringParam(p, "tradeType", "buy"));
        RouteUtils.validatePaging(page, pageSize);

        RegionRow region = findOr404(serverSeg, nameSeg, locationType(p));

        long total = shops.countInRegion(region.id, tradeType);
        List<ChestShopDto> results = new ArrayList<>();
        for (ChestShopRow row : shops.findInRegion(region.id, tradeType, pageSize, (page - 1) * pageSize)) {
            results.add(DtoMappers.toChestShopDto(row));
        }
        RouteUtils.sendJson(exchange, 200,
                new PaginatedResponse<>(page, Pagination.getNumPages(pageSize, total), total, results));
    }

    private void addRegion(HttpExchange exchange) throws Exception {
        apiKeyValidator.validateAPIKey(exchange.getRequestHeaders().getFirst("Authorization"));
        RegionRequest request = Json.GSON.fromJson(RouteUtils.readBody(exchange), RegionRequest.class);
        RegionRow region = regionLogic.listRegion(request);
        ingest.linkAndShowChestShops(region);
        RouteUtils.sendRawString(exchange, 200, "Successfully listed region " + request.getName());
    }

    private void removeRegion(HttpExchange exchange) throws Exception {
        apiKeyValidator.validateAPIKey(exchange.getRequestHeaders().getFirst("Authorization"));
        RegionRequest request = Json.GSON.fromJson(RouteUtils.readBody(exchange), RegionRequest.class);
        RegionRow region = regionLogic.unlistRegion(request);
        ingest.linkAndHideChestShops(region);
        RouteUtils.sendRawString(exchange, 200, "Successfully unlisted region " + request.getName());
    }

    private RegionDto toRegionDto(RegionRow row) throws SQLException {
        return DtoMappers.toRegionDto(row, regions.numChestShops(row.id), regions.mayorNamesOf(row.id));
    }

    private ShopLocationType locationType(Map<String, String> params) {
        String raw = params.get("type");
        if (raw == null || raw.isBlank()) return ShopLocationType.MARKET_STALL;
        ShopLocationType type = ShopLocationType.fromString(raw);
        if (type == null) throw new ApiException(400, "Invalid shop location type: " + raw);
        return type;
    }
}
