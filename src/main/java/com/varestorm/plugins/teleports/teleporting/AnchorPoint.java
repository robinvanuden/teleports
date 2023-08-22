package com.varestorm.plugins.teleports.teleporting;

import com.varestorm.plugins.teleports.ParticleManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.RespawnAnchor;

public record AnchorPoint(Telepoint telepoint) {

  private RespawnAnchor getAnchor() {
    Block block = this.getLocation().getBlock();
    BlockData data = block.getBlockData();
    if (data instanceof RespawnAnchor) {
      return (RespawnAnchor) data;
    }
    return null;
  }

  public boolean isBroken() {
    return this.getAnchor() == null;
  }

  public Telepoint getTelepoint() {
    return this.telepoint;
  }

  public Location getLocation() {
    return this.getTelepoint().getLocation();
  }

  public int getCharges() {
    RespawnAnchor anchor = this.getAnchor();
    if (anchor == null) {
      return 0;
    }
    return anchor.getCharges();
  }

  public int getMaximumCharges() {
    RespawnAnchor anchor = this.getAnchor();
    if (anchor == null) {
      return 0;
    }
    return anchor.getMaximumCharges();
  }

  public boolean hasNoCharges() {
    return this.getCharges() == 0;
  }

  private Block checkForAnchor() {
    if (this.getAnchor() == null) {
      return null;
    }
    return this.getLocation().getBlock();
  }

  public void removeCharge() {
    Block block = this.checkForAnchor();
    RespawnAnchor anchor = this.getAnchor();
    if (block == null || anchor == null) {
      return;
    }
    int newCharge = anchor.getCharges() - 1;
    anchor.setCharges(newCharge);
    block.setBlockData(anchor);
    block.getState().update(true);
    block.getWorld().playSound(block.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1, 1);
  }

  public void resetCharges() {
    Block block = this.checkForAnchor();
    RespawnAnchor anchor = this.getAnchor();
    if (block == null || anchor == null) {
      return;
    }
    anchor.setCharges(anchor.getMaximumCharges());
    block.setBlockData(anchor);
    block.getState().update(true);
    ParticleManager.highlightBlock(block, Color.YELLOW);
    block.getWorld().playSound(block.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1, 1);
  }

  public void newAnchorEffect() {
    int count = (this.getCharges() * 100) + 1;
    Location location = this.getLocation();
    World world = location.getWorld();
    if (world == null) {
      return;
    }
    Block block = world.getBlockAt(location).getRelative(BlockFace.UP);
    Location particleLoc = block.getLocation().clone();
    world.spawnParticle(Particle.ENCHANTMENT_TABLE, particleLoc.add(0.5, 1, 0.5), count);
  }

}
