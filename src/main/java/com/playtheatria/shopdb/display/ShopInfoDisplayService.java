package com.playtheatria.shopdb.display;

import com.Acrobot.Breeze.Utils.BlockUtil;
import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.Utils.uBlock;
import com.playtheatria.shopdb.ShopDBPlugin;
import com.playtheatria.shopdb.updater.ChestShopUtil;
import com.playtheatria.shopdb.updater.QuantityUtil;
import com.playtheatria.shopdb.updater.ShopEventsListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class ShopInfoDisplayService implements Listener {
    private static final double TEXT_Y_OFFSET = 0.80;
    private static final double ITEM_Y_OFFSET = 0.65;
    private static final float MAX_TEXT_SCALE = 0.4f;
    private static final float MIN_TEXT_SCALE = 0.16f;
    private static final int TEXT_LINES_AT_MAX_SCALE = 6;
    private static final float ITEM_SCALE = 0.25f;
    private static final int TARGET_MISS_GRACE_SCANS = 2;
    private static final int TARGET_CHANGE_CONFIRMATION_SCANS = 1;
    private static final String ENTITY_TAG = "shopdb_info_display";
    private static final int SPIN_SECONDS_PER_ROTATION = 6;

    private final ShopDBPlugin plugin;
    private final int scanIntervalTicks;
    private final int rangeBlocks;
    private final int stockRefreshTicks;
    private final Map<UUID, ActiveDisplay> activeDisplays = new HashMap<>();
    private final Map<UUID, TargetKey> unresolvedTargets = new HashMap<>();
    private final Map<UUID, PendingTarget> pendingTargets = new HashMap<>();
    private final Set<UUID> playersWithLoggedScanError = new HashSet<>();
    private BukkitTask scanTask;

    public ShopInfoDisplayService(ShopDBPlugin plugin, int scanIntervalTicks,
                                  int rangeBlocks, int stockRefreshTicks) {
        this.plugin = plugin;
        this.scanIntervalTicks = scanIntervalTicks;
        this.rangeBlocks = rangeBlocks;
        this.stockRefreshTicks = stockRefreshTicks;
    }

    public void start() {
        sweepLeftoverEntities();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        scanTask = new BukkitRunnable() {
            @Override
            public void run() {
                scanPlayers();
            }
        }.runTaskTimer(plugin, 0L, scanIntervalTicks);
    }

    public void stop() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        for (ActiveDisplay active : activeDisplays.values()) {
            despawn(active);
        }
        activeDisplays.clear();
        unresolvedTargets.clear();
        pendingTargets.clear();
        playersWithLoggedScanError.clear();
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        removePlayerState(playerId);
        playersWithLoggedScanError.remove(playerId);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        removePlayerState(playerId);
        playersWithLoggedScanError.remove(playerId);
    }

    private void scanPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            try {
                scanPlayer(player);
                playersWithLoggedScanError.remove(playerId);
            } catch (RuntimeException exception) {
                removePlayerState(playerId);
                if (playersWithLoggedScanError.add(playerId)) {
                    plugin.getLogger().log(Level.WARNING,
                            "Failed to update shop info display for " + player.getName(), exception);
                }
            }
        }
    }

    private void scanPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        ActiveDisplay active = activeDisplays.get(playerId);
        if (active != null && (!active.itemEntity.isValid() || !active.textEntity.isValid())) {
            activeDisplays.remove(playerId);
            despawn(active);
            active = null;
            pendingTargets.remove(playerId);
        }

        Sign sign = findTargetShopSign(player);
        if (sign == null) {
            if (active != null && isPlayerInFrontOfActiveSign(player, active)) {
                active.missedTargetScans++;
                if (isWithinTargetMissGrace(active.missedTargetScans)) {
                    rotate(active);
                    return;
                }
            }
            removePlayerState(playerId);
            return;
        }

        Location signLocation = sign.getLocation();
        String itemLine = sign.getLine(ChestShopSign.ITEM_LINE);
        if (active != null) {
            active.missedTargetScans = 0;
        }
        if (active != null
                && active.signLocation.equals(signLocation)
                && active.itemLine.equals(itemLine)) {
            unresolvedTargets.remove(playerId);
            pendingTargets.remove(playerId);
            rotate(active);
            refreshStockIfDue(active, sign);
            return;
        }

        if (active != null) {
            TargetKey targetKey = new TargetKey(signLocation, itemLine);
            PendingTarget pending = pendingTargets.get(playerId);
            int observations = pending != null && pending.targetKey.equals(targetKey)
                    ? pending.observations + 1
                    : 1;
            if (!isTargetChangeConfirmed(observations)) {
                pendingTargets.put(playerId, new PendingTarget(targetKey, observations));
                rotate(active);
                return;
            }
            pendingTargets.remove(playerId);
            activeDisplays.remove(playerId);
            despawn(active);
        } else {
            pendingTargets.remove(playerId);
        }

        TargetKey targetKey = new TargetKey(signLocation, itemLine);
        if (targetKey.equals(unresolvedTargets.get(playerId))) {
            return;
        }
        unresolvedTargets.remove(playerId);

        ItemStack item = ShopEventsListener.determineItemTradedByShop(sign);
        if (item == null) {
            unresolvedTargets.put(playerId, targetKey);
            return;
        }

        ActiveDisplay spawned = spawnDisplay(player, sign, item, itemLine);
        activeDisplays.put(playerId, spawned);
    }

    private Sign findTargetShopSign(Player player) {
        Block target = player.getTargetBlockExact(rangeBlocks);
        if (target == null) {
            return null;
        }

        if (BlockUtil.isSign(target)) {
            Sign sign = (Sign) target.getState();
            return isTargetableFromPlayerPosition(player, sign) ? sign : null;
        }

        if (!ChestShopSign.isShopBlock(target)) {
            return null;
        }

        if (!(target.getState() instanceof Container container)) {
            return null;
        }
        InventoryHolder inventoryHolder = container.getInventory().getHolder();
        if (inventoryHolder == null) {
            return null;
        }

        List<Sign> connectedSigns = ChestShopUtil.findConnectedShopSigns(inventoryHolder);
        for (Sign sign : connectedSigns) {
            if (isTargetableFromPlayerPosition(player, sign)) {
                return sign;
            }
        }
        return null;
    }

    private boolean isPlayerInFrontOfActiveSign(Player player, ActiveDisplay active) {
        if (!(active.signLocation.getBlock().getState() instanceof Sign sign)) {
            return false;
        }
        return isTargetableFromPlayerPosition(player, sign);
    }

    private boolean isTargetableFromPlayerPosition(Player player, Sign sign) {
        if (!ChestShopSign.isValid(sign)) {
            return false;
        }

        BlockFace facing = signFacing(sign.getBlockData());
        if (facing == null) {
            return false;
        }

        Location center = sign.getLocation().toCenterLocation();
        Location playerLocation = player.getLocation();
        return isPositionInFrontOfSign(
                center.getX(), center.getZ(), facing,
                playerLocation.getX(), playerLocation.getZ());
    }

    private static BlockFace signFacing(BlockData blockData) {
        if (blockData instanceof Directional directional) {
            return directional.getFacing();
        }
        if (blockData instanceof Rotatable rotatable) {
            return rotatable.getRotation();
        }
        return null;
    }

    static boolean isPositionInFrontOfSign(double signX, double signZ, BlockFace facing,
                                           double playerX, double playerZ) {
        double offsetX = playerX - signX;
        double offsetZ = playerZ - signZ;
        return offsetX * facing.getModX() + offsetZ * facing.getModZ() > 0;
    }

    static boolean isWithinTargetMissGrace(int consecutiveMisses) {
        return consecutiveMisses < TARGET_MISS_GRACE_SCANS;
    }

    static boolean isTargetChangeConfirmed(int consecutiveObservations) {
        return consecutiveObservations >= TARGET_CHANGE_CONFIRMATION_SCANS;
    }

    private ActiveDisplay spawnDisplay(Player player, Sign sign, ItemStack item, String itemLine) {
        ItemStack shown = item.clone();
        shown.setAmount(1);

        Component component = buildText(sign, item);
        Location base = sign.getLocation().toCenterLocation();
        World world = base.getWorld();

        TextDisplay text = world.spawn(base.clone().add(0, TEXT_Y_OFFSET, 0), TextDisplay.class, e -> {
            e.setVisibleByDefault(false);
            e.setPersistent(false);
            e.addScoreboardTag(ENTITY_TAG);
            updateText(e, component);
            e.setBillboard(Display.Billboard.CENTER);
            e.setAlignment(TextDisplay.TextAlignment.CENTER);
            e.setShadowed(false);
            e.setSeeThrough(false);
            e.setBackgroundColor(Color.fromARGB(0xB0, 0, 0, 0));
        });

        ItemDisplay ghost = null;
        try {
            player.showEntity(plugin, text);
            ghost = world.spawn(base.clone().add(0, ITEM_Y_OFFSET, 0), ItemDisplay.class, e -> {
                e.setVisibleByDefault(false);
                e.setPersistent(false);
                e.addScoreboardTag(ENTITY_TAG);
                e.setItemStack(shown);
                e.setBillboard(Display.Billboard.FIXED);
                e.setTransformation(new Transformation(
                        new Vector3f(),
                        new Quaternionf(),
                        new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE),
                        new Quaternionf()));
            });
            player.showEntity(plugin, ghost);
            return new ActiveDisplay(sign.getLocation(), itemLine, item, ghost, text);
        } catch (RuntimeException exception) {
            text.remove();
            if (ghost != null) {
                ghost.remove();
            }
            throw exception;
        }
    }

    private void rotate(ActiveDisplay active) {
        active.spinAngleDeg += 360f * (scanIntervalTicks / 20f) / SPIN_SECONDS_PER_ROTATION;
        active.itemEntity.setInterpolationDelay(0);
        active.itemEntity.setInterpolationDuration(scanIntervalTicks);
        active.itemEntity.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotationY((float) Math.toRadians(active.spinAngleDeg)),
                new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE),
                new Quaternionf()));
    }

    private void refreshStockIfDue(ActiveDisplay active, Sign sign) {
        active.ticksSinceStockRefresh += scanIntervalTicks;
        if (active.ticksSinceStockRefresh >= stockRefreshTicks) {
            updateText(active.textEntity, buildText(sign, active.item));
            active.ticksSinceStockRefresh = 0;
        }
    }

    private static void updateText(TextDisplay textDisplay, Component component) {
        float scale = textScaleForLineCount(ShopInfoTextBuilder.estimatedRenderedLineCount(component));
        textDisplay.text(component);
        textDisplay.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf()));
    }

    static float textScaleForLineCount(int lineCount) {
        float scale = MAX_TEXT_SCALE * TEXT_LINES_AT_MAX_SCALE / Math.max(1, lineCount);
        return Math.max(MIN_TEXT_SCALE, Math.min(MAX_TEXT_SCALE, scale));
    }

    private Component buildText(Sign sign, ItemStack item) {
        boolean adminShop = ChestShopSign.isAdminShop(sign);
        String priceLine = sign.getLine(ChestShopSign.PRICE_LINE);
        boolean hasBuyPrice = PriceUtil.getExactBuyPrice(priceLine).compareTo(BigDecimal.ZERO) >= 0;
        boolean hasSellPrice = PriceUtil.getExactSellPrice(priceLine).compareTo(BigDecimal.ZERO) >= 0;
        int quantity = QuantityUtil.parseQuantity(sign.getLine(ChestShopSign.QUANTITY_LINE));

        Integer stockCount = null;
        boolean showShopFull = false;
        if (!adminShop) {
            Container container = uBlock.findConnectedContainer(sign);
            if (container != null) {
                Inventory inventory = container.getInventory();
                stockCount = InventoryUtil.getAmount(item, inventory);
                showShopFull = hasSellPrice && ChestShopUtil.chestIsFull(item, inventory);
            }
        }

        return ShopInfoTextBuilder.build(
                item, adminShop, stockCount, quantity, hasBuyPrice, showShopFull);
    }

    private void removePlayerState(UUID playerId) {
        ActiveDisplay active = activeDisplays.remove(playerId);
        if (active != null) {
            despawn(active);
        }
        unresolvedTargets.remove(playerId);
        pendingTargets.remove(playerId);
    }

    private void despawn(ActiveDisplay active) {
        active.itemEntity.remove();
        active.textEntity.remove();
    }

    private void sweepLeftoverEntities() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Display display : world.getEntitiesByClass(Display.class)) {
                if (display.getScoreboardTags().contains(ENTITY_TAG)) {
                    display.remove();
                }
            }
        }
    }

    private record TargetKey(Location signLocation, String itemLine) {
    }

    private record PendingTarget(TargetKey targetKey, int observations) {
    }
}
