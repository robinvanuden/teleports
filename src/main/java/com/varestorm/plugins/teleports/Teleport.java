package com.varestorm.plugins.teleports;

import com.google.gson.JsonObject;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Teleport {

    protected static Map<UUID, TeleportList> list;
    static final ChatColor RED = ChatColor.RED;
    static final ChatColor RESET = ChatColor.RESET;

    public static void init() {
        list = new HashMap<>();
    }

    public static TeleportList getList(UUID uuid) {
        return list.getOrDefault(uuid, null);
    }

    public static void setList(Player player, Application application, JsonObject object) {
        TeleportList tp = setList(player);
        tp.fromJson(application, object);
    }

    public static TeleportList setList(Player player) {
        TeleportList tp = new TeleportList(player);
        list.put(player.getUniqueId(), tp);
        return tp;
    }

    public static void instant(Player player, String name, Location location) {
        instant(player, name, location, false);
    }

    public static void instant(Player player, String name, Location location, boolean force) {
        Location to = Worlds.getFreeLocation(location);
        if (to == null) {
            player.sendMessage(RED + String.format("%sWarning!%s \"%s\" not found...", RED, RESET, name));
        } else if (to.getWorld() == null) {
            player.sendMessage(String.format("%sWarning!%s Something went wrong while teleporting to '%s'.", RED, RESET, name));
        } else {
            World world = to.getWorld();
            if (!force && !world.getName().equals(player.getWorld().getName())) {
                TeleportList list = getList(player.getUniqueId());
                if (!list.canCrossTeleport()) {
                    int cooldown = list.getCrossTeleportCooldown();
                    player.sendMessage(String.format("%sWarning!%s You have to wait %s second%s until you can teleport between worlds.", RED, RESET, cooldown, cooldown == 1 ? "" : "s"));
                    return;
                }
            }

            name = getName(name, world);
            if (!Worlds.canSpawn(to)) {
                player.sendMessage(String.format("%sWarning!%s %s is obstructed.", RED, RESET, name));
                return;
            }
            particle(player.getLocation());
            getList(player.getUniqueId()).setBack(player.getLocation());
            player.teleport(to);
            particle(to);
            player.sendMessage("Teleported to " + name);
        }
    }

    public static void particle(Location location) {
        int count = 5;
        World world = location.getWorld();
        if (world != null) {
            Location location2 = new Location(world, location.getX(), location.getY() + 0.5, location.getZ());
            Location location3 = new Location(world, location.getX(), location.getY() + 1, location.getZ());
            Location location4 = new Location(world, location.getX(), location.getY() + 1.5, location.getZ());

            Color purple = Color.fromRGB(138, 43, 226);
            Color purple_light = Color.fromRGB(147, 112, 219);
            Color purple_lighter = Color.fromRGB(172, 149, 220);
            Particle.DustOptions dustOptions = new Particle.DustOptions(purple, 3);
            Particle.DustOptions dustOptions2 = new Particle.DustOptions(purple_light, 3);
            Particle.DustOptions dustOptions3 = new Particle.DustOptions(purple_light, 2);
            Particle.DustOptions dustOptions4 = new Particle.DustOptions(purple, 2);
            Particle.DustOptions dustOptions5 = new Particle.DustOptions(purple_lighter, 1);
            Particle.DustOptions dustOptions6 = new Particle.DustOptions(purple_lighter, 1);

            world.spawnParticle(Particle.REDSTONE, location, count, dustOptions);
            world.spawnParticle(Particle.REDSTONE, location2, count, dustOptions2);
            world.spawnParticle(Particle.REDSTONE, location3, count, dustOptions);
            world.spawnParticle(Particle.REDSTONE, location4, count, dustOptions2);

            world.spawnParticle(Particle.REDSTONE, location, count, dustOptions3);
            world.spawnParticle(Particle.REDSTONE, location2, count, dustOptions4);
            world.spawnParticle(Particle.REDSTONE, location3, count, dustOptions3);
            world.spawnParticle(Particle.REDSTONE, location4, count, dustOptions4);

            world.spawnParticle(Particle.REDSTONE, location2, count, dustOptions5);
            world.spawnParticle(Particle.REDSTONE, location4, count, dustOptions6);
            world.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }
    }

    public static String getName(String name, World world) {
        switch (world.getEnvironment()) {
            case NETHER:
                return ChatColor.GOLD + name + ChatColor.RESET;
            case THE_END:
                return ChatColor.DARK_PURPLE + name + ChatColor.RESET;
            default:
            case NORMAL:
                return ChatColor.BLUE + name + ChatColor.RESET;
        }
    }

}
