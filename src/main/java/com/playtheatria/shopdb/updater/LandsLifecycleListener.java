package com.playtheatria.shopdb.updater;

import com.playtheatria.shopdb.models.Server;
import com.playtheatria.shopdb.services.PlayerShopLifecycleService;
import me.angeschossen.lands.api.events.ChunkDeleteEvent;
import me.angeschossen.lands.api.events.ChunkPostClaimEvent;
import me.angeschossen.lands.api.events.LandDeleteEvent;
import me.angeschossen.lands.api.events.LandOwnerChangeEvent;
import me.angeschossen.lands.api.events.LandRenameEvent;
import me.angeschossen.lands.api.land.Container;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.land.enums.LandType;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reconciles ShopDB after lifecycle changes from the optional Lands plugin.
 * This class must only be instantiated after confirming that Lands is enabled.
 */
public final class LandsLifecycleListener implements Listener {
    private static final String TRACKED_WORLD = "the_ark";
    private static final String TRACKED_SERVER = Server.THE_ARK.name();

    private final Plugin plugin;
    private final PlayerShopLifecycleService lifecycle;
    private final Runnable requestRescan;
    private final Logger logger;
    private volatile boolean active = true;

    public LandsLifecycleListener(Plugin plugin, PlayerShopLifecycleService lifecycle,
                                  Runnable requestRescan, Logger logger) {
        this.plugin = plugin;
        this.lifecycle = lifecycle;
        this.requestRescan = requestRescan;
        this.logger = logger;
    }

    /** Prevents already-scheduled tasks from touching services closed during reload/disable. */
    public void deactivate() {
        active = false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLandDelete(LandDeleteEvent event) {
        Land land = event.getLand();
        if (!isTrackedPlayerLand(land)) return;

        String externalId = land.getULID().toString();
        defer(() -> {
            // LandDeleteEvent is a pre-event. A later handler at the same
            // priority can still cancel it, so confirm the post-tick state.
            if (land.exists()) return;

            reconcile("deleted land " + externalId,
                    () -> lifecycle.landDeleted(TRACKED_SERVER, externalId));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLandOwnerChange(LandOwnerChangeEvent event) {
        // The same event is also emitted for rentable subareas. Only changing
        // the owner of the whole land invalidates the owner's publication grant.
        if (event.getArea() != null) return;

        Land land = event.getLand();
        if (!isTrackedPlayerLand(land)) return;

        String externalId = land.getULID().toString();
        UUID newOwner = event.getTargetUUID();
        defer(() -> {
            if (!land.exists() || newOwner == null || !newOwner.equals(land.getOwnerUID())) return;

            reconcile("owner transfer for land " + externalId,
                    () -> lifecycle.landOwnerChanged(TRACKED_SERVER, externalId));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkDelete(ChunkDeleteEvent event) {
        // Whole-land deletion is reconciled once by LandDeleteEvent instead of
        // running the same land-wide operation for each claimed chunk.
        if (event.getUnclaimType() == ChunkDeleteEvent.UnclaimType.LAND_DELETION) return;

        Land land = event.getLand();
        World world = event.getWorld();
        if (!isTrackedPlayerLand(land) || !isTrackedWorld(world)) return;

        String externalId = land.getULID().toString();
        int chunkX = event.getX();
        int chunkZ = event.getZ();
        defer(() -> {
            // ChunkDeleteEvent is a cancellable pre-event for a normal single
            // unclaim. Selection/all operations settle on the same next tick.
            if (!land.exists() || land.hasChunk(world, chunkX, chunkZ)) return;

            reconcile("unclaimed chunk " + chunkX + "," + chunkZ + " of land " + externalId,
                    () -> lifecycle.chunkUnclaimed(
                            TRACKED_SERVER, externalId, chunkX, chunkZ));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkPostClaim(ChunkPostClaimEvent event) {
        Land land = event.getLand();
        World world = event.getWorld().getWorld();
        if (!isTrackedPlayerLand(land) || !isTrackedWorld(world)) return;

        int chunkX = event.getX();
        int chunkZ = event.getZ();
        defer(() -> {
            if (land.exists() && land.hasChunk(world, chunkX, chunkZ)) {
                requestRescan("claimed chunk " + chunkX + "," + chunkZ
                        + " of land " + land.getULID());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLandRename(LandRenameEvent event) {
        Land land = event.getLand();
        if (!isTrackedPlayerLand(land)) return;

        String newName = event.getNewName();
        defer(() -> {
            if (land.exists() && newName != null && newName.equals(land.getName())) {
                requestRescan("renamed land " + land.getULID() + " to " + newName);
            }
        });
    }

    private void reconcile(String description, SqlReconciliation operation) {
        try {
            PlayerShopLifecycleService.Reconciliation result = operation.run();
            if (result.regionFound()) {
                logger.info("Lands reconciliation: " + description + "; hid "
                        + result.affectedShops() + " shop(s).");
            }
        } catch (SQLException e) {
            logger.severe("Lands reconciliation failed for " + description + ": " + e);
        } finally {
            requestRescan(description);
        }
    }

    private void requestRescan(String reason) {
        if (!active || requestRescan == null || !plugin.isEnabled()) return;
        try {
            requestRescan.run();
        } catch (RuntimeException e) {
            logger.warning("Could not request a shop rescan after " + reason + ": " + e);
        }
    }

    private void defer(Runnable action) {
        if (!active) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (active && plugin.isEnabled()) action.run();
        }, 1L);
    }

    private static boolean isTrackedPlayerLand(Land land) {
        if (land == null || land.getLandType() != LandType.LAND) return false;
        for (Container container : land.getContainers()) {
            if (container != null && TRACKED_WORLD.equalsIgnoreCase(container.getWorld().getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTrackedWorld(World world) {
        return world != null && TRACKED_WORLD.equalsIgnoreCase(world.getName());
    }

    @FunctionalInterface
    private interface SqlReconciliation {
        PlayerShopLifecycleService.Reconciliation run() throws SQLException;
    }
}
