package com.playtheatria.shopdb.updater;

import com.playtheatria.shopdb.models.ShopLocationType;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.Area;
import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Container;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.land.enums.LandType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Typed bridge to the optional Lands plugin. This class is loaded only when Lands is enabled. */
public final class LandsPlayerShopResolver implements PlayerShopResolver {
    private final LandsIntegration lands;

    public LandsPlayerShopResolver(Plugin plugin) {
        this.lands = LandsIntegration.of(plugin);
    }

    @Override
    public PlayerShopClaim findClaim(Location location) {
        if (location == null || location.getWorld() == null) return null;

        Area area = lands.getArea(location);
        if (area == null) return null;

        Land land = area.getLand();
        if (land == null || land.getLandType() != LandType.LAND) return null;

        Container container = land.getContainer(location.getWorld());
        if (container == null || container.getChunks().isEmpty()) return null;

        int minChunkX = Integer.MAX_VALUE;
        int minChunkZ = Integer.MAX_VALUE;
        int maxChunkX = Integer.MIN_VALUE;
        int maxChunkZ = Integer.MIN_VALUE;
        for (ChunkCoordinate chunk : container.getChunks()) {
            minChunkX = Math.min(minChunkX, chunk.getX());
            minChunkZ = Math.min(minChunkZ, chunk.getZ());
            maxChunkX = Math.max(maxChunkX, chunk.getX());
            maxChunkZ = Math.max(maxChunkZ, chunk.getZ());
        }

        UUID ownerId = land.getOwnerUID();
        Set<String> owners = new HashSet<>();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
        if (owner.getName() != null) owners.add(owner.getName());

        UpdaterRegion region = new UpdaterRegion();
        region.setName(land.getName());
        region.setServer(location.getWorld().getName());
        region.setType(ShopLocationType.PLAYER_SHOP);
        region.setExternalId(land.getULID().toString());
        region.setOwners(owners);
        region.getiBounds().setX(minChunkX << 4);
        region.getiBounds().setY(container.getMinY());
        region.getiBounds().setZ(minChunkZ << 4);
        region.getoBounds().setX((maxChunkX << 4) + 15);
        region.getoBounds().setY(container.getMaxY());
        region.getoBounds().setZ((maxChunkZ << 4) + 15);
        return new PlayerShopClaim(region, ownerId);
    }
}
