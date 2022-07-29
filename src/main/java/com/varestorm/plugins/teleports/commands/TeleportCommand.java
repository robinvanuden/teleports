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

public record TeleportCommand(Application application) implements CommandExecutor, TabCompleter, Listener {

   static final ChatColor RED = ChatColor.RED;

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
      TeleportClient client = this.application.getClient(player.getUniqueId());
      if (client == null) {
         System.out.println("TeleportClient is null");
         sender.sendMessage(RED + "Something went wrong!");
         return true;
      }
      if (args.length > 0) {
         if (args[0].equals("list")) {
            client.getList();
            return true;
         }
         if (args.length > 1) {
            switch (args[0]) {
               case "set":
                  client.set(args[1]);
                  return true;
               case "to":
                  client.to(args[1]);
                  return true;
               case "test":
                  if (!player.isOp()) {
                     return true;
                  }
                  client.test(args[1]);
                  return true;
               case "remove":
                  return client.remove(args[1]);
               default:
                  player.sendMessage("This command is temporarily not available...");
                  return true;
            }
         }
      }
      return false;
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
         if (args[0].equals("")) {
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
               if (args[1].equals("")) {
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
