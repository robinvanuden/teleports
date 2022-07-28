package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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

public class TeleportPlayerCommand implements CommandExecutor, TabCompleter {

   static ChatColor BLUE = ChatColor.BLUE;
   static ChatColor GREY = ChatColor.GRAY;
   static ChatColor GREEN = ChatColor.GREEN;
   static ChatColor AQUA = ChatColor.DARK_AQUA;
   static ChatColor PURPLE = ChatColor.DARK_PURPLE;
   static ChatColor RED = ChatColor.RED;
   static ChatColor ITALIC = ChatColor.ITALIC;
   static ChatColor BOLD = ChatColor.BOLD;
   static ChatColor RESET = ChatColor.RESET;

   final Application application;

   public TeleportPlayerCommand(Application application) {
      this.application = application;
   }

   private void sendMessage(Player player, String message) {
      player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
   }

   protected boolean list(Player player) {
      TeleportClient client = this.application.getClient(player);
      if (!client.hasRequests()) {
         player.sendMessage(RED + "No requests received");
         return true;
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
      return true;
   }

   public boolean request(Player requester, String playerName) {
      Player player = this.application.getServer().getPlayer(playerName);
      if (player == null) {
         this.sendMessage(requester, String.format("%s%sPlayer not found.", RED, ITALIC));
         return true;
      }
      TeleportClient client = this.application.getClient(player);
      if (client.hasRequest(requester.getUniqueId())) {
         this.sendMessage(requester, String.format("%s%sYou already send a request to %s%s.", GREY, ITALIC, Telepoint.getTitle(player.getWorld(), player.getDisplayName()), GREY));
         return true;
      }
      client.addRequest(requester.getUniqueId());
      this.sendMessage(requester, String.format("Request to teleport to %s has been made!", Telepoint.getTitle(player.getWorld(), player.getDisplayName())));
      player.sendMessage(String.format("You received a teleport request from %s.", Telepoint.getTitle(player.getWorld(), player.getDisplayName())));
      player.sendMessage(String.format("To accept the request use %s/tpp accept %s", GREEN, requester.getDisplayName()));
      player.sendMessage(String.format("or deny the request with %s/tpp deny %s%s.", RED, requester.getDisplayName(), RESET));
      return true;
   }

   public boolean process(Player player, String requesterName, boolean accept) {
      Player requester = this.application.getServer().getPlayer(requesterName);
      if (requester == null) {
         this.sendMessage(player, String.format("%s%sPlayer is not online...", RED, ITALIC));
         return true;
      }
      TeleportClient client = this.application.getClient(player);
      if (!client.hasRequest(requester.getUniqueId())) {
         this.sendMessage(player, String.format("%s%s%s did not send a request...", requesterName, RED, ITALIC));
         return true;
      }
      TeleportClient requesterClient = this.application.getClient(requester);
      if (accept) {
         Telepoint telepoint = Telepoint.create(player.getDisplayName(), Telepoint.Type.CUSTOM, player.getLocation(), player.getWorld());
         requesterClient.teleportInstant(telepoint);
         this.sendMessage(requester, String.format("%s%s%s has%s accepted%s your request!", AQUA, player.getDisplayName(), RESET, GREEN, RESET));
      } else {
         this.sendMessage(requester, String.format("%s%s%s has%s denied%s your request!", AQUA, player.getDisplayName(), RESET, RED, RESET));
      }
      client.removeRequest(requester.getUniqueId());
      return true;
   }

   @Override
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
      if (sender instanceof Player) {
         Player player = (Player) sender;

         if (args.length > 0) {
            if ("list".equals(args[0])) {
               return this.list(player);
            }
         }
         if (args.length > 1) {
            switch (args[0]) {
               case "request":
                  return this.request(player, args[1]);
               case "accept":
               case "deny":
                  return this.process(player, args[1], args[0].equals("accept"));
            }
         }
      }
      return false;
   }

   @Override
   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
      List<String> suggestions = new ArrayList<>();
      List<String> commands = new ArrayList<>();
      if (sender instanceof Player) {
         Player player = (Player) sender;
         commands.add("list");
         commands.add("request");
         commands.add("accept");
         commands.add("deny");

         if (args.length > 0) {
            if (args[0].equals("")) {
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
               case "request":
                  for (Player p : this.application.getServer().getOnlinePlayers()) {
                     if (p.getUniqueId() != player.getUniqueId()) {
                        players.add(p.getDisplayName());
                     }
                  }
                  if (args[1].equals("")) {
                     suggestions.addAll(players);
                  } else {
                     for (String c : players) {
                        if (c.startsWith(args[1])) {
                           suggestions.add(c);
                        }
                     }
                  }
                  break;
               case "accept":
               case "deny":
                  TeleportClient client = this.application.getClient(player);
                  for (UUID uuid : client.getRequests()) {
                     OfflinePlayer p = this.application.getServer().getOfflinePlayer(uuid);
                     players.add(p.getName());
                  }
                  if (args[1].equals("")) {
                     suggestions.addAll(players);
                  } else {
                     for (String c : players) {
                        if (c.startsWith(args[1])) {
                           suggestions.add(c);
                        }
                     }
                  }
                  break;
            }
         }
         if (args.length > 2) {
            suggestions.clear();
         }
      }
      return suggestions;
   }
}
