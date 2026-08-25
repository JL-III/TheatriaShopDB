package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.ChestShopRow;
import com.playtheatria.shopdb.database.PlayerRepository;
import com.playtheatria.shopdb.database.RegionRow;
import com.playtheatria.shopdb.database.ShopRepository;
import com.playtheatria.shopdb.models.EventType;
import com.playtheatria.shopdb.models.RegionRequest;
import com.playtheatria.shopdb.models.Server;
import com.playtheatria.shopdb.models.ShopEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/** Port of the previous backend's ChestShopService.createChestShopSigns, same semantics and response text. */
public class ChestShopIngestService {
    private static final String RESPONSE =
            "Successfully inserted/updated %s player(s), %s region(s), and %s chest shop(s). Removed %s chest shop(s).";
    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

    private final ShopRepository shops;
    private final PlayerRepository players;
    private final RegionLogicService regionLogic;
    private final Logger logger;

    public ChestShopIngestService(ShopRepository shops, PlayerRepository players,
                                  RegionLogicService regionLogic, Logger logger) {
        this.shops = shops;
        this.players = players;
        this.regionLogic = regionLogic;
        this.logger = logger;
    }

    public String createChestShopSigns(List<ShopEvent> shopEvents) throws SQLException {
        List<String> shopIdsToDelete = new ArrayList<>();
        List<ShopEvent> upserts = new ArrayList<>();
        Set<String> playerNames = new HashSet<>();

        logger.info("Sorting shop events...");
        for (ShopEvent shopEvent : shopEvents) {
            if (EventType.DELETE.equals(shopEvent.getEventType())) {
                shopIdsToDelete.add(shopEvent.getId());
            } else {
                upserts.add(shopEvent);
                playerNames.add(shopEvent.getOwner().toLowerCase(Locale.ROOT));
                if (shopEvent.getRegions() != null) {
                    for (RegionRequest regionRequest : shopEvent.getRegions()) {
                        playerNames.addAll(regionRequest.getMayorNames());
                    }
                }
            }
        }

        logger.info("Found " + shopIdsToDelete.size() + " shop deletion events.");
        logger.info("Found " + upserts.size() + " shops to create or modify.");

        for (String id : shopIdsToDelete) {
            shops.deleteById(id);
        }

        if (upserts.isEmpty()) {
            String response = String.format(RESPONSE, 0, 0, 0, shopIdsToDelete.size());
            logger.info(response);
            return response;
        }

        logger.info("Retrieving/adding " + playerNames.size() + " players...");
        HashMap<String, PlayerRepository.PlayerRow> knownPlayers = players.getOrAdd(playerNames);

        Set<RegionRequest> regionRequests = new HashSet<>();
        for (ShopEvent e : upserts) {
            if (e.getRegions() != null) regionRequests.addAll(e.getRegions());
        }
        logger.info("Inserting/updating " + regionRequests.size() + " regions...");
        HashMap<String, RegionRow> regions = regionLogic.upsertRegions(regionRequests, knownPlayers);

        logger.info("Mapping " + shopEvents.size() + " events to chest shops...");
        int persisted = 0;
        for (ShopEvent upsert : upserts) {
            if (!eventIsValid(upsert)) continue;
            shops.upsert(convert(upsert, knownPlayers, regions));
            persisted++;
        }
        logger.info("Added " + persisted + " chest shops.");

        String response = String.format(RESPONSE,
                knownPlayers.keySet().size(), regions.keySet().size(), upserts.size(), shopIdsToDelete.size());
        logger.info(response);
        return response;
    }

    public void linkAndShowChestShops(RegionRow region) throws SQLException {
        for (ChestShopRow shop : shopsIn(region)) {
            shops.setTownAndHidden(shop.id, region.id, false);
        }
    }

    public void linkAndHideChestShops(RegionRow region) throws SQLException {
        for (ChestShopRow shop : shopsIn(region)) {
            shops.setTownAndHidden(shop.id, region.id, true);
        }
    }

    private List<ChestShopRow> shopsIn(RegionRow region) throws SQLException {
        return shops.findInBounds(region.server, region.iX, region.oX, region.iY, region.oY, region.iZ, region.oZ);
    }

    private ChestShopRow convert(ShopEvent event,
                                 HashMap<String, PlayerRepository.PlayerRow> knownPlayers,
                                 HashMap<String, RegionRow> regions) {
        ChestShopRow sign = new ChestShopRow();
        sign.id = event.getId();

        if ("The_Ark".equals(event.getWorld())) {
            sign.server = Server.THE_ARK.name();
        }
        sign.x = event.getX();
        sign.y = event.getY();
        sign.z = event.getZ();

        PlayerRepository.PlayerRow owner = knownPlayers.get(event.getOwner().toLowerCase(Locale.ROOT));
        sign.ownerId = owner == null ? null : owner.id;
        sign.quantity = event.getQuantity();
        sign.quantityAvailable = event.getCount();

        if (event.getSellPrice() != null && event.getSellPrice().doubleValue() != -1.0) {
            sign.sellPrice = event.getSellPrice().doubleValue();
            sign.sellPriceEach = sign.quantity == null ? null : sign.sellPrice / sign.quantity;
            sign.isSellSign = Boolean.TRUE;
        } else {
            sign.sellPrice = null;
            sign.sellPriceEach = null;
            sign.isSellSign = Boolean.FALSE;
        }

        if (event.getBuyPrice() != null && event.getBuyPrice().doubleValue() != -1.0) {
            sign.buyPrice = event.getBuyPrice().doubleValue();
            sign.buyPriceEach = sign.quantity == null ? null : sign.buyPrice / sign.quantity;
            sign.isBuySign = Boolean.TRUE;
        } else {
            sign.buyPrice = null;
            sign.buyPriceEach = null;
            sign.isBuySign = Boolean.FALSE;
        }

        List<RegionRow> shopRegions = new ArrayList<>();
        if (event.getRegions() != null) {
            for (RegionRequest regionReq : event.getRegions()) {
                RegionRow r = regions.get(regionReq.getName() + "|" + regionReq.getServer());
                if (r != null) shopRegions.add(r);
            }
        }

        RegionRow town = regionLogic.findActiveOrSmallest(shopRegions);
        sign.townId = town == null ? null : town.id;
        sign.material = event.getItem().toLowerCase(Locale.ROOT);
        sign.isHidden = town == null || !Boolean.TRUE.equals(town.active);
        sign.isFull = event.getFull();
        sign.isSellSign = sign.sellPrice != null;
        sign.baseMaterial = event.getBaseMaterial() == null
                ? null : event.getBaseMaterial().toLowerCase(Locale.ROOT);
        sign.itemDetails = event.getItemDetails() == null || event.getItemDetails().isEmpty()
                ? null : GSON.toJson(event.getItemDetails());
        return sign;
    }

    private boolean eventIsValid(ShopEvent event) {
        if (event.getId() == null || event.getId().isEmpty()) {
            logger.info("Skipping event " + event + " - ID is null or empty.");
            return false;
        }

        if (event.getEventType() == null) {
            logger.info("Skipping event " + event + " - event type not specified.");
            return false;
        }

        if (event.getWorld() == null || event.getWorld().isEmpty()) {
            logger.info("Skipping event " + event + " - no world specified.");
            return false;
        }

        if (!event.getWorld().equals("The_Ark")) {
            logger.info("Skipping event " + event + " - server cannot be determined.");
            return false;
        }

        if (event.getX() == null || event.getY() == null || event.getZ() == null) {
            logger.info("Skipping event " + event + " - X, Y, or Z coordinate is missing.");
            return false;
        }

        if (event.getOwner() == null || event.getOwner().isEmpty()) {
            logger.info("Skipping event " + event + " - owner is missing");
            return false;
        }

        if (event.getQuantity() == null || event.getQuantity() == 0) {
            logger.info("Skipping event " + event + " - shop quantity is missing");
            return false;
        }

        if (event.getCount() == null) {
            logger.info("Skipping event " + event + " - count is missing");
            return false;
        }

        if (event.getItem() == null || event.getItem().isEmpty()) {
            logger.info("Skipping event " + event + " - item is missing");
            return false;
        }

        if (event.getFull() == null) {
            logger.info("Skipping event " + event + " - 'full' indicator is missing");
            return false;
        }

        return true;
    }
}
