package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuickTeleportCommand implements CommandExecutor {

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
        TeleportClient client = this.application.getClient(player.getUniqueId());
        switch (command.getName()) {
            case "back":
                client.toBack();
                return true;
            case "home":
            case "bed":
            case "anchor":
                client.instant(command.getName());
                return true;
            case "sethome":
                client.setHome();
                return true;
            case "testhome":
                client.testHome();
                return true;
            case "spawn":
                player.sendMessage("This command is temporarily not available...");
                return true;
            default:
                return false;
        }
    }


}
