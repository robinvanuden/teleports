package com.varestorm.plugins.teleports.listener;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        // TODO: Do set back logic
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
}
