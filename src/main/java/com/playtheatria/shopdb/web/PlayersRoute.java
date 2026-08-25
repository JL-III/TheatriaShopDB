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
import com.playtheatria.shopdb.models.SortBy;
import com.playtheatria.shopdb.models.TradeType;
import com.playtheatria.shopdb.services.DtoMappers;
import com.playtheatria.shopdb.services.Pagination;
import com.sun.net.httpserver.HttpExchange;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PlayersRoute implements RouteUtils.ThrowingHandler {
    public static final String EMPTY_PLAYER_NAME = "Player name cannot be null or blank.";
    public static final String PLAYER_NOT_FOUND = "Player with name '%s' was not found.";

    private final PlayerRepository players;
    private final RegionRepository regions;
    private final ShopRepository shops;
    private final Logger logger;

    public PlayersRoute(PlayerRepository players, RegionRepository regions, ShopRepository shops, Logger logger) {
        this.players = players;
        this.regions = regions;
        this.shops = shops;
        this.logger = logger;
    }

    @Override
    public void handle(HttpExchange exchange) throws Exception {
        String[] seg = RouteUtils.subPath(exchange);
        if (!"GET".equals(exchange.getRequestMethod())) {
            throw new ApiException(405, "Method not allowed.");
        }

        if (seg.length == 0) {
            list(exchange);
        } else if (seg.length == 1 && "player-names".equals(seg[0])) {
            RouteUtils.sendJson(exchange, 200, players.names());
        } else if (seg.length == 1) {
            player(exchange, seg[0]);
        } else if (seg.length == 2 && "regions".equals(seg[1])) {
            playerRegions(exchange, seg[0]);
        } else if (seg.length == 2 && "chest-shops".equals(seg[1])) {
            playerChestShops(exchange, seg[0]);
        } else {
            throw new ApiException(404, "Resource not found.");
        }
    }

    private void list(HttpExchange exchange) throws Exception {
        logger.info("GET /players");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        int page = RouteUtils.intParam(p, "page", 1);
        int pageSize = RouteUtils.intParam(p, "pageSize", 6);
        String name = RouteUtils.stringParam(p, "name", "");
        SortBy sortBy = SortBy.fromString(RouteUtils.stringParam(p, "sortBy", "name"));

        RouteUtils.validatePaging(page, pageSize);

        long total = players.count(name);
        List<PlayerDto> results = new ArrayList<>();
        for (PlayerRepository.PlayerRow row : players.page(name, sortBy, pageSize, (page - 1) * pageSize)) {
            results.add(toPlayerDto(row));
        }
        RouteUtils.sendJson(exchange, 200,
                new PaginatedResponse<>(page, Pagination.getNumPages(pageSize, total), total, results));
    }

    private void player(HttpExchange exchange, String name) throws Exception {
        logger.info("GET /players/" + name);
        PlayerRepository.PlayerRow row = players.findByName(name);
        if (row == null) {
            // The old JAX-RS resource returned null, which rendered as 204 No Content.
            RouteUtils.sendNoContent(exchange);
            return;
        }
        RouteUtils.sendJson(exchange, 200, toPlayerDto(row));
    }

    private void playerRegions(HttpExchange exchange, String name) throws Exception {
        logger.info("GET /players/" + name + "/regions");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        int page = RouteUtils.intParam(p, "page", 1);
        int pageSize = RouteUtils.intParam(p, "pageSize", 6);

        RouteUtils.validatePaging(page, pageSize);
        if (name == null || name.isEmpty()) throw new ApiException(400, EMPTY_PLAYER_NAME);

        PlayerRepository.PlayerRow player = players.findByName(name);
        if (player == null) throw new ApiException(404, String.format(PLAYER_NOT_FOUND, name));

        List<RegionRow> towns = players.townsOf(player.id);
        int total = towns.size();
        List<RegionDto> results = new ArrayList<>();
        for (RegionRow town : Pagination.getPage(towns, page, pageSize)) {
            results.add(DtoMappers.toRegionDto(town, regions.numChestShops(town.id), regions.mayorNamesOf(town.id)));
        }
        RouteUtils.sendJson(exchange, 200,
                new PaginatedResponse<>(page, Pagination.getNumPages(pageSize, total), total, results));
    }

    private void playerChestShops(HttpExchange exchange, String name) throws Exception {
        logger.info("GET /players/" + name + "/chest-shops");
        Map<String, String> p = RouteUtils.queryParams(exchange);
        int page = RouteUtils.intParam(p, "page", 1);
        int pageSize = RouteUtils.intParam(p, "pageSize", 6);
        TradeType tradeType = TradeType.fromString(RouteUtils.stringParam(p, "tradeType", "buy"));

        RouteUtils.validatePaging(page, pageSize);
        if (name == null || name.isEmpty()) throw new ApiException(400, EMPTY_PLAYER_NAME);

        PlayerRepository.PlayerRow player = players.findByName(name);
        if (player == null) throw new ApiException(404, String.format(PLAYER_NOT_FOUND, name));

        long total = shops.countOwnedBy(player.id, tradeType);
        List<ChestShopDto> results = new ArrayList<>();
        for (ChestShopRow row : shops.findOwnedBy(player.id, tradeType, pageSize, (page - 1) * pageSize)) {
            results.add(DtoMappers.toChestShopDto(row));
        }
        RouteUtils.sendJson(exchange, 200,
                new PaginatedResponse<>(page, Pagination.getNumPages(pageSize, total), total, results));
    }

    private PlayerDto toPlayerDto(PlayerRepository.PlayerRow row) throws SQLException {
        return DtoMappers.toPlayerDto(row, players.numChestShops(row.id), players.townsOf(row.id));
    }
}
