package com.varestorm.plugins.teleports;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Worlds {

    public static Location getFreeLocation(Block block) {
        return getFreeLocation(block.getLocation());
    }

    public static List<Block> getSurroundingBlocks(Location location) {
        World world = location.getWorld();
        Block block = location.getBlock();
        List<Block> blocks = new ArrayList<>();
        if (world != null) {
            blocks.add(block.getRelative(BlockFace.UP));
            blocks.add(block.getRelative(BlockFace.DOWN));
            blocks.add(block.getRelative(BlockFace.NORTH));
            blocks.add(block.getRelative(BlockFace.EAST));
            blocks.add(block.getRelative(BlockFace.SOUTH));
            blocks.add(block.getRelative(BlockFace.WEST));
        }
        return blocks;
    }

    public static Block findBlock(Material material, List<Block> blocks) {
        for (Block block : blocks) {
            if (block.getBlockData().getMaterial() == material) {
                return block;
            }
        }
        return null;
    }

    public static Block findBlock(String name, List<Block> blocks) {
        for (Block block : blocks) {
            if (block.getBlockData().getMaterial().name().contains(name)) {
                return block;
            }
        }
        return null;
    }

    public static Block findBlock(String name, Location location) {
        return findBlock(name, getSurroundingBlocks(location));
    }

    public static Location getFreeLocation(Location location) {
        System.out.println("Find location:");
        if (location == null) {
            return null;
        }
        if (canSpawn(location)) {
            System.out.println(TeleportList.locToString("location", location));
            return location;
        }

        Location center = getCenter(location);
        if (canSpawn(center)) {
            System.out.println(TeleportList.locToString("center", center));
            return center;
        }

        double x = center.getX();
        double y = center.getY();
        double z = center.getZ();
        float pitch = center.getPitch();
        float yaw = center.getYaw();
        World world = center.getWorld();

        Location above = new Location(world, x, y + 1, z, yaw, pitch);
        if (canSpawn(above)) {
            System.out.println(TeleportList.locToString("above", above));
            return above;
        }
        Location below = new Location(world, x, y - 1, z, yaw, pitch);
        if (canSpawn(below)) {
            System.out.println(TeleportList.locToString("below", below));
            return below;
        }
        Location top = new Location(world, x, y, z + 1, yaw, pitch);
        if (canSpawn(top)) {
            System.out.println(TeleportList.locToString("top", top));
            return top;
        }
        Location bottom = new Location(world, x, y, z - 1, yaw, pitch);
        if (canSpawn(bottom)) {
            System.out.println(TeleportList.locToString("bottom", bottom));
            return bottom;
        }
        Location left = new Location(world, x + 1, y, z, yaw, pitch);
        if (canSpawn(left)) {
            System.out.println(TeleportList.locToString("left", left));
            return left;
        }
        Location right = new Location(world, x - 1, y, z, yaw, pitch);
        if (canSpawn(right)) {
            System.out.println(TeleportList.locToString("right", right));
            return right;
        }
        return null;
    }

    public static Location getCenter(Location location) {
        Location center = location.clone();
        double x = (int) center.getX() + 0.5;
        double y = (int) center.getY();
        double z = (int) center.getZ() + 0.5;
        float pitch = center.getPitch();
        float yaw = center.getYaw();
        World world = center.getWorld();

        return new Location(world, x, y, z, yaw, pitch);
    }

    public static boolean canSpawn(Location location) {
        return isFreeBlock(location);
    }

    public static boolean isFreeBlock(Location location) {
        Block block = location.getBlock();
        return block.isPassable();
    }

    public static Location lookAt(Location from, Location look_at) {
        Vector target = look_at.toVector(); //our target location (Point B)
        from.setDirection(target.subtract(from.toVector())); //set the origin's direction to be the direction vector between point A and B.
        return from;
    }

}
