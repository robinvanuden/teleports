package com.varestorm.plugins.teleports.teleporting;

import com.varestorm.plugins.teleports.Teleport;
import org.bukkit.Color;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;

public class AnchorPoint {

    Telepoint telepoint;

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

    public Telepoint getTelepoint() {
        return this.telepoint;
    }

    public boolean hasCharges() {
        RespawnAnchor anchor = this.getAnchor();
        if (anchor == null) {
            return false;
        }
        return anchor.getCharges() > 0;
    }

    public void removeCharge() {
        Block block = this.telepoint.getLocation().getBlock();
        if (!(block.getBlockData() instanceof RespawnAnchor)) {
            return;
        }
//        RespawnAnchor anchor = (RespawnAnchor) block.getBlockData();
//        anchor.setCharges(0);
        Teleport.smoke(this.telepoint.getSafeLocation(), Color.YELLOW);
        ((RespawnAnchor) block.getBlockData()).setCharges(0);
        block.getState().update(true);
    }

}
