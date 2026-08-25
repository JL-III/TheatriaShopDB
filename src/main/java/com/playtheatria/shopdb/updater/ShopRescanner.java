package com.playtheatria.shopdb.updater;

import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.Utils.uBlock;
import com.playtheatria.shopdb.database.ShopRepository;
import com.playtheatria.shopdb.models.EventType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Walks every shop location known to the database and re-reads it from the
 * world, refreshing item metadata (and pruning rows whose sign is gone).
 * Built to be safe on a live production server:
 *
 * - chunks are loaded with {@code getChunkAtAsync} (never a sync load stall);
 * - exactly one chunk is in flight at a time, and the next one starts only
 *   {@code paceTicks} ticks after the previous finished, so main-thread work
 *   per tick is bounded by the handful of signs inside a single chunk;
 * - no chunk is force-loaded — Paper unloads them again on its own.
 *
 * Events go through the normal buffer, so the API picks them up on the next
 * updater cycle (a flush is kicked off when the scan completes).
 */
public class ShopRescanner {
    private final Plugin plugin;
    private final ShopRepository shops;
    private final EventBuffer buffer;
    private final ShopEventsListener listener;
    private final ShopUpdater updater;
    private final Logger logger;
    private final int paceTicks;

    private volatile boolean running;
    private volatile boolean cancelled;
    private Deque<ChunkGroup> queue;
    private int totalChunks;
    private int refreshed;
    private int stale;

    public ShopRescanner(Plugin plugin, ShopRepository shops, EventBuffer buffer,
                         ShopEventsListener listener, ShopUpdater updater, int paceTicks, Logger logger) {
        this.plugin = plugin;
        this.shops = shops;
        this.buffer = buffer;
        this.listener = listener;
        this.updater = updater;
        this.paceTicks = Math.max(1, paceTicks);
        this.logger = logger;
    }

    public boolean isRunning() {
        return running;
    }

    /** Starts a scan; returns a message describing what happened. */
    public synchronized String start() {
        if (running) {
            return "A rescan is already running.";
        }

        World world = findTrackedWorld();
        if (world == null) {
            return "Tracked world not found - cannot rescan.";
        }

        List<ShopRepository.ShopCoord> coords;
        try {
            coords = shops.findAllCoords();
        } catch (SQLException e) {
            return "Failed to read shop locations from the database: " + e.getMessage();
        }
        if (coords.isEmpty()) {
            return "The database has no shops to rescan.";
        }

        Map<Long, ChunkGroup> groups = new LinkedHashMap<>();
        for (ShopRepository.ShopCoord coord : coords) {
            long key = (((long) (coord.x >> 4)) << 32) | ((coord.z >> 4) & 0xFFFFFFFFL);
            groups.computeIfAbsent(key, k -> new ChunkGroup(coord.x >> 4, coord.z >> 4)).shops.add(coord);
        }

        queue = new ArrayDeque<>(groups.values());
        totalChunks = queue.size();
        refreshed = 0;
        stale = 0;
        cancelled = false;
        running = true;

        logger.info("Rescan started: " + coords.size() + " shops across " + totalChunks
                + " chunks, one chunk every " + paceTicks + " tick(s).");
        scheduleNext(world);
        return "Rescan started: " + coords.size() + " shops across " + totalChunks
                + " chunks. Progress is logged to console.";
    }

    /** Stops an in-progress scan; returns false when none is running. */
    public synchronized boolean cancel() {
        if (!running) return false;
        cancelled = true;
        return true;
    }

    private void scheduleNext(World world) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> step(world), paceTicks);
    }

    private void step(World world) {
        if (!plugin.isEnabled() || cancelled || queue.isEmpty()) {
            finish();
            return;
        }

        ChunkGroup group = queue.poll();
        world.getChunkAtAsync(group.chunkX, group.chunkZ)
                .thenAccept(chunk -> {
                    // Runs on the main thread.
                    if (plugin.isEnabled() && !cancelled) {
                        processGroup(world, group);
                    }
                    scheduleNext(world);
                })
                .exceptionally(e -> {
                    logger.warning("Rescan: failed to load chunk " + group.chunkX + "," + group.chunkZ + ": " + e);
                    Bukkit.getScheduler().runTask(plugin, () -> scheduleNext(world));
                    return null;
                });
    }

    private void processGroup(World world, ChunkGroup group) {
        for (ShopRepository.ShopCoord coord : group.shops) {
            BlockState state = world.getBlockAt(coord.x, coord.y, coord.z).getState();

            Sign sign = state instanceof Sign ? (Sign) state : null;
            Container container = null;
            if (sign != null && ChestShopSign.isValid(sign) && !ChestShopSign.isAdminShop(sign)) {
                container = uBlock.findConnectedContainer(sign);
            }

            if (container != null) {
                buffer.createOrUpdate(listener.buildUpdateEvent(sign, container.getInventory()));
                refreshed++;
            } else {
                // No sign, not a (player) shop sign anymore, or no chest behind
                // it: the row advertises a shop that no longer exists.
                BufferedShopEvent delete = new BufferedShopEvent();
                delete.id = coord.id;
                delete.eventType = EventType.DELETE;
                buffer.createOrUpdate(delete);
                stale++;
            }
        }

        int done = totalChunks - queue.size();
        if (done % 50 == 0 || queue.isEmpty()) {
            logger.info("Rescan progress: " + done + "/" + totalChunks + " chunks ("
                    + refreshed + " refreshed, " + stale + " stale).");
        }
    }

    private void finish() {
        synchronized (this) {
            if (!running) return;
            running = false;
        }
        String outcome = cancelled ? "cancelled" : "complete";
        logger.info("Rescan " + outcome + ": " + refreshed + " shops refreshed, "
                + stale + " stale rows queued for deletion.");
        if (updater != null && plugin.isEnabled()) {
            updater.flushAsync();
        }
    }

    private World findTrackedWorld() {
        for (World world : Bukkit.getServer().getWorlds()) {
            if ("the_ark".equals(world.getName().toLowerCase(Locale.ROOT))) {
                return world;
            }
        }
        return null;
    }

    private static class ChunkGroup {
        final int chunkX;
        final int chunkZ;
        final List<ShopRepository.ShopCoord> shops = new ArrayList<>();

        ChunkGroup(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }
}
