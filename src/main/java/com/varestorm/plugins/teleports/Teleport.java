package com.varestorm.plugins.teleports;

import com.varestorm.plugins.teleports.client.TeleportClient;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

public class Teleport {

    public static boolean instant(Telepoint telepoint, TeleportClient client) {
        Player player = client.getPlayer();
        client.setBack(player.getLocation());
        Location location = telepoint.getSafeLocation();
        particle(location, client);
        player.teleport(location);
        client.getApplication().getServer().getScheduler().runTaskLater(client.getApplication(), () -> {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }, 10);
        return true;
    }

    public static boolean to(Telepoint telepoint, TeleportClient client) {
        if (instant(telepoint, client)) {
            client.removeEnderPearl();
            return true;
        }
        return false;
    }

    public static void particle(Location location, TeleportClient client) {
        int count = 5;
        World world = location.getWorld();
        if (world != null) {
            Application application = client.getApplication();
            BukkitScheduler scheduler = application.getServer().getScheduler();

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

            scheduler.runTaskAsynchronously(application, () -> {
                world.spawnParticle(Particle.REDSTONE, location, count, dustOptions);
                world.spawnParticle(Particle.REDSTONE, location, count, dustOptions3);
            });
            scheduler.runTaskAsynchronously(application, () -> {
                world.spawnParticle(Particle.REDSTONE, location2, count, dustOptions2);
                world.spawnParticle(Particle.REDSTONE, location2, count, dustOptions4);
                world.spawnParticle(Particle.REDSTONE, location2, count, dustOptions5);
            });
            scheduler.runTaskAsynchronously(application, () -> {
                world.spawnParticle(Particle.REDSTONE, location3, count, dustOptions);
                world.spawnParticle(Particle.REDSTONE, location3, count, dustOptions3);
            });
            scheduler.runTaskAsynchronously(application, () -> {
                world.spawnParticle(Particle.REDSTONE, location4, count, dustOptions2);
                world.spawnParticle(Particle.REDSTONE, location4, count, dustOptions4);
                world.spawnParticle(Particle.REDSTONE, location4, count, dustOptions6);
            });
        }
    }

}
