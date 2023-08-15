package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
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

public record TeleportPlayerCommand(Application application) implements CommandExecutor, TabCompleter {

   static ChatColor BLUE = ChatColor.BLUE;
   static ChatColor GREY = ChatColor.GRAY;
   static ChatColor GREEN = ChatColor.GREEN;
   static ChatColor AQUA = ChatColor.DARK_AQUA;
   static ChatColor PURPLE = ChatColor.DARK_PURPLE;
   static ChatColor RED = ChatColor.RED;
   static ChatColor ITALIC = ChatColor.ITALIC;
   static ChatColor BOLD = ChatColor.BOLD;
   static ChatColor RESET = ChatColor.RESET;

   private void sendMessage(Player player, String message) {
      player.sendActionBar(Component.text(message));
   }

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
         this.sendMessage(sender, String.format("%s%sPlayer not found.", RED, ITALIC));
         return;
      }
      TeleportClient client = this.application.getClient(receiver);
      if (client.hasRequest(sender.getUniqueId())) {
         this.sendMessage(sender, String.format("%s%sYou already send a request to %s%s.", GREY, ITALIC, Telepoint.getTitle(receiver.getWorld(), receiver.getName()), GREY));
         return;
      }
      client.addRequest(sender.getUniqueId());
      sender.sendMessage(String.format("Teleport request to %s has been made!", Telepoint.getTitle(receiver.getWorld(), receiver.getName())));

      receiver.sendMessage(String.format("%s%sTELEPORT REQUEST!", BLUE, BOLD));
      receiver.sendMessage(String.format("%s wants to teleport to your location.", Telepoint.getTitle(sender.getWorld(), sender.getName())));
      receiver.sendMessage(String.format("To accept this request use %s/tpp accept %s", GREEN, sender.getName()));
      receiver.sendMessage(String.format("or deny with %s/tpp deny %s%s.", RED, sender.getName(), RESET));
   }

   public void process(Player player, String requesterName, boolean accept) {
      Player requester = this.application.getServer().getPlayer(requesterName);
      if (requester == null) {
         this.sendMessage(player, String.format("%s%sPlayer is not online...", RED, ITALIC));
         return;
      }
      TeleportClient client = this.application.getClient(player);
      if (!client.hasRequest(requester.getUniqueId())) {
         this.sendMessage(player, String.format("%s%s%s did not send a request...", requesterName, RED, ITALIC));
         return;
      }
      TeleportClient requesterClient = this.application.getClient(requester);
      if (accept) {
         Telepoint telepoint = Telepoint.create(player.getName(), Telepoint.Type.CUSTOM, player.getLocation(), player.getWorld());
         requesterClient.getManager().teleport(telepoint);
         requesterClient.getManager().sendArrivalTitle(telepoint, false);
         this.sendMessage(requester, String.format("%s%s%s has%s accepted%s your request!", AQUA, player.getName(), RESET, GREEN, RESET));
      } else {
         this.sendMessage(requester, String.format("%s%s%s has%s denied%s your request!", AQUA, player.getName(), RESET, RED, RESET));
      }
      client.removeRequest(requester.getUniqueId());
   }

   @Override
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
      if (!(sender instanceof Player player)) {
         return false;
      }
      if (label.equals("tpa")) {
         player.sendMessage(String.format("%s%sATTENTION!", RED, BOLD));
         player.sendMessage(String.format("The %s/tpa%s command will be removed. Use %s/tpp%s instead.", RED, RESET, BLUE, RESET));
         player.sendMessage(String.format("%s(Teleport Player -> tpp, makes more sense.)", ITALIC));
         player.sendMessage(String.format("%s%sStill works the same, only starts different.", GREY, ITALIC));
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
   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
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
