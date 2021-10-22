package com.varestorm.plugins.teleports;

import com.varestorm.plugins.teleports.client.TeleportClient;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class Teleport {

    public static boolean instant(Telepoint telepoint, TeleportClient client) {
        Player player = client.getPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 400, 1000, false));
        client.setBack(player.getLocation());
        Location location = telepoint.getSafeLocation();
        if (player.teleport(location)) {
            particle(location);
            Application.runLaterAsync(() -> {
                player.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                Application.runLater(() -> player.removePotionEffect(PotionEffectType.BLINDNESS), 1);
            }, 2);
        }
        return true;
    }

    public static boolean to(Telepoint telepoint, TeleportClient client) {
        if (instant(telepoint, client)) {
            client.removeEnderPearl();
            return true;
        }
        return false;
    }


    public static void particle(Location location) {
        particle(location, Color.fromRGB(138, 43, 226), false);
    }

    public static void particle(Location location, Color primary) {
        particle(location, primary, false);
    }

    public static void particle(Location location, Color primary, boolean low) {
        if (location == null) {
            return;
        }
        int count = 5;
        World world = location.getWorld();
        if (world != null) {

            Location location2 = new Location(world, location.getX(), location.getY() + 0.5, location.getZ());

            Location location3 = new Location(world, location.getX(), location.getY() + 1, location.getZ());
            Location location4 = new Location(world, location.getX(), location.getY() + 1.5, location.getZ());


            Application.runAsync(() -> {
                world.spawnParticle(Particle.REDSTONE, location, count, randomOption(primary));
                world.spawnParticle(Particle.REDSTONE, location, count, randomOption(primary));
            });
            Application.runAsync(() -> {
                world.spawnParticle(Particle.REDSTONE, location2, count, randomOption(primary));
                world.spawnParticle(Particle.REDSTONE, location2, count, randomOption(primary));
                world.spawnParticle(Particle.REDSTONE, location2, count, randomOption(primary));
            });
            if (!low) {
                Application.runAsync(() -> {
                    world.spawnParticle(Particle.REDSTONE, location3, count, randomOption(primary));
                    world.spawnParticle(Particle.REDSTONE, location3, count, randomOption(primary));
                });
                Application.runAsync(() -> {
                    world.spawnParticle(Particle.REDSTONE, location4, count, randomOption(primary));
                    world.spawnParticle(Particle.REDSTONE, location4, count, randomOption(primary));
                    world.spawnParticle(Particle.REDSTONE, location4, count, randomOption(primary));
                });
            }
        }
    }

    private static Particle.DustOptions randomOption(Color primary) {
        int min = 2;
        Random random = new Random();
        int rand = random.nextInt(10 - min + 1) + min;
        return new Particle.DustOptions(primary, rand);
    }

}
