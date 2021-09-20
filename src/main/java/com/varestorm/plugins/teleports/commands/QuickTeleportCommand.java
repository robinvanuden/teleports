package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Teleport;
import com.varestorm.plugins.teleports.TeleportList;
import com.varestorm.plugins.teleports.Worlds;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuickTeleportCommand implements CommandExecutor {

    static final ChatColor RED = ChatColor.RED;
    static final ChatColor RESET = ChatColor.RESET;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            System.out.println(command.getName());
            switch (command.getName()) {
                case "back":
                    return this.back(player);
                case "spawn":
                    return this.spawn(player);
                case "bed":
                    return this.bed(player);
                case "anchor":
                    return this.anchor(player);
                case "home":
                    return this.home(player);
                case "sethome":
                    return this.setHome(player);
            }
            return true;
        }
        return false;
    }

    protected boolean setHome(Player player) {
        Teleport.getList(player.getUniqueId()).setHome(player.getLocation());
        player.sendMessage(Teleport.getName("Home", player.getWorld()) + " set");
        return true;
    }

    protected boolean home(Player player) {
        TeleportList list = Teleport.getList(player.getUniqueId());
        if (!list.hasHome()) {
            player.sendMessage("You haven't set a home yet.");
        }
        Teleport.instant(player, "home", list.getHome());
        return true;
    }

    protected boolean spawn(Player player) {
        Teleport.instant(player, "the world spawn", player.getWorld().getSpawnLocation());
        return true;
    }

    protected boolean back(Player player) {
        TeleportList list = Teleport.getList(player.getUniqueId());
        Teleport.instant(player, "last location", Worlds.getFreeLocation(list.getBack()));
        return true;
    }

    protected boolean bed(Player player) {
        TeleportList list = Teleport.getList(player.getUniqueId());
        Location bed = list.getBed();
        if (bed == null) {
            player.sendMessage("Your bed is missing or obstructed.");
            return true;
        }
        String name = bed.getWorld() != null ? Teleport.getName("bed", bed.getWorld()) : "bed";
        if (!bed.getBlock().getBlockData().getMaterial().name().contains("_BED")) {
            player.sendMessage(String.format("Your %s is missing or obstructed.", name));
            return true;
        }
        Location spawn = Worlds.getFreeLocation(bed);
        Location bed2 = list.getOtherBedBlock();
        if (spawn == null) {
            spawn = Worlds.getFreeLocation(bed2);
        } else {
            bed = bed2;
        }
        if (spawn == null) {
            player.sendMessage(String.format("Your %s is missing or obstructed.", name));
            return true;
        }
        Teleport.instant(player, "your bed", Worlds.lookAt(spawn, bed));
        return true;
    }

    protected boolean anchor(Player player) {
        TeleportList list = Teleport.getList(player.getUniqueId());
        Location anchor = list.getAnchor();
        if (anchor == null) {
            player.sendMessage("Your respawn anchor is missing or obstructed.");
            return true;
        }
        String name = anchor.getWorld() != null ? Teleport.getName("respawn anchor", anchor.getWorld()) : "respawn anchor";
        if (anchor.getBlock().getBlockData().getMaterial() != Material.RESPAWN_ANCHOR) {
            player.sendMessage(String.format("Your %s is missing or obstructed.", name));
            return true;
        }
        if (!list.isAnchorCharged()) {
            player.sendMessage(String.format("Your %s has %sno charges%s left to teleport you.", name, RED, RESET));
            return true;
        }
        Location spawn = Worlds.getFreeLocation(anchor);
        if (spawn == null) {
            player.sendMessage(String.format("Your %s is missing or obstructed.", name));
            return true;
        }
        Teleport.instant(player, "your respawn anchor", Worlds.lookAt(spawn, anchor));
        int charges = list.getAnchorCharges();
        if (charges < 2) {
            player.sendMessage(String.format("%sWarning!%s Your %s has only %s%s%s charge%s left.", RED, RESET, name, RED, charges, RESET, charges == 1 ? "" : "s"));
        }
        return true;
    }

}
