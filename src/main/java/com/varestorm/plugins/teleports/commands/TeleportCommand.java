package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public record TeleportCommand(
  Application application) implements CommandExecutor, TabCompleter, Listener {


  private final static ChatColor BLUE = ChatColor.BLUE;
  private final static ChatColor GOLD = ChatColor.GOLD;
  private final static ChatColor MAGENTA = ChatColor.LIGHT_PURPLE;
  private final static ChatColor GREEN = ChatColor.GREEN;
  private final static ChatColor RED = ChatColor.RED;
  private final static ChatColor BOLD = ChatColor.BOLD;
  private final static ChatColor RESET = ChatColor.RESET;

  public static Map<UUID, Integer> areTeleporting;

  public TeleportCommand(Application application) {
    this.application = application;
    TeleportCommand.areTeleporting = new HashMap<>();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can teleport!");
      return true;
    }
    if (args.length == 0) {
      player.sendMessage(RED + "Unknown command. Try /tp help");
      return true;
    }
    TeleportClient client = this.application.getClient(player.getUniqueId());
    if (client == null) {
      System.out.println("TeleportClient is null");
      sender.sendMessage(RED + "Something went wrong!");
      return true;
    }
    switch (args[0]) {
      case "list" -> {
        client.getList();
        return true;
      }
      case "set" -> {
        client.set(args[1]);
        return true;
      }
      case "to" -> {
        client.to(args[1]);
        return true;
      }
      case "test" -> {
        if (!player.isOp()) {
          return true;
        }
        client.test(args[1]);
        return true;
      }
      case "remove" -> {
        return client.remove(args[1]);
      }
      case "help" -> {
        this.showHelp(player);
        return true;
      }
      default -> {
        player.sendMessage(RED + "Unknown command. Try /tp help");
        return true;
      }
    }
  }

  void showHelp(Player player) {
    player.sendMessage(String.format("%s%sTeleport help", BLUE, BOLD).toUpperCase());
    player.sendMessage("------------------------------");
    player.sendMessage(String.format("%s%sHow to use:", GOLD, BOLD));
    player.sendMessage(String.format("Use %s/tp set example%s to create a teleport named%s example%s.", GREEN, RESET, GREEN, RESET));
    player.sendMessage(String.format("- The location%s where you are standing%s will be used.", BLUE, RESET));
    player.sendMessage(String.format("Use %s/tp to example%s to teleport to%s example%s.", MAGENTA, RESET, MAGENTA, RESET));
    player.sendMessage(String.format("Use %s/tp remove example%s to remove%s example%s.", RED, RESET, RED, RESET));
    player.sendMessage("");
    player.sendMessage(String.format("%s%sQuick teleports:", GOLD, BOLD));
    player.sendMessage(String.format("Use %s/back%s to teleport back to the previous location.", GOLD, RESET));
    player.sendMessage(String.format("- Also works after you%s died%s and want to go back to your stuff.", RED, RESET));
    player.sendMessage(String.format("Use %s/home set%s to set a%s home-point%s.", BLUE, RESET, BLUE, RESET));
    player.sendMessage(String.format("Use %s/home%s to teleport to your%s home-point%s.", BLUE, RESET, BLUE, RESET));
    player.sendMessage(String.format("Use %s/bed%s to teleport to your bed.", GOLD, RESET));
    player.sendMessage(String.format("Use %s/anchor%s to teleport to your%s respawn anchor%s.", MAGENTA, RESET, MAGENTA, RESET));
    player.sendMessage(String.format("- Your%s respawn anchor%s needs to be%s charged%s!", MAGENTA, RESET, GOLD, RESET));
    player.sendMessage("");
    player.sendMessage(String.format("%s%sPlayer teleports:", GOLD, BOLD));
    player.sendMessage(String.format("Use %s/tpp request player%s to request a teleport to%s player%s.", MAGENTA, RESET, MAGENTA, RESET));
    player.sendMessage(String.format("Use %s/tpp accept other_player%s to accept the request.", GREEN, RESET));
    player.sendMessage(String.format("Use %s/tpp deny other_player%s to deny the request.", RED, RESET));
    player.sendMessage("");
    player.sendMessage(String.format("%s%sLists:", GOLD, BOLD));
    player.sendMessage(String.format("Use %s/tp list%s gives you a list of all your teleports.", BLUE, RESET));
    player.sendMessage(String.format("Use %s/tpp list%s gives you a list pending teleport requests.", BLUE, RESET));
  }

  @EventHandler
  public void onPlayerMoved(PlayerMoveEvent event) {
    TeleportClient client = this.application.getClient(event.getPlayer().getUniqueId());
    if (client != null) {
      client.getManager().cancel();
    }
  }

  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
    List<String> suggestions = new ArrayList<>();
    List<String> commands = new ArrayList<>();
    if (!(sender instanceof Player player)) {
      return suggestions;
    }
    commands.add("to");
    commands.add("set");
    commands.add("remove");
    commands.add("list");
    if (player.isOp()) {
      commands.add("test");
    }

    if (args.length == 1) {
      if (args[0].isEmpty()) {
        suggestions.addAll(commands);
      } else {
        for (String c : commands) {
          if (c.startsWith(args[0])) {
            suggestions.add(c);
          }
        }
      }
      return suggestions;
    }
    if (args.length == 2) {
      switch (args[0]) {
        case "test", "to", "remove" -> {
          List<String> names = this.application.getClient(player.getUniqueId()).names();
          if (args[1].isEmpty()) {
            suggestions.addAll(names);
          } else {
            for (String n : names) {
              if (n.startsWith(args[1])) {
                suggestions.add(n);
              }
            }
          }
        }
      }
      return suggestions;
    }
    return suggestions;
  }
}
