package com.varestorm.plugins.teleports.listener;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.Teleport;
import com.varestorm.plugins.teleports.TeleportList;
import com.varestorm.plugins.teleports.commands.TeleportCommand;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
        this.application.setList(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (this.application.saveFile(player, Teleport.getList(player.getUniqueId()).toJSON())) {
            LOGGER.warning("Error while saving teleports of player P#" + player.getUniqueId());
        }
    }

    @EventHandler
    public void onDeathPlayer(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Teleport.getList(player.getUniqueId()).setBack(player.getLocation());
    }

    @EventHandler
    public void onRespawnSet(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.hasBlock() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null) {
                BlockData data = block.getBlockData();
                Material material = data.getMaterial();
                if (material.isBlock() && material == Material.RESPAWN_ANCHOR) {
                    if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
                        if (data instanceof RespawnAnchor) {
                            RespawnAnchor anchor = (RespawnAnchor) data;
                            if (anchor.getCharges() > 0) {
                                TeleportList list = Teleport.getList(player.getUniqueId());
                                Location block_location = block.getLocation();
                                if (!list.isYourAnchor(block_location)) {
                                    list.setAnchor(block_location);
                                    player.sendMessage(Teleport.getName("Anchor point", block.getWorld()) + " set");
                                } else {
                                    if (list.canClickAnchor()) {
                                        if (player.getInventory().getItemInMainHand().getType() != Material.GLOWSTONE) {
                                            String name = Teleport.getName("respawn anchor", player.getWorld());
                                            player.sendMessage(String.format("This %s has %sno%s charges left.", name, ChatColor.RED, ChatColor.RESET));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (material.isBlock() && material.name().contains("_BED")) {
                    if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                        Location block_location = block.getLocation();
                        if (!Teleport.getList(player.getUniqueId()).isYourBed(block_location)) {
                            Teleport.getList(player.getUniqueId()).setBed(block_location);
                            player.sendMessage(Teleport.getName("Respawn point", block.getWorld()) + " set");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMoved(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        TeleportCommand.list.remove(player.getUniqueId());
    }

}
