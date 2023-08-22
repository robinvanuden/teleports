package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record QuickTeleportCommand(
  Application application) implements CommandExecutor, TabCompleter {

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can teleport!");
      return true;
    }
    TeleportClient client = this.application.getClient(player.getUniqueId());
    switch (command.getName()) {
      case "back" -> {
        client.toBack();
        return true;
      }
      case "home" -> {
        if (args.length > 0 && args[0].equals("test")) {
          client.testHome();
          return true;
        }
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
          return this.doTeleportSpawn(player.getWorld(), client);
        }
        World world = this.application.getWorld(Application.WorldType.valueOf(args[0].toUpperCase()));
        if (world == null) {
          world = player.getWorld();
        }
        return this.doTeleportSpawn(world, client);
      }
      default -> {
        return false;
      }
    }
  }

  private boolean doTeleportSpawn(World world, TeleportClient client) {
    Telepoint spawn = Telepoint.create("Spawn", Telepoint.Type.CUSTOM, world.getSpawnLocation(), world);
    if (spawn.isInterdimensional(client.getPlayer())) {
      client.getManager().start(spawn, true);
      return true;
    }
    client.getManager().sendArrivalTitle(spawn);
    return client.getManager().teleport(spawn);
  }

  @Override
  public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    List<String> suggestions = new ArrayList<>();
    if (command.getName().equals("spawn")) {
      for (Application.WorldType type : Application.WorldType.values()) {
        suggestions.add(type.name().toLowerCase());
      }
    }
    if (command.getName().equals("home")) {
      suggestions.add("set");
    }
    return suggestions;
  }
}
