package com.playtheatria.shopdb.commands;

import com.playtheatria.shopdb.ShopDBPlugin;
import com.playtheatria.shopdb.updater.ShopDBCommands;
import com.playtheatria.shopdb.updater.ShopRescanner;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /shopdb — player owners publish the Lands claim they are standing in;
 * admins may additionally manage named WorldGuard market stalls and services.
 */
public class ShopDBRootCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "theatria.shopdb.admin";

    private final ShopDBPlugin plugin;

    public ShopDBRootCommand(ShopDBPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to reload ShopDB.");
                return true;
            }
            sender.sendMessage(ChatColor.YELLOW + "Reloading ShopDB...");
            if (plugin.reloadServices()) {
                sender.sendMessage(ChatColor.GREEN + "ShopDB reloaded.");
            } else {
                sender.sendMessage(ChatColor.RED + "ShopDB reload failed - check the server log.");
            }
            return true;
        }

        if (args.length >= 1 && "rescan".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to rescan shops.");
                return true;
            }
            ShopRescanner rescanner = plugin.getRescanner();
            if (rescanner == null) {
                sender.sendMessage(ChatColor.RED + "The shop event updater is not running (see the server log).");
                return true;
            }
            if (args.length >= 2 && "cancel".equalsIgnoreCase(args[1])) {
                sender.sendMessage(rescanner.cancel()
                        ? ChatColor.YELLOW + "Rescan cancelled."
                        : ChatColor.RED + "No rescan is running.");
                return true;
            }
            sender.sendMessage(ChatColor.YELLOW + rescanner.start());
            return true;
        }

        ShopDBCommands delegate = plugin.getUpdaterCommands();
        if (delegate == null) {
            sender.sendMessage(ChatColor.RED + "The shop event updater is not running (see the server log).");
            return true;
        }
        return delegate.onCommand(sender, cmd, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>(List.of("list", "unlist"));
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                options.add("reload");
                options.add("rescan");
            }
            for (String option : options) {
                if (option.startsWith(prefix)) result.add(option);
            }
            return result;
        }

        if (args.length == 2 && "rescan".equalsIgnoreCase(args[0])
                && sender.hasPermission(ADMIN_PERMISSION)
                && "cancel".startsWith(args[1].toLowerCase(Locale.ROOT))) {
            result.add("cancel");
            return result;
        }

        if (args.length == 2
                && sender.hasPermission(ADMIN_PERMISSION)
                && ("list".equalsIgnoreCase(args[0]) || "unlist".equalsIgnoreCase(args[0]))) {
            ShopDBCommands delegate = plugin.getUpdaterCommands();
            if (delegate != null) {
                return delegate.completeRegionNames(args[1]);
            }
        }

        return result;
    }
}
