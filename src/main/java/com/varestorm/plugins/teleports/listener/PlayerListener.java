package com.varestorm.plugins.teleports.listener;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.logging.Logger;


public record PlayerListener(Application application) implements Listener {

   static Logger LOGGER;

   public PlayerListener {
      LOGGER = application.getLogger();
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      this.application.setClient(player);
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      this.application.getClient(player.getUniqueId()).saveTelepoints();
   }

   @EventHandler
   public void onDeathPlayer(PlayerDeathEvent event) {
      Player player = event.getEntity();
      TeleportClient client = this.application.getClient(player);
      client.setBack(player.getLocation());
   }

   @EventHandler
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      Player player = event.getPlayer();
      TeleportClient client = this.application.getClient(player);
      client.setBack(player.getLocation());
   }

   @EventHandler
   public void onDamagePlayer(EntityDamageEvent event) {
      if (!(event.getEntity() instanceof Player player)) {
         return;
      }
      boolean valid = switch (event.getCause()) {
         case DROWNING, FIRE, WITHER, SUFFOCATION, FIRE_TICK, BLOCK_EXPLOSION, VOID, CRAMMING, CUSTOM, FLY_INTO_WALL, LAVA, ENTITY_EXPLOSION -> true;
         default -> false;
      };
      if (!valid) {
         return;
      }
      TeleportClient client = this.application.getClient(player);
      client.getManager().resetCooldown();
   }

   @EventHandler
   public void onRespawnTrigger(PlayerInteractEvent event) {
      if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
         return;
      }
      Block block = event.getClickedBlock();
      if (block == null) {
         return;
      }
      Material material = block.getType();
      if (!material.equals(Material.RESPAWN_ANCHOR)) {
         return;
      }
      Player player = event.getPlayer();
      TeleportClient client = this.application.getClient(player);
      client.setAnchor(block.getLocation());
   }

   @EventHandler
   public void onBedSet(PlayerBedEnterEvent event) {
      Player player = event.getPlayer();
      TeleportClient client = this.application.getClient(player);
      if (client.setBed(event.getBed().getLocation())) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onBreak(BlockBreakEvent event) {
      Block block = event.getBlock();
      Material material = block.getType();
      Player player = event.getPlayer();
      if (material.equals(Material.RESPAWN_ANCHOR) && block.getBlockData() instanceof RespawnAnchor) {
         this.alertAnchorBroken(block, player);
         return;
      }
      if (!material.name().endsWith("_BED")) {
         return;
      }
      if (!(block.getBlockData() instanceof Bed bed)) {
         return;
      }
      if (!bed.getPart().equals(Bed.Part.HEAD)) {
         // Switch to top if bottom half is broken
         block = block.getRelative(bed.getFacing());
      }
      this.alertBedBroken(block, player);
   }

   private void alertBedBroken(Block block, Player player) {
      for (TeleportClient client : this.application.getClients()) {
         client.brokeBed(block, player);
      }
   }

   private void alertAnchorBroken(Block block, Player player) {
      for (TeleportClient client : this.application.getClients()) {
         client.brokeAnchor(block, player);
      }
   }
}
