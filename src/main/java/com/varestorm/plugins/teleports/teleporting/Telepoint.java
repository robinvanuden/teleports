package com.varestorm.plugins.teleports.teleporting;

import com.varestorm.plugins.teleports.Application;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Objects;

public class Telepoint {

    public enum Type {BACK, HOME, BED, ANCHOR, CUSTOM}

    private final World world;
    private final Location location;
    private final String name;
    private final Type type;

    public Telepoint(String name, Type type, World world, Location location) {
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
        switch (environment) {
            case NETHER:
                return String.format("%s%s%s%s", ChatColor.GOLD, bold ? ChatColor.BOLD : "", name, ChatColor.RESET);
            case THE_END:
                return String.format("%s%s%s%s", ChatColor.DARK_PURPLE, bold ? ChatColor.BOLD : "", name, ChatColor.RESET);
            default:
            case NORMAL:
                return String.format("%s%s%s%s", ChatColor.BLUE, bold ? ChatColor.BOLD : "", name, ChatColor.RESET);
        }
    }

    public static String getWorldName(World world) {
        return getWorldName(world, false);
    }

    public static String getWorldName(World world, boolean bold) {
        return getWorldName(world.getEnvironment(), bold);
    }

    public static String getWorldName(World.Environment environment) {
        return getWorldName(environment, false);
    }

    public static String getWorldName(World.Environment environment, boolean bold) {
        String world;
        switch (environment) {
            case NORMAL:
                world = "the overworld";
                break;
            case NETHER:
                world = "the Nether";
                break;
            case THE_END:
                world = "the End";
                break;
            default:
            case CUSTOM:
                world = "a different place";
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
        Location safe = safeLocation(this.getLocation());
        if (safe != null) {
            return safe;
        }
        Block block = this.getLocation().getBlock();
        for (BlockFace face : BlockFace.values()) {
            Block b = block.getRelative(face);
            Location s = safeLocation(b.getLocation());
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    public static Location safeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        location.setX(location.getBlockX() + 0.5);
        location.setY(location.getBlockY() + 0.5);
        location.setZ(location.getBlockZ() + 0.5);

        Block block = location.getBlock();
        if (block.isPassable()) {
            return location;
        }
        Block top = block.getRelative(BlockFace.UP);
        Location topLocation = top.getLocation();
        topLocation.setX(topLocation.getBlockX() + 0.5);
        topLocation.setY(topLocation.getBlockY() + 0.5);
        topLocation.setZ(topLocation.getBlockZ() + 0.5);

        if (top.isPassable()) {
            return topLocation;
        }
//        Teleport.smoke(location, Color.RED);
        return null;
    }

    public static Telepoint fromJson(String s) {
        if (s.contains(",")) {
            String[] split = s.split(",");
            Type type = getType(split[1]);
            if (type == null) {
                return null;
            }
            World world = Application.getWorld(split[2]);
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

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s", this.getName(), this.getType(), Objects.requireNonNull(this.location.getWorld()).getName(), this.location.getX(), this.location.getY(), this.location.getZ(), this.location.getYaw(), this.location.getPitch());
    }
}
