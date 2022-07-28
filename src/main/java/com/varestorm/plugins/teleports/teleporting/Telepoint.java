package com.varestorm.plugins.teleports.teleporting;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.ParticleManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Objects;

public class Telepoint {

   public enum Type {BACK, HOME, BED, ANCHOR, CUSTOM}

   private static final BlockFace[] FACES = new BlockFace[]{BlockFace.SELF, BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST};

   private final World world;
   private final Location location;
   private final String name;
   private final Type type;

   Telepoint(String name, Type type, World world, Location location) {
      this.location = location;
      this.world = world;
      this.name = name.toLowerCase();
      this.type = type;
   }

   public Location getLocation() {
      return this.location;
   }

   public boolean isInterdimensional(Player player) {
      World world = player.getLocation().getWorld();
      if (world == null) {
         return true;
      }
      World.Environment environment = world.getEnvironment();
      World.Environment environment2 = this.getEnvironment();
      return (!environment.equals(environment2));
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
      switch (environment) {
         case NETHER:
            return ChatColor.GOLD;
         case THE_END:
            return ChatColor.DARK_PURPLE;
         case NORMAL:
            return ChatColor.BLUE;
         default:
            return ChatColor.GREEN;
      }
   }

   public static String getWorldName(World world, boolean bold) {
      return getWorldName(world.getEnvironment(), bold);
   }

   public static String getWorldName(World.Environment environment, boolean bold) {
      String world;
      switch (environment) {
         case NORMAL:
            world = "Varestørm";
            break;
         case NETHER:
            world = "The Nether Regions";
            break;
         case THE_END:
            world = "The End";
            break;
         default:
            world = "The Unknown";
            break;
      }
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
      if (this.getLocation() == null) {
         return null;
      }
      Location location = this.getLocation().clone();
      ParticleManager.highlightBlock(location, Color.BLUE);
      Location safe = checkSafeLocation(location.getBlock());
      if (safe != null) {
         return location;
      }
      return getSafeLocation(this.getLocation(), BlockFace.UP);
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
      return getSafeLocation(block.getRelative(checkAfter).getLocation(), checkAfter);
   }

   private static Location checkSafeLocation(Block block) {
      if (!block.isEmpty()) {
         ParticleManager.highlightBlock(block, Color.RED);
         return null;
      }
      if (block.isLiquid()) {
         ParticleManager.highlightBlock(block, Color.RED);
         return null;
      }
      if (!blockBelowSafe(block)) {
         ParticleManager.highlightBlock(block, Color.RED);
         return null;
      }
      ParticleManager.highlightBlock(block, Color.GREEN);
      Location blockLocation = block.getLocation();
      blockLocation.setX(blockLocation.getX() + 0.5);
      blockLocation.setY(blockLocation.getY() + 0.5);
      blockLocation.setZ(blockLocation.getZ() + 0.5);
      return blockLocation;
   }

   public static boolean blockBelowSafe(Block block) {
      Block below = block.getRelative(BlockFace.DOWN);
//      if (below.isLiquid()) {
//         return false;
//      }
      Material belowType = below.getType();
      if (!belowType.isSolid()) {
         return false;
      }
      return !belowType.isAir();
   }

   public Block getRelative(BlockFace face) {
      return this.getLocation().getBlock().getRelative(face);
   }

   public Location getRelativeLocation(BlockFace face) {
      return this.getRelative(face).getLocation();
   }

   public Location getTop() {
      return this.getRelativeLocation(BlockFace.UP);
   }

   public Location getBottom() {
      return this.getRelativeLocation(BlockFace.DOWN);
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
