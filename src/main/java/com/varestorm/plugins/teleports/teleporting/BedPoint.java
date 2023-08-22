package com.varestorm.plugins.teleports.teleporting;

import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;

public record BedPoint(Telepoint telepoint) {

  public boolean isBroken() {
    Block block = this.telepoint.getLocation().getBlock();
    return !(block.getBlockData() instanceof Bed);
  }
}
