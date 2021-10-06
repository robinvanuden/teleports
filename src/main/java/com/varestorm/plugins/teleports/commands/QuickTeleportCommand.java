package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuickTeleportCommand implements CommandExecutor {

    static final ChatColor RED = ChatColor.RED;
    static final ChatColor RESET = ChatColor.RESET;
    private final Application application;

    public QuickTeleportCommand(Application application) {
        this.application = application;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can teleport!");
            return true;
        }
        Player player = (Player) sender;
        switch (command.getName()) {
            case "back":
            case "home":
            case "bed":
            case "anchor":
                if (!this.application.getClient(player.getUniqueId()).instant(command.getName())) {
                    return true;
                }
                return true;
            case "spawn":
            case "sethome":
                player.sendMessage("This command is temporarily not available...");
                return true;
            default:
                return false;
        }
    }


}
