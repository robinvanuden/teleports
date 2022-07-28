package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class QuickTeleportCommand implements CommandExecutor {

   private final Application application;

   public QuickTeleportCommand(Application application) {
      this.application = application;
   }

   @Override
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
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
         case "testbed":
            client.testBed();
            return true;
         case "testanchor":
            client.testAnchor();
            return true;
         case "spawn":
            return this.doTeleportSpawn(player, client);
         default:
            return false;
      }
   }

   private boolean doTeleportSpawn(Player player, TeleportClient client) {
      World world = player.getWorld();
      Telepoint spawn = Telepoint.create("Spawn", Telepoint.Type.CUSTOM, world.getSpawnLocation(), world);
      client.teleportInstant(spawn);
      player.sendTitle((ChatColor.BOLD + Telepoint.getWorldName(world, true)).toUpperCase(), String.format("You've arrived at %s", spawn.getTitle(false)), 0, 40, 10);
      return true;
   }


}
