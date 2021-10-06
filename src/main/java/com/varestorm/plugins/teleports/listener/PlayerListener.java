package com.varestorm.plugins.teleports.listener;

import com.varestorm.plugins.teleports.Application;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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

    @EventHandler
    public void onRespawnSet(PlayerInteractEvent event) {
        // TODO: Do set respawn anchor logic
    }

    @EventHandler
    public void onPlayerMoved(PlayerMoveEvent event) {
        // TODO: Do cooldown reset / cancel teleport logic
    }

}
