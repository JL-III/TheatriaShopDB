package com.playtheatria.shopdb.updater;

import com.google.gson.Gson;
import com.playtheatria.shopdb.models.ShopLocationType;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Player Lands publishing plus admin-only WorldGuard market-stall management. */
public class ShopDBCommands implements CommandExecutor {
    private static final String ADMIN_PERMISSION = "theatria.shopdb.admin";
    private static final List<String> WORLDS = new ArrayList<>(Arrays.asList("the_ark"));
    private static final int COOLDOWN_TIME = 60000;
    private static final Map<String, Long> COOLDOWNS = new HashMap<>();
    private String server;
    private final RegionManager regionManager;
    private final ShopDBClient client;
    private final PlayerShopResolver playerShops;
    private final Runnable afterPublication;
    private final Gson gson = new Gson();

    public ShopDBCommands(ShopDBClient client) {
        this(client, PlayerShopResolver.unavailable(), null);
    }

    public ShopDBCommands(ShopDBClient client, PlayerShopResolver playerShops,
                          Runnable afterPublication) {
        this.client = client;
        this.playerShops = playerShops == null ? PlayerShopResolver.unavailable() : playerShops;
        this.afterPublication = afterPublication;
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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player) || args.length < 1) {
            return false;
        }

        String command = args[0];
        if (command == null || command.isEmpty()) return false;
        if (!"list".equalsIgnoreCase(command) && !"unlist".equalsIgnoreCase(command)) return false;

        Player player = (Player) sender;
        if (args.length == 1) {
            return handlePlayerShop(player, command);
        }
        return handleMarketStall(player, command, args[1]);
    }

    private boolean handlePlayerShop(Player player, String command) {
        if (!playerShops.isAvailable()) {
            player.sendMessage(ChatColor.RED + "Player shop publishing is unavailable because Lands is not enabled.");
            return true;
        }

        PlayerShopClaim claim = playerShops.findClaim(player.getLocation());
        if (claim == null || claim.region() == null) {
            player.sendMessage(ChatColor.RED + "Stand inside a player-owned land before using this command.");
            return true;
        }

        if (!canModifyLand(player.getUniqueId(), claim.ownerId())) {
            player.sendMessage(ChatColor.RED + "Only the land owner can list or unlist its shops.");
            return true;
        }

        if (!cooldownReady(player)) return true;
        boolean listing = "list".equalsIgnoreCase(command);
        send(claim.region(), player, listing ? "PUT" : "DELETE",
                listing ? "Listing" : "Unlisting", listing ? "listed" : "unlisted",
                "player shop");
        return true;
    }

    private boolean handleMarketStall(Player bukkitPlayer, String command, String regionName) {
        if (!bukkitPlayer.hasPermission(ADMIN_PERMISSION)) {
            bukkitPlayer.sendMessage(ChatColor.RED + "You don't have permission to manage market stalls.");
            return true;
        }
        if (regionName == null || regionName.isEmpty()) return false;

        LocalPlayer player = WorldGuardPlugin.inst().wrapPlayer(bukkitPlayer);
        if (player == null) return false;

        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null) {
            bukkitPlayer.sendMessage(ChatColor.RED + "That market stall does not exist.");
            return true;
        }

        if (!canModifyRegion(region.isOwner(player), bukkitPlayer.hasPermission(ADMIN_PERMISSION))) {
            bukkitPlayer.sendMessage(ChatColor.RED + "You don't have permission to manage market stalls.");
            return true;
        }

        if (!cooldownReady(bukkitPlayer)) return true;

        UpdaterRegion rg = new UpdaterRegion();
        rg.setName(region.getId());
        rg.setServer(this.server);
        rg.setType(ShopLocationType.MARKET_STALL);
        rg.setExternalId(region.getId());
        rg.setOwners(uuidsToPlayerNames(region.getOwners().getUniqueIds()));
        rg.getiBounds().setX(region.getMinimumPoint().getBlockX());
        rg.getiBounds().setY(region.getMinimumPoint().getBlockY());
        rg.getiBounds().setZ(region.getMinimumPoint().getBlockZ());
        rg.getoBounds().setX(region.getMaximumPoint().getBlockX());
        rg.getoBounds().setY(region.getMaximumPoint().getBlockY());
        rg.getoBounds().setZ(region.getMaximumPoint().getBlockZ());

        boolean listing = "list".equalsIgnoreCase(command);
        send(rg, bukkitPlayer, listing ? "PUT" : "DELETE",
                listing ? "Listing" : "Unlisting", listing ? "listed" : "unlisted",
                "market stall");
        return true;
    }

    private boolean cooldownReady(Player player) {
        String key = player.getUniqueId().toString();
        if (COOLDOWNS.containsKey(key)) {
            long time = System.currentTimeMillis() - COOLDOWNS.get(key);
            if (time < COOLDOWN_TIME) {
                player.sendMessage(ChatColor.RED + "Time before next ShopDB command: "
                        + Math.round((COOLDOWN_TIME - time) / 1000.0) + " seconds.");
                return false;
            }
        }
        COOLDOWNS.put(key, System.currentTimeMillis());
        return true;
    }

    /** Region listings are admin-only; WorldGuard ownership neither grants nor restricts access. */
    static boolean canModifyRegion(boolean ownsRegion, boolean hasAdminPermission) {
        return hasAdminPermission;
    }

    /** Player-shop publishing is deliberately owner-only; permissions and trusted membership do not bypass it. */
    static boolean canModifyLand(UUID actor, UUID landOwner) {
        return actor != null && actor.equals(landOwner);
    }

    private void send(UpdaterRegion region, CommandSender sender, String method, String doing,
                      String done, String locationLabel) {
        new Thread(() -> {
            try {
                sender.sendMessage(ChatColor.YELLOW + doing + " shops at " + locationLabel + " '"
                        + region.getName() + "' in ShopDB...");
                int status = client.sendData(gson.toJson(region), "regions", method);
                if (status < 200 || status >= 300) {
                    throw new IllegalStateException("ShopDB API returned HTTP " + status);
                }
                sender.sendMessage(ChatColor.GREEN + "Successfully " + done + " shops at "
                        + locationLabel + " '" + region.getName() + "' in ShopDB.");
                // Publication can change which location wins for every shop in an overlap.
                // Re-read shop locations after both list and unlist, for both providers.
                if (afterPublication != null) afterPublication.run();
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(ChatColor.RED + "Error occurred while " + doing.toLowerCase()
                        + " shops at " + locationLabel + " '" + region.getName() + "' in ShopDB. " +
                        "Please try again in a few minutes.");
            }
        }, "ShopDB-publication").start();
    }

    /** Region name completions for /shopdb list|unlist, filtered by prefix. */
    public List<String> completeRegionNames(String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(java.util.Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String name : regionManager.getRegions().keySet()) {
            if (name.toLowerCase(java.util.Locale.ROOT).startsWith(lower)) {
                result.add(name);
                if (result.size() >= 50) break;
            }
        }
        java.util.Collections.sort(result);
        return result;
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

    private org.bukkit.World getWorld() {
        for (org.bukkit.World world : Bukkit.getServer().getWorlds()) {
            if (WORLDS.contains(world.getName().toLowerCase())) {
                this.server = world.getName();
                return world;
            }
        }

        return null;
    }
}
