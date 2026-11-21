package com.varestorm.plugins.teleports.world;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class WorldManager {

  public enum WorldType {OVERWORLD, NETHER, END}

  @Nullable
  public static World getWorld(Server server, String name) {
    return server.getWorld(name);
  }

  @Nullable
  public static World getWorld(Server server, World.Environment environment) {
    for (World world : server.getWorlds()) {
      if (world.getEnvironment().equals(environment)) {
        return world;
      }
    }
    return null;
  }

  @Nullable
  public static World getWorld(Server server, WorldType type) {
    return switch (type) {
      case OVERWORLD -> getWorld(server, World.Environment.NORMAL);
      case NETHER -> getWorld(server, World.Environment.NETHER);
      case END -> getWorld(server, World.Environment.THE_END);
    };
  }

  public static NamedTextColor getWorldColor(World.Environment environment) {
    return switch (environment) {
      case NETHER -> GOLD;
      case THE_END -> DARK_PURPLE;
      case NORMAL -> BLUE;
      default -> GREEN;
    };
  }

  public static NamedTextColor getWorldColor(WorldType type) {
    return switch (type) {
      case NETHER -> GOLD;
      case END -> DARK_PURPLE;
      case OVERWORLD -> BLUE;
    };
  }

  public static NamedTextColor getWorldColor(World world) {
    return getWorldColor(world.getEnvironment());
  }

  public static Component getWorldName(World world) {
    String name = switch (world.getEnvironment()) {
      case NORMAL -> "The Overworld";
      case NETHER -> "The Nether Regions";
      case THE_END -> "The End";
      default -> "The Unknown";
    };
    return Component.text(name.toUpperCase(), getWorldColor(world.getEnvironment()), BOLD);
  }
}
