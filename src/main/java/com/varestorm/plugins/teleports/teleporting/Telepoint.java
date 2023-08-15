package com.varestorm.plugins.teleports.teleporting;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.ParticleManager;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Objects;

public record Telepoint(String name, com.varestorm.plugins.teleports.teleporting.Telepoint.Type type, World world, Location location) {

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

   public String getTitle() {
      return this.getTitle(false);
   }

   public String getTitle(boolean bold) {
      return getTitle(this.getEnvironment(), this.name, bold);
   }

   public static String getTitle(Location location, String name) {
      return getTitle(location.getWorld(), name);
   }

   public static String getTitle(@Nullable World world, String name) {
      if (world == null) {
         return name;
      }
      return getTitle(world.getEnvironment(), name);
   }

   public static String getTitle(World.Environment environment, String name) {
      return getTitle(environment, name, false);
   }

   public static String getTitle(World.Environment environment, String name, boolean bold) {
      return String.format("%s%s%s%s", getWorldColor(environment), bold ? ChatColor.BOLD : "", name, ChatColor.RESET);
   }

   public static ChatColor getWorldColor(World.Environment environment) {
      return switch (environment) {
         case NETHER -> ChatColor.GOLD;
         case THE_END -> ChatColor.DARK_PURPLE;
         case NORMAL -> ChatColor.BLUE;
         default -> ChatColor.GREEN;
      };
   }

   public ChatColor getWorldColor() {
      return getWorldColor(this.getWorld().getEnvironment());
   }

   public static String getWorldName(World world, boolean bold) {
      return getWorldName(world.getEnvironment(), bold);
   }

   public static String getWorldName(World.Environment environment, boolean bold) {
      String world = switch (environment) {
         case NORMAL -> "Varestørm";
         case NETHER -> "The Nether Regions";
         case THE_END -> "The End";
         default -> "The Unknown";
      };
      return Telepoint.getTitle(environment, world, bold);
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
      ParticleManager.highlightBlock(location, Color.BLUE);
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

   public static Telepoint fromString(String s, Application application) {
      if (s.contains(",")) {
         String[] split = s.split(",");
         Type type = getType(split[1]);
         if (type == null) {
            return null;
         }
         World world = application.getWorld(split[2]);
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
      return String.format("%s,%s,%s,%s,%s,%s,%s,%s", this.getName(), this.getType(), Objects.requireNonNull(this.location.getWorld()).getName(), this.location.getX(), this.location.getY(), this.location.getZ(), this.location.getYaw(), this.location.getPitch());
   }
}
