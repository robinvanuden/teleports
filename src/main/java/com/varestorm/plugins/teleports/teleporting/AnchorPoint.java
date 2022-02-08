package com.varestorm.plugins.teleports.teleporting;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.Teleport;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Player;

public class AnchorPoint {

    Telepoint telepoint;
    Player player;

    public AnchorPoint(Telepoint telepoint) {
        this.telepoint = telepoint;
    }

    private RespawnAnchor getAnchor() {
        Block block = this.telepoint.getLocation().getBlock();
        if (block.getBlockData() instanceof RespawnAnchor) {
            return (RespawnAnchor) block.getBlockData();
        }
        return null;
    }

    public boolean isBroken() {
        return this.getAnchor() == null;
    }

    public Telepoint getTelepoint() {
        return this.telepoint;
    }

    public int getCharges() {
        RespawnAnchor anchor = this.getAnchor();
        if (anchor == null) {
            return 0;
        }
        return anchor.getCharges();
    }

    public boolean hasNoCharges() {
        return this.getCharges() == 0;
    }

    public void removeCharge() {
        Location location = this.telepoint.getLocation();
        Block block = location.getBlock();
        if (!(block.getBlockData() instanceof RespawnAnchor)) {
            return;
        }
        Teleport.smoke(this.telepoint.getSafeLocation(), Color.fromRGB(255, 170, 0));
        RespawnAnchor anchor = ((RespawnAnchor) block.getBlockData());
        int newCharge = anchor.getCharges() - 1;
        anchor.setCharges(newCharge);
        block.setBlockData(anchor);
        block.getState().update(true);
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Application.runLaterAsync(() -> world.playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1, 1), 10);
    }

}
