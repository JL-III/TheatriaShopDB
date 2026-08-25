package com.playtheatria.shopdb.commands;

import com.playtheatria.shopdb.ShopDBPlugin;
import com.playtheatria.shopdb.updater.ShopDBEditCommands;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /shopdbedit — delegates to the updater's edit handler, with tab completion. */
public class ShopDBEditRootCommand implements CommandExecutor, TabCompleter {
    private final ShopDBPlugin plugin;

    public ShopDBEditRootCommand(ShopDBPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        ShopDBEditCommands delegate = plugin.getEditCommands();
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
            if ("delete".startsWith(args[0].toLowerCase(Locale.ROOT))) result.add("delete");
            return result;
        }

        // Suggest the sender's current block coordinates for x/y/z, vanilla-style.
        if (args.length >= 2 && args.length <= 4 && "delete".equalsIgnoreCase(args[0]) && sender instanceof Player player) {
            Location loc = player.getLocation();
            int coordinate = switch (args.length) {
                case 2 -> loc.getBlockX();
                case 3 -> loc.getBlockY();
                default -> loc.getBlockZ();
            };
            result.add(String.valueOf(coordinate));
        }

        return result;
    }
}
