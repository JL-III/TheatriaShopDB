package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.PlayerRepository;
import com.playtheatria.shopdb.database.RegionRepository;
import com.playtheatria.shopdb.database.RegionRow;
import com.playtheatria.shopdb.models.Location;
import com.playtheatria.shopdb.models.RegionRequest;
import com.playtheatria.shopdb.models.Server;
import com.playtheatria.shopdb.models.ShopLocationType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/** Port of the previous backend's RegionService, byte-for-byte semantics. */
public class RegionLogicService {
    private static final Pattern MAYOR_NAME = Pattern.compile("[a-zA-Z0-9+_]{3,16}");

    private final RegionRepository regions;
    private final PlayerRepository players;
    private final Logger logger;
    private final HashMap<String, Server> servers = new HashMap<>();

    public RegionLogicService(RegionRepository regions, PlayerRepository players, Logger logger) {
        this.regions = regions;
        this.players = players;
        this.logger = logger;
        this.servers.put("The_Ark", Server.THE_ARK);
    }

    public RegionRow listRegion(RegionRequest request) throws SQLException {
        RegionRow region = convertAndPersist(request, Boolean.TRUE, null);
        logger.info("Successfully listed region " + request.getName());
        return region;
    }

    public RegionRow unlistRegion(RegionRequest request) throws SQLException {
        RegionRow region = convertAndPersist(request, Boolean.FALSE, null);
        logger.info("Successfully unlisted region " + request.getName());
        return region;
    }

    public HashMap<String, RegionRow> upsertRegions(Set<RegionRequest> requests,
                                                    HashMap<String, PlayerRepository.PlayerRow> knownPlayers) throws SQLException {
        HashMap<String, RegionRow> result = new HashMap<>();
        for (RegionRequest request : requests) {
            RegionRow region = convertAndPersist(request, null, knownPlayers);
            if (region != null) {
                result.put(identityKey(request), region);
            }
        }
        return result;
    }

    /**
     * Mirrors RegionService.convert + persist. Returns null for invalid requests.
     * active == null leaves an existing region's active flag unchanged (new regions default inactive).
     */
    private RegionRow convertAndPersist(RegionRequest request, Boolean active,
                                        HashMap<String, PlayerRepository.PlayerRow> knownPlayers) throws SQLException {
        if (!regionRequestIsValid(request)) return null;

        Server server = servers.get(request.getServer());
        Location[] bounds = sortBounds(request.getiBounds(), request.getoBounds());

        RegionRow region = new RegionRow();

        // Name and claim shape are mutable (especially for Lands); stable identity is externalId.
        region.name = request.getName().toLowerCase(Locale.ROOT);
        region.server = server.name();
        region.type = request.getType();
        region.externalId = request.getExternalId();
        region.iX = bounds[0].getX();
        region.iY = bounds[0].getY();
        region.iZ = bounds[0].getZ();
        region.oX = bounds[1].getX();
        region.oY = bounds[1].getY();
        region.oZ = bounds[1].getZ();

        List<Long> mayorIds = new ArrayList<>();
        if (knownPlayers != null) {
            for (String p : request.getMayorNames()) {
                PlayerRepository.PlayerRow row = knownPlayers.get(p);
                mayorIds.add(row == null ? null : row.id);
            }
        } else if (!request.getMayorNames().isEmpty()) {
            for (PlayerRepository.PlayerRow row : players.getOrAdd(request.getMayorNames()).values()) {
                mayorIds.add(row.id);
            }
        }

        region.lastUpdated = System.currentTimeMillis();
        region = regions.upsertByIdentity(region, active);
        regions.setMayors(region.id, mayorIds);
        return region;
    }

    public RegionRow findActiveOrSmallest(List<RegionRow> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        return candidates.stream()
                .min(java.util.Comparator
                        .comparingInt(RegionLogicService::associationPriority)
                        .thenComparingLong(RegionLogicService::volume)
                        .thenComparing(r -> r.externalId == null ? r.name : r.externalId,
                                String.CASE_INSENSITIVE_ORDER))
                .orElse(null);
    }

    public boolean regionRequestIsValid(RegionRequest regionRequest) {
        if (regionRequest == null) {
            logger.warning("Filtering out null region request.");
            return false;
        }

        if (regionRequest.getName() == null || regionRequest.getName().isEmpty()) {
            logger.warning("Filtering out region request with invalid name: " + regionRequest);
            return false;
        }

        if (regionRequest.getServer() == null) {
            logger.warning("Filtering out region request with null server: " + regionRequest);
            return false;
        }

        if (servers.get(regionRequest.getServer()) == null) {
            logger.warning("Filtering out region request with invalid server: " + regionRequest);
            return false;
        }

        if (regionRequest.getiBounds() == null) {
            logger.warning("Filtering out region request with invalid iBounds: " + regionRequest);
            return false;
        }

        if (regionRequest.getoBounds() == null) {
            logger.warning("Filtering out region request with invalid oBounds: " + regionRequest);
            return false;
        }

        if (regionRequest.getMayorNames() == null) {
            logger.warning("Filtering out region request with null mayors: " + regionRequest);
            return false;
        }

        for (String mayorName : regionRequest.getMayorNames()) {
            if (!MAYOR_NAME.matcher(mayorName).matches()) {
                logger.warning("Filtering out region request with invalid mayor(s): " + regionRequest);
                return false;
            }
        }

        return true;
    }

    public static String identityKey(RegionRequest request) {
        return request.getServer() + "|" + request.getType() + "|" + request.getExternalId();
    }

    private static int associationPriority(RegionRow region) {
        boolean active = Boolean.TRUE.equals(region.active);
        ShopLocationType type = region.type == null ? ShopLocationType.MARKET_STALL : region.type;
        if (active && type == ShopLocationType.MARKET_STALL) return 0;
        if (active) return 1;
        if (type == ShopLocationType.PLAYER_SHOP) return 2;
        return 3;
    }

    private static long volume(RegionRow region) {
        long x = Math.max(1L, Math.abs((long) region.iX - region.oX) + 1L);
        long y = Math.max(1L, Math.abs((long) region.iY - region.oY) + 1L);
        long z = Math.max(1L, Math.abs((long) region.iZ - region.oZ) + 1L);
        if (x > Long.MAX_VALUE / y) return Long.MAX_VALUE;
        long xy = x * y;
        return z > Long.MAX_VALUE / xy ? Long.MAX_VALUE : xy * z;
    }

    private Location[] sortBounds(Location l1, Location l2) {
        Location lower = new Location(
                Math.min(l1.getX(), l2.getX()),
                Math.min(l1.getY(), l2.getY()),
                Math.min(l1.getZ(), l2.getZ()));
        Location upper = new Location(
                Math.max(l1.getX(), l2.getX()),
                Math.max(l1.getY(), l2.getY()),
                Math.max(l1.getZ(), l2.getZ()));
        return new Location[]{lower, upper};
    }
}
