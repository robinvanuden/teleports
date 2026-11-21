package com.varestorm.plugins.teleports.teleporting;

import com.varestorm.plugins.teleports.world.WorldManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;

import java.util.Objects;

import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

public record Telepoint(String name,
                        com.varestorm.plugins.teleports.teleporting.Telepoint.Type type,
                        World world, Location location) {

  public enum Type {BACK, HOME, BED, ANCHOR, CUSTOM}

  private static final BlockFace[] FACES = new BlockFace[]{BlockFace.SELF, BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST};

  public Location getLocation() {
    return this.location.clone();
  }

  public boolean isInterdimensional(Player player) {
    return isInterdimensional(player, this.getEnvironment());
  }

  public static boolean isInterdimensional(Player player, World.Environment target) {
    World world = player.getLocation().getWorld();
    if (world == null) {
      return true;
    }
    return (!target.equals(world.getEnvironment()));
  }

  public String getName() {
    return this.name;
  }

  public Component getTitle() {
    return Component.text(this.name, WorldManager.getWorldColor(this.getEnvironment()));
  }

  private static Component getTitle(World.Environment environment, String name) {
    return Component.text(name, WorldManager.getWorldColor(environment));
  }

  public static Component getTitle(Location location, String name) {
    World world = location.getWorld();
    NamedTextColor color = world == null ? WHITE : WorldManager.getWorldColor(world.getEnvironment());
    return Component.text(name, color);
  }

  public static Component getName(Location location, String name) {
    World world = location.getWorld();
    if (world == null) {
      return Component.text(name);
    }
    return getTitle(world.getEnvironment(), name);
  }

  public NamedTextColor getWorldColor() {
    return WorldManager.getWorldColor(this.getEnvironment());
  }

  public Type getType() {
    return this.type;
  }

  public World getWorld() {
    return this.world;
  }

  public World.Environment getEnvironment() {
    return this.getWorld().getEnvironment();
  }

  public static Type getType(String name) {
    for (Type type : Type.values()) {
      if (type.name().equalsIgnoreCase(name)) {
        return type;
      }
    }
    return null;
  }

  public Location getSafeLocation() {
    Location location = this.getLocation();
    // Check self block
    Location safe = checkSafeLocation(location.getBlock());
    if (safe != null) {
      return safe;
    }
    safe = getSafeLocation(location, BlockFace.UP);
    if (safe != null) {
      return safe;
    }
    return getSafeLocation(location, BlockFace.DOWN);
  }

  private static Location getSafeLocation(Location location, BlockFace checkAfter) {
    Block block = location.getBlock();
    for (BlockFace face : FACES) {
      Block side = block.getRelative(face);
      Location sideLocation = checkSafeLocation(side);
      if (sideLocation != null) {
        return sideLocation;
      }
    }
    if ((checkAfter == BlockFace.UP) && (location.getY() >= Objects.requireNonNull(location.getWorld()).getMaxHeight())) {
      return null;
    }
    if ((checkAfter == BlockFace.DOWN) && (location.getY() <= Objects.requireNonNull(location.getWorld()).getMinHeight())) {
      return null;
    }
    return getSafeLocation(block.getRelative(checkAfter).getLocation(), checkAfter);
  }

  public static Location checkSafeLocation(Block block) {
    Location location = block.getLocation().clone().add(.5, 0, .5);
    // Is slab
    if (block.getBlockData() instanceof Slab slab && slab.getType().equals(Slab.Type.BOTTOM)) {
      return location.add(0, .5, 0);
    }
    // Is trapdoor
    if (block.getBlockData() instanceof TrapDoor) {
      return location;
    }
    // Is stairs
    if (block.getBlockData() instanceof Stairs) {
      return location.add(0, 1, 0);
    }
    if (!block.isEmpty()) {
      return null;
    }
    if (block.isLiquid()) {
      return null;
    }
    if (!blockBelowSafe(block)) {
      return null;
    }
    return location;
  }

  public static boolean blockBelowSafe(Block block) {
    Block below = block.getRelative(BlockFace.DOWN);
    if (below.isLiquid()) {
      return false;
    }
    if (block.getBlockData() instanceof Slab slab) {
      return slab.getType().equals(Slab.Type.BOTTOM);
    }
    return !below.isEmpty();
  }

  public static Telepoint fromString(String s, Server server) {
    if (s.contains(",")) {
      String[] split = s.split(",");
      Type type = getType(split[1]);
      if (type == null) {
        return null;
      }
      World world = server.getWorld(split[2]);
      if (world == null) {
        return null;
      }
      double x = Double.parseDouble(split[3]);
      double y = Double.parseDouble(split[4]);
      double z = Double.parseDouble(split[5]);
      float yaw = Float.parseFloat(split[6]);
      float pitch = Float.parseFloat(split[7]);
      Location location = new Location(world, x, y, z, yaw, pitch);
      return new Telepoint(split[0], type, world, location);
    }
    return null;
  }

  public static Telepoint create(String name, Type type, Location location) {
    return new Telepoint(name, type, location.getWorld(), location);
  }

  public static Telepoint create(String name, Type type, Location location, World world) {
    return new Telepoint(name, type, world, location);
  }

  @Override
  public String toString() {
    return String.format("%s,%s,%s,%s,%s,%s,%s,%s", this.name, this.type, Objects.requireNonNull(this.location.getWorld()).getName(), this.location.getX(), this.location.getY(), this.location.getZ(), this.location.getYaw(), this.location.getPitch());
  }
}
