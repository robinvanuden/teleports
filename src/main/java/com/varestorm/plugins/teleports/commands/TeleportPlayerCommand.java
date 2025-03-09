package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record TeleportPlayerCommand(
  Application application) implements CommandExecutor, TabCompleter {

  private final static NamedTextColor BLUE = NamedTextColor.BLUE;
  private final static NamedTextColor GREY = NamedTextColor.GRAY;
  private final static NamedTextColor GREEN = NamedTextColor.GREEN;
  private final static NamedTextColor PURPLE = NamedTextColor.DARK_PURPLE;
  private final static NamedTextColor RED = NamedTextColor.RED;
  private final static TextDecoration ITALIC = TextDecoration.ITALIC;
  private final static TextDecoration BOLD = TextDecoration.BOLD;

  private void list(Player player) {
    TeleportClient client = this.application.getClient(player);
    if (!client.hasRequests()) {
      player.sendMessage(RED + "No requests received");
      return;
    }
    player.sendMessage(String.format("%s%sTeleport requests", PURPLE, BOLD));
    player.sendMessage("------------------");
    Server server = this.application.getServer();
    int count = 1;
    for (UUID uuid : client.getRequests()) {
      OfflinePlayer p = server.getOfflinePlayer(uuid);
      String name = p.getName();
      if (!p.isOnline()) {
        name = String.format("%s (offline)", name);
      }
      player.sendMessage(String.format("%d. %s%s", count, p.isOnline() ? BLUE : RED, name));
      count++;
    }
  }

  public void request(Player sender, String playerName) {
    Player receiver = this.application.getServer().getPlayer(playerName);
    if (receiver == null) {
      sender.sendActionBar(Component.text("Player not found.", RED, ITALIC));
      return;
    }
    TeleportClient client = this.application.getClient(receiver);
    if (client.hasRequest(sender.getUniqueId())) {
      Component component = Component.text("You already sent a request to ", GREY, ITALIC).append(Telepoint.getTitle(receiver.getLocation(), receiver.getName()));
      sender.sendActionBar(component);
      return;
    }
    client.addRequest(sender.getUniqueId());
    sender.sendMessage(Component.text("Teleport request to ")
      .append(Telepoint.getTitle(receiver.getLocation(), receiver.getName()))
      .append(Component.text(" has been made!")));

    receiver.sendMessage(Component.text("TELEPORT REQUEST!", BLUE, BOLD));
    receiver.sendMessage(Telepoint.getTitle(sender.getLocation(), sender.getName())
      .append(Component.text(" wants to teleport to your location.")));
    receiver.sendMessage(Component.text("To accept this request use ")
      .append(Component.text("/tpp accept %s.".formatted(sender.getName()), GREEN)));
    receiver.sendMessage(Component.text("or deny with")
      .append(Component.text("/tpp deny %s.".formatted(sender.getName()), RED)));
  }

  public void process(Player player, String requesterName, boolean accept) {
    Player requester = this.application.getServer().getPlayer(requesterName);
    if (requester == null) {
      player.sendActionBar(Component.text("Player is not online...", RED, ITALIC));
      return;
    }
    TeleportClient client = this.application.getClient(player);
    if (!client.hasRequest(requester.getUniqueId())) {
      player.sendActionBar(Component.text("%s did not send a request...".formatted(requesterName), RED, ITALIC));
      return;
    }
    TeleportClient requesterClient = this.application.getClient(requester);
    if (accept) {
      Telepoint telepoint = Telepoint.create(player.getName(), Telepoint.Type.CUSTOM, player.getLocation(), player.getWorld());
      requesterClient.getManager().teleport(telepoint);
      requesterClient.getManager().sendArrivalTitle(telepoint);
      requester.sendActionBar(Component.text("%s has accepted your request!".formatted(player.getName()), GREEN));
    } else {
      requester.sendActionBar(Component.text("%s has denied your request!".formatted(player.getName()), RED));
    }
    client.removeRequest(requester.getUniqueId());
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      return false;
    }
    if (label.equals("tpa")) {
      player.sendMessage(Component.text("ATTENTION!", RED, BOLD));
      player.sendMessage(Component.text("The /tpa command will be removed.", RED)
        .appendSpace()
        .append(Component.text("Use /tpp instead.", BLUE)));
      player.sendMessage(Component.text("(TelePort Player -> tpp, makes more sense.)", GREY, ITALIC));
      player.sendMessage(Component.text("Still works the same, only starts different.", GREY, ITALIC));
    }
    if (args.length > 0) {
      if ("list".equals(args[0])) {
        this.list(player);
        return true;
      }
    }
    if (args.length > 1) {
      switch (args[0]) {
        case "request" -> {
          this.request(player, args[1]);
          return true;
        }
        case "accept", "deny" -> {
          this.process(player, args[1], args[0].equals("accept"));
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
    List<String> suggestions = new ArrayList<>();
    List<String> commands = new ArrayList<>();
    if (!(sender instanceof Player player)) {
      return suggestions;
    }
    commands.add("list");
    commands.add("request");
    commands.add("accept");
    commands.add("deny");

    if (args.length > 0) {
      if (args[0].isEmpty()) {
        suggestions.addAll(commands);
      } else {
        for (String c : commands) {
          if (c.startsWith(args[0])) {
            suggestions.add(c);
          }
        }
      }
    }
    if (args.length > 1) {
      suggestions.clear();
      List<String> players = new ArrayList<>();
      switch (args[0]) {
        case "request" -> {
          for (Player p : this.application.getServer().getOnlinePlayers()) {
            if (p.getUniqueId() != player.getUniqueId()) {
              players.add(p.getName());
            }
          }
          if (args[1].isEmpty()) {
            suggestions.addAll(players);
          } else {
            for (String c : players) {
              if (c.startsWith(args[1])) {
                suggestions.add(c);
              }
            }
          }
        }
        case "accept", "deny" -> {
          TeleportClient client = this.application.getClient(player);
          for (UUID uuid : client.getRequests()) {
            OfflinePlayer p = this.application.getServer().getOfflinePlayer(uuid);
            players.add(p.getName());
          }
          if (args[1].isEmpty()) {
            suggestions.addAll(players);
          } else {
            for (String c : players) {
              if (c.startsWith(args[1])) {
                suggestions.add(c);
              }
            }
          }
        }
      }
    }
    if (args.length > 2) {
      suggestions.clear();
    }
    return suggestions;
  }
}
