package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.client.TeleportClient;
import com.varestorm.plugins.teleports.client.TeleportClientManager;
import com.varestorm.plugins.teleports.world.WorldManager;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record QuickTeleportCommand(TeleportClientManager clientManager) implements CommandExecutor, TabCompleter {

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can teleport!");
      return true;
    }
    TeleportClient client = this.clientManager.get(player);
    switch (command.getName()) {
      case "back" -> {
        client.toBack();
        return true;
      }
      case "home" -> {
        if (args.length > 0 && args[0].equals("set")) {
          client.setHome();
          return true;
        }
        client.toHome();
        return true;
      }
      case "bed" -> {
        client.toBed();
        return true;
      }
      case "anchor" -> {
        client.toAnchor();
        return true;
      }
      case "spawn" -> {
        if (args.length == 0) {
          return client.toSpawn(player.getWorld());
        }
        World world = WorldManager.getWorld(player.getServer(), WorldManager.WorldType.valueOf(args[0].toUpperCase()));
        if (world == null) {
          world = player.getWorld();
        }
        return client.toSpawn(world);
      }
      default -> {
        return false;
      }
    }
  }

  @Override
  public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    List<String> suggestions = new ArrayList<>();
    if (command.getName().equals("spawn")) {
      for (WorldManager.WorldType type : WorldManager.WorldType.values()) {
        suggestions.add(type.name().toLowerCase());
      }
    }
    if (command.getName().equals("home")) {
      suggestions.add("set");
    }
    return suggestions;
  }
}
