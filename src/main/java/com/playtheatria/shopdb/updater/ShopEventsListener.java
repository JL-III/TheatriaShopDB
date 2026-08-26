package com.playtheatria.shopdb.updater;

import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.Events.ItemParseEvent;
import com.Acrobot.ChestShop.Events.ShopCreatedEvent;
import com.Acrobot.ChestShop.Events.ShopDestroyedEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.google.gson.Gson;
import com.playtheatria.shopdb.models.EventType;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.Acrobot.ChestShop.Signs.ChestShopSign.ITEM_LINE;
import static com.Acrobot.ChestShop.Signs.ChestShopSign.NAME_LINE;
import static com.Acrobot.ChestShop.Signs.ChestShopSign.PRICE_LINE;
import static com.Acrobot.ChestShop.Signs.ChestShopSign.QUANTITY_LINE;

/** ShopDB-Updater's ShopEventsListener, writing into the in-plugin event buffer. */
public class ShopEventsListener implements Listener {
    private static final List<String> WORLDS = new ArrayList<>(Arrays.asList("the_ark"));
    private static final Gson gson = new Gson();
    private final EventBuffer buffer;
    private final RegionManager regionManager;
    private String server;

    public ShopEventsListener(EventBuffer buffer) {
        this.buffer = buffer;
        org.bukkit.World bukkitWorld = getWorld();

        if (bukkitWorld == null) {
            throw new NullPointerException("Couldn't find world by names: " + WORLDS);
        }

        World world = BukkitAdapter.adapt(bukkitWorld);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(world);

        if (regionManager == null) {
            throw new NullPointerException("No region manager found for world: " + world.getName());
        }

        this.regionManager = regionManager;
    }

    @EventHandler
    public void onShopCreation(ShopCreatedEvent event) {
        if (ChestShopSign.isAdminShop(event.getSignLine(NAME_LINE)) || event.getContainer() == null) {
            return;
        }

        ItemStack itemTradedByShop = determineItemTradedByShop(event.getSignLine(ITEM_LINE));

        BufferedShopEvent shopEvent = new BufferedShopEvent();
        shopEvent.id = IDGenerator.generateID(event.getSign().getLocation());
        shopEvent.eventType = EventType.CREATE;
        shopEvent.world = event.getSign().getWorld().getName();
        shopEvent.regions = gson.toJson(findRegions(event.getSign().getX(), event.getSign().getY(), event.getSign().getZ()));
        shopEvent.x = event.getSign().getX();
        shopEvent.y = event.getSign().getY();
        shopEvent.z = event.getSign().getZ();
        shopEvent.owner = event.getSignLine(NAME_LINE);
        shopEvent.quantity = QuantityUtil.parseQuantity(event.getSignLine(QUANTITY_LINE));
        shopEvent.count = itemTradedByShop == null ? 0 : InventoryUtil.getAmount(itemTradedByShop, event.getContainer().getInventory());
        shopEvent.buyPrice = PriceUtil.getExactBuyPrice(event.getSignLine(PRICE_LINE));
        shopEvent.sellPrice = PriceUtil.getExactSellPrice(event.getSignLine(PRICE_LINE));
        shopEvent.item = event.getSignLine(ITEM_LINE);
        shopEvent.full = ChestShopUtil.chestIsFull(itemTradedByShop, event.getContainer().getInventory());
        applyItemDetails(shopEvent, itemTradedByShop);

        buffer.createOrUpdate(shopEvent);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() == InventoryType.ENDER_CHEST
                || event.getInventory().getLocation() == null
                || !ChestShopSign.isShopBlock(event.getInventory().getLocation().getBlock())) {
            return;
        }

        for (Sign shopSign : ChestShopUtil.findConnectedShopSigns(event.getInventory().getHolder())) {
            if (ChestShopSign.isAdminShop(shopSign)) {
                return;
            }

            buffer.createOrUpdate(buildUpdateEvent(shopSign, event.getInventory()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTransaction(final TransactionEvent event) {
        if (ChestShopSign.isAdminShop(event.getSign())) {
            return;
        }

        for (Sign shopSign : ChestShopUtil.findConnectedShopSigns(event.getOwnerInventory().getHolder())) {
            if (ChestShopSign.isAdminShop(shopSign)) {
                return;
            }

            buffer.createOrUpdate(buildUpdateEvent(shopSign, event.getOwnerInventory()));
        }
    }

    @EventHandler
    public void onShopDestruction(ShopDestroyedEvent event) {
        if (ChestShopSign.isAdminShop(event.getSign())) {
            return;
        }

        BufferedShopEvent shopEvent = new BufferedShopEvent();
        shopEvent.id = IDGenerator.generateID(event.getSign().getLocation());
        shopEvent.eventType = EventType.DELETE;

        buffer.createOrUpdate(shopEvent);
    }

    /** Builds an UPDATE event from a shop sign and its container inventory (also used by the rescanner). */
    public BufferedShopEvent buildUpdateEvent(Sign shopSign, Inventory chestShopInventory) {
        ItemStack itemTradedByShop = determineItemTradedByShop(shopSign);

        BufferedShopEvent shopEvent = new BufferedShopEvent();
        shopEvent.id = IDGenerator.generateID(shopSign.getLocation());
        shopEvent.eventType = EventType.UPDATE;
        shopEvent.world = shopSign.getWorld().getName();
        shopEvent.regions = gson.toJson(findRegions(shopSign.getX(), shopSign.getY(), shopSign.getZ()));
        shopEvent.x = shopSign.getX();
        shopEvent.y = shopSign.getY();
        shopEvent.z = shopSign.getZ();
        shopEvent.owner = shopSign.getLine(NAME_LINE);
        shopEvent.quantity = QuantityUtil.parseQuantity(shopSign.getLine(QUANTITY_LINE));
        shopEvent.count = itemTradedByShop == null ? 0 : InventoryUtil.getAmount(itemTradedByShop, chestShopInventory);
        shopEvent.buyPrice = PriceUtil.getExactBuyPrice(shopSign.getLine(PRICE_LINE));
        shopEvent.sellPrice = PriceUtil.getExactSellPrice(shopSign.getLine(PRICE_LINE));
        shopEvent.item = shopSign.getLine(ITEM_LINE);
        shopEvent.full = itemTradedByShop == null || ChestShopUtil.chestIsFull(itemTradedByShop, chestShopInventory);
        applyItemDetails(shopEvent, itemTradedByShop);

        return shopEvent;
    }

    private static void applyItemDetails(BufferedShopEvent shopEvent, ItemStack itemTradedByShop) {
        shopEvent.baseMaterial = ItemDetailsExtractor.baseMaterial(itemTradedByShop);
        com.playtheatria.shopdb.models.ItemDetailsDto details = ItemDetailsExtractor.details(itemTradedByShop);
        shopEvent.itemDetails = details == null ? null : gson.toJson(details);
    }

    public static ItemStack determineItemTradedByShop(Sign sign) {
        return determineItemTradedByShop(sign.getLine(ITEM_LINE));
    }

    public static ItemStack determineItemTradedByShop(String material) {
        ItemParseEvent parseEvent = new ItemParseEvent(material);
        Bukkit.getPluginManager().callEvent(parseEvent);
        return parseEvent.getItem();
    }

    private List<UpdaterRegion> findRegions(int x, int y, int z) {
        List<UpdaterRegion> result = new ArrayList<>();

        BlockVector3 vec = BlockVector3.at(x, y, z);
        ApplicableRegionSet set = regionManager.getApplicableRegions(vec);

        for (ProtectedRegion region : set.getRegions()) {
            UpdaterRegion rg = new UpdaterRegion();
            rg.setName(region.getId());
            rg.setServer(this.server);
            rg.setOwners(uuidsToPlayerNames(region.getOwners().getUniqueIds()));
            rg.getiBounds().setX(region.getMinimumPoint().getBlockX());
            rg.getiBounds().setY(region.getMinimumPoint().getBlockY());
            rg.getiBounds().setZ(region.getMinimumPoint().getBlockZ());
            rg.getoBounds().setX(region.getMaximumPoint().getBlockX());
            rg.getoBounds().setY(region.getMaximumPoint().getBlockY());
            rg.getoBounds().setZ(region.getMaximumPoint().getBlockZ());
            result.add(rg);
        }

        return result;
    }

    private org.bukkit.World getWorld() {
        for (org.bukkit.World world : Bukkit.getServer().getWorlds()) {
            if (WORLDS.contains(world.getName().toLowerCase())) {
                this.server = world.getName();
                return world;
            }
        }

        return null;
    }

    private Set<String> uuidsToPlayerNames(Set<UUID> uuids) {
        Set<String> playerNames = new HashSet<>();
        if (uuids == null) return playerNames;

        for (UUID uuid : uuids) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String playerName = player.getName();
            if (playerName == null) continue;
            playerNames.add(playerName);
        }

        return playerNames;
    }
}
