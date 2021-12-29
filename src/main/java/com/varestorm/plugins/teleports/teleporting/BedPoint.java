package com.varestorm.plugins.teleports.teleporting;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;

public class BedPoint {

    Telepoint telepoint;
    Block block;
    Bed bed;

    public BedPoint(Telepoint telepoint) {
        this.telepoint = telepoint;
        Location location = telepoint.getLocation();
//        this.block = location.getBlock();
//        this.bed = (Bed) this.block;
    }

    public Telepoint getTelepoint() {
        return this.telepoint;
    }
}
