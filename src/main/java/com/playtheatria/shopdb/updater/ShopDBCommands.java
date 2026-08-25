package com.playtheatria.shopdb.updater;

import com.google.gson.Gson;
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

/** /shopdb list|unlist <region> — ShopDB-Updater's ShopDBCommands. */
public class ShopDBCommands implements CommandExecutor {
    private static final List<String> WORLDS = new ArrayList<>(Arrays.asList("the_ark"));
    private static final int COOLDOWN_TIME = 60000;
    private static final Map<String, Long> COOLDOWNS = new HashMap<>();
    private String server;
    private final RegionManager regionManager;
    private final ShopDBClient client;
    private final Gson gson = new Gson();

    public ShopDBCommands(ShopDBClient client) {
        this.client = client;
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
        if (!(sender instanceof Player) || args.length < 2) {
            return false;
        }

        LocalPlayer player = WorldGuardPlugin.inst().wrapPlayer((Player) sender);
        if (player == null) return false;

        String command = args[0];
        String regionName = args[1];

        if (command == null || command.isEmpty()) return false;
        if (regionName == null || regionName.isEmpty()) return false;

        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null) {
            sender.sendMessage(ChatColor.RED + "That region does not exist.");
            return true;
        }

        if (!region.isOwner(player) || !player.hasPermission("theatria.shopdb.admin")) {
            sender.sendMessage(ChatColor.RED + "You must be an owner of the region to modify it.");
            return true;
        }

        if ("list".equalsIgnoreCase(command) || "unlist".equalsIgnoreCase(command)) {
            if (COOLDOWNS.containsKey(player.getUniqueId().toString())) {
                long time = System.currentTimeMillis() - COOLDOWNS.get(player.getUniqueId().toString());
                if (time < COOLDOWN_TIME) {
                    sender.sendMessage(ChatColor.RED + "Time before next ShopDB command: " + Math.round((COOLDOWN_TIME - time) / 1000.0) + " seconds.");
                    return true;
                }
            }

            COOLDOWNS.put(player.getUniqueId().toString(), System.currentTimeMillis());

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

            if ("list".equalsIgnoreCase(command)) {
                send(rg, sender, "PUT", "Listing", "listed");
            } else {
                send(rg, sender, "DELETE", "Unlisting", "unlisted");
            }

            return true;
        }

        return false;
    }

    private void send(UpdaterRegion region, CommandSender sender, String method, String doing, String done) {
        new Thread(() -> {
            try {
                sender.sendMessage(ChatColor.YELLOW + doing + " shops in region '" + region.getName() + "' in ShopDB...");
                client.sendData(gson.toJson(region), "regions", method);
                sender.sendMessage(ChatColor.GREEN + "Successfully " + done + " shops in region '" + region.getName() + "' in ShopDB.");
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(ChatColor.RED + "Error occurred while " + doing.toLowerCase() + " shops in region '" + region.getName() + "' in ShopDB. " +
                        "Please try again in a few minutes.");
            }
        }).start();
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
