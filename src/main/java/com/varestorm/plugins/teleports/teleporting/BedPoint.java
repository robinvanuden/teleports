package com.varestorm.plugins.teleports.teleporting;

import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;

public class BedPoint {

    Telepoint telepoint;

    public BedPoint(Telepoint telepoint) {
        this.telepoint = telepoint;
    }

    public Telepoint getTelepoint() {
        return this.telepoint;
    }

    private Bed getBed() {
        Block block = this.telepoint.getLocation().getBlock();
        if (block.getBlockData() instanceof Bed) {
            return (Bed) block.getBlockData();
        }
        return null;
    }

    public boolean isBroken() {
        return this.getBed() == null;
    }
}
