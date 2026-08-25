package com.playtheatria.shopdb.commands;

import com.playtheatria.shopdb.ShopDBPlugin;
import com.playtheatria.shopdb.updater.ShopDBCommands;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /shopdb — handles `reload` itself, delegates `list`/`unlist` to the
 * updater's command handler, and provides tab completion for all of it.
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
            for (String option : new String[]{"list", "unlist", "reload"}) {
                if ("reload".equals(option) && !sender.hasPermission(ADMIN_PERMISSION)) continue;
                if (option.startsWith(prefix)) result.add(option);
            }
            return result;
        }

        if (args.length == 2 && ("list".equalsIgnoreCase(args[0]) || "unlist".equalsIgnoreCase(args[0]))) {
            ShopDBCommands delegate = plugin.getUpdaterCommands();
            if (delegate != null) {
                return delegate.completeRegionNames(args[1]);
            }
        }

        return result;
    }
}
