package com.playtheatria.shopdb.updater;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Posts buffered shop events to the (now in-plugin) ShopDB API every
 * config interval — ShopDB-Updater's ShopUpdater, plus a synchronous
 * flush used on plugin disable so a restart doesn't sit on events.
 */
public class ShopUpdater {
    private static final Gson gson = new Gson();

    private final Plugin plugin;
    private final EventBuffer buffer;
    private final ShopDBClient client;
    private final UpdaterConfig config;
    private final Logger logger;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ShopUpdater(Plugin plugin, EventBuffer buffer, ShopDBClient client, UpdaterConfig config, Logger logger) {
        this.plugin = plugin;
        this.buffer = buffer;
        this.client = client;
        this.config = config;
        this.logger = logger;
    }

    public void startSubmitting() {
        final Runnable submitTask = () -> {
            if (!plugin.isEnabled()) {
                scheduler.shutdown();
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> new Thread(this::submitData).start());
        };

        scheduler.scheduleAtFixedRate(submitTask, 0, config.intervalMinutes, TimeUnit.MINUTES);
    }

    /**
     * Flushes the buffer and waits for completion. The HTTP client refuses to run
     * on the main thread, and disable/reload happen there — so the POST runs on a
     * worker thread we join (bounded, in case the API is unresponsive).
     */
    public void flushNow() {
        if (Bukkit.isPrimaryThread()) {
            Thread worker = new Thread(this::submitData, "ShopDB-flush");
            worker.start();
            try {
                worker.join(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            submitData();
        }
    }

    /** Kicks off a buffer flush on a worker thread without blocking the caller. */
    public void flushAsync() {
        new Thread(this::submitData, "ShopDB-flush-async").start();
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private synchronized void submitData() {
        try {
            List<BufferedShopEvent> shopEvents = buffer.findAll();

            if (shopEvents == null || shopEvents.isEmpty()) {
                if (config.logHttp) {
                    logger.info("No shop events found, will not send data to ShopDB.");
                }
                return;
            }

            List<ShopEventDTO> dtos = new ArrayList<>();
            for (BufferedShopEvent shopEvent : shopEvents) {
                ShopEventDTO dto = new ShopEventDTO();
                dto.setId(shopEvent.id);
                dto.setEventType(shopEvent.eventType);
                dto.setRegions(gson.fromJson(shopEvent.regions, UpdaterRegion[].class));
                dto.setWorld(shopEvent.world);
                dto.setX(shopEvent.x);
                dto.setY(shopEvent.y);
                dto.setZ(shopEvent.z);
                dto.setOwner(shopEvent.owner);
                dto.setQuantity(shopEvent.quantity);
                dto.setCount(shopEvent.count);
                dto.setBuyPrice(shopEvent.buyPrice);
                dto.setSellPrice(shopEvent.sellPrice);
                dto.setItem(shopEvent.item);
                dto.setFull(shopEvent.full);
                dto.setBaseMaterial(shopEvent.baseMaterial);
                dto.setItemDetails(shopEvent.itemDetails == null
                        ? null
                        : gson.fromJson(shopEvent.itemDetails, com.playtheatria.shopdb.models.ItemDetailsDto.class));
                dtos.add(dto);
            }

            int responseCode = client.sendData(gson.toJson(dtos), "chest-shops", "POST");

            // Only acknowledge the uploaded revisions once ShopDB confirms;
            // events written while this request was in flight must survive.
            if (responseCode >= 200 && responseCode < 300) {
                buffer.acknowledge(shopEvents);
            }
        } catch (Exception e) {
            logger.warning("Failed to submit data to ShopDB: " + e.getMessage());
        }
    }
}
