package com.playtheatria.shopdb.updater;

import com.playtheatria.shopdb.models.EventType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /shopdbedit delete <x> <y> <z> — ShopDB-Updater's ShopDBEditCommands. */
public class ShopDBEditCommands implements CommandExecutor {
    private final EventBuffer buffer;

    public ShopDBEditCommands(EventBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!commandSender.hasPermission("theatria.shopdb.admin")) return false;
        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("delete")) {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                World world = Bukkit.getWorld("The_Ark");
                if (world == null) return false;
                try {
                    Location location = new Location(world, x, y, z);
                    BufferedShopEvent shopEvent = new BufferedShopEvent();
                    shopEvent.id = IDGenerator.generateID(location);
                    shopEvent.eventType = EventType.DELETE;
                    buffer.createOrUpdate(shopEvent);
                    return true;
                } catch (NullPointerException | NumberFormatException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        return false;
    }
}
