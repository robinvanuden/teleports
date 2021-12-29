package com.varestorm.plugins.teleports.listener;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Logger;


public class PlayerListener implements Listener {

    final Application application;
    static Logger LOGGER;

    public PlayerListener(Application application) {
        LOGGER = application.getLogger();
        this.application = application;
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

//    @EventHandler
//    public void onRespawnTrigger(PlayerInteractEvent event) {
//        Player player = event.getPlayer();
//        Material material = event.getMaterial();
//        boolean anchor = material.equals(Material.RESPAWN_ANCHOR);
//        if (!anchor) {
//            return;
//        }
//        Block block = event.getClickedBlock();
//        TeleportClient client = this.application.getClient(player);
//        if (block == null) {
//            return;
//        }
//        client.setAnchor(block.getLocation());
//    }

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
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getBlockData() instanceof Bed){
            Bed bed = (Bed) block.getBlockData();
            if (!bed.getPart().equals(Bed.Part.HEAD)) {
                // Switch to top if bottom half is broken
                block = block.getRelative(bed.getFacing());
            }
            this.alertBedBroken(block, player);
        }
    }

    private void alertBedBroken(Block block, Player player){
        for (TeleportClient client : this.application.getClients()){
            if (client.brokeBed(block, player)){
                return;
            }
        }
    }
}
