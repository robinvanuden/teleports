package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public record QuickTeleportCommand(Application application) implements CommandExecutor {

   @Override
   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("Only players can teleport!");
         return true;
      }
      TeleportClient client = this.application.getClient(player.getUniqueId());
      switch (command.getName()) {
         case "back":
            client.toBack();
            return true;
         case "home":
            client.toHome();
            return true;
         case "bed":
            client.toBed();
            return true;
         case "anchor":
            client.toAnchor();
            return true;
         case "sethome":
            client.setHome();
            return true;
         case "testhome":
            client.testHome();
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
      client.getManager().sendArrivalTitle(spawn);
      return client.getManager().teleport(spawn);
   }


}
