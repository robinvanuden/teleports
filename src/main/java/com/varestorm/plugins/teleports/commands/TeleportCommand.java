package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import net.kyori.adventure.text.Component;
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

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public record TeleportCommand(
  Application application) implements CommandExecutor, TabCompleter, Listener {

  public static Map<UUID, Integer> areTeleporting;

  public TeleportCommand(Application application) {
    this.application = application;
    TeleportCommand.areTeleporting = new HashMap<>();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can teleport!");
      return true;
    }
    if (args.length == 0) {
      player.sendMessage(Component.text("Unknown command. Try /tp help", RED));
      return true;
    }
    TeleportClient client = this.application.getClient(player.getUniqueId());
    if (client == null) {
      System.out.println("TeleportClient is null");
      sender.sendMessage(Component.text("Something went wrong!", RED));
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
        if (args.length > 1) {
          client.to(args[1], false);
          return true;
        } else {
          return false;
        }
      }
      case "force" -> {
        client.to(args[1], true);
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
        player.sendMessage(Component.text("Unknown command. Try /tp help", RED));
        return true;
      }
    }
  }

  void showHelp(Player player) {
    player.sendMessage(Component.text("TELEPORT HELP", BLUE, BOLD));
    player.sendMessage("------------------------------");
    // Base commands
    player.sendMessage(Component.text("Teleportation", GOLD, BOLD));
    // /tp set
    player.sendMessage(Component.text("Use ")
      .append(TeleportCommandTexts.teleportSet())
      .append(Component.text(" to create a teleport named "))
      .append(Component.text("example", GREEN))
      .append(Component.text(" based on your location."))
      .appendNewline()
      // /tp to
      .append(Component.text("Use "))
      .append(TeleportCommandTexts.teleportTo())
      .append(Component.text(" to teleport to "))
      .append(Component.text("example", LIGHT_PURPLE))
      .append(Component.text("."))
      .appendNewline()
      // /tp remove
      .append(Component.text("Use "))
      .append(TeleportCommandTexts.teleportRemove())
      .append(Component.text(" to remove "))
      .append(Component.text("example", RED))
      .append(Component.text("."))
      .appendNewline()
      // /tp list
      .append(Component.text("Use "))
      .append(TeleportCommandTexts.teleportList())
      .append(Component.text(" to show all saved teleports ."))
      .appendNewline()
      .appendNewline()
    );
    // Quick commands
    player.sendMessage(Component.text("Quick teleports", GOLD, BOLD));
    // /tp set
    player.sendMessage(Component.text("Use ")
      .append(TeleportCommandTexts.back())
      .append(Component.text(" to teleport back to the previous location."))
      .appendNewline()
      .append(Component.text("- Also works after you died and want to go back to your stuff.", GRAY, ITALIC))
      .appendNewline()
      // /home set
      .append(Component.text("Use "))
      .append(TeleportCommandTexts.homeSet())
      .append(Component.text(" to set a teleport that brings you home."))
      .appendNewline()
      // /home
      .append(Component.text("Use "))
      .append(TeleportCommandTexts.home())
      .append(Component.text(" to instantly go home."))
      .appendNewline()
      // /bed
      .append(Component.text("Use "))
      .append(TeleportCommandTexts.bed())
      .append(Component.text(" to instantly go to your bed."))
      .appendNewline()
      .append(Component.text("- Only works if you have slept in a bed.", GRAY, ITALIC))
      .appendNewline()
      // /anchor
      .append(Component.text("Use "))
      .append(TeleportCommandTexts.anchor())
      .append(Component.text(" to instantly go to your Respawn Anchor."))
      .appendNewline()
      .append(Component.text("- Only works if the anchor is charged.", GRAY, ITALIC))
      .appendNewline()
      .appendNewline()
    );
    // Player teleports
    player.sendMessage(Component.text("Player teleports", GOLD, BOLD));
    // /tp set
    player.sendMessage(Component.text("Use ")
      .append(TeleportCommandTexts.teleportPlayerRequest())
      .append(Component.text(" to request teleports to other players."))
      .appendNewline()
      // /tpp accept
      .append(Component.text("Use "))
      .append(TeleportCommandTexts.teleportPlayerAccept())
      .append(Component.text(" to accept the request."))
      .appendNewline()
      // /tpp deny
      .append(Component.text("Use "))
      .append(TeleportCommandTexts.teleportPlayerDeny())
      .append(Component.text(" to deny the request."))
      .appendNewline()
      // /tpp list
      .append(Component.text("Use "))
      .append(TeleportCommandTexts.teleportPlayerList())
      .append(Component.text(" to show all requests from send to you."))
    );
  }

  @EventHandler
  public void onPlayerMoved(PlayerMoveEvent event) {
    TeleportClient client = this.application.getClient(event.getPlayer().getUniqueId());
    if (client != null) {
      client.getManager().cancel();
    }
  }

  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
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

  public static class TeleportCommandTexts {

    public static Component homeSet() {
      return Component.text("/home set", GREEN);
    }

    public static Component home() {
      return Component.text("/home", BLUE);
    }

    public static Component bed() {
      return Component.text("/bed", DARK_RED);
    }

    public static Component anchor() {
      return Component.text("/anchor", DARK_PURPLE);
    }

    public static Component teleportList() {
      return Component.text("/tp list", BLUE);
    }

    public static Component teleportSet() {
      return Component.text("/tp set [example]", GREEN);
    }

    public static Component teleportTo() {
      return Component.text("/tp to [example]", LIGHT_PURPLE);
    }

    public static Component teleportRemove() {
      return Component.text("/tp remove [example]", RED);
    }

    public static Component back() {
      return Component.text("/back", GOLD);
    }

    public static Component teleportPlayerList() {
      return Component.text("/tpp list", BLUE);
    }

    public static Component teleportPlayerRequest() {
      return Component.text("/tpp request [player]", LIGHT_PURPLE);
    }

    public static Component teleportPlayerAccept() {
      return teleportPlayerAccept("[player]");
    }

    public static Component teleportPlayerAccept(String name) {
      return Component.text("/tpp accept " + name, GREEN);
    }

    public static Component teleportPlayerDeny() {
      return teleportPlayerDeny("[player]");
    }

    public static Component teleportPlayerDeny(String name) {
      return Component.text("/tpp accept " + name, RED);
    }

  }
}
