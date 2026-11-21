package com.varestorm.plugins.teleports;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public record ParticleManager() {

  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

   public static void highlightBlock(Location location, Color color) {
      if (location == null) {
         return;
      }
      highlightBlock(location.getBlock(), color);
   }

   public static void highlightBlock(Block block, Color color) {
      Location location = block.getLocation();
      World world = block.getWorld();
      double minX = location.getX();
      double maxX = minX + 1;
      double minY = location.getY();
      double maxY = minY + 1;
      double minZ = location.getZ();
      double maxZ = minZ + 1;
      for (double x = minX; x <= maxX; x += 0.25) {
         for (double y = minY; y <= maxY; y += 0.25) {
            for (double z = minZ; z <= maxZ; z += 0.25) {
               boolean a = (x == minX || x == maxX);
               boolean b = (y == minY || y == maxY);
               boolean c = (z == minZ || z == maxZ);
               if (a ? b || c : b && c) {
                  world.spawnParticle(Particle.DUST, x, y, z, 1, new Particle.DustOptions(color, 1));
               }
            }
         }
      }
   }

   public void smoke(Location location) {
      Color purple = Color.fromRGB(138, 43, 226);
      this.smoke(location, purple, false);
   }

   public void smoke(Location location, Color primary) {
      this.smoke(location, primary, false);
   }

   public void smoke(Location location, Color primary, boolean low) {
      this.smoke(location, primary, low, 5);
   }

   public void smoke(Location location, Color primary, boolean low, int count) {
      if (location == null) {
         return;
      }
      Location center = location.clone().add(0.5, 0, 0.5);
      Location top = center.clone().add(0, 1, 0);
     scheduler.schedule(() -> doSmoke(center, primary, count), 1, TimeUnit.SECONDS);
     if (!low) scheduler.schedule(() -> doSmoke(top, primary, count), 1, TimeUnit.SECONDS);
   }

   public static void doSmoke(Location location, Color primary, int count) {
      World world = location.getWorld();
      if (world == null) {
         return;
      }
      Location location2 = location.clone().add(0, 0.5, 0);
      world.spawnParticle(Particle.DUST, location, count, randomOption(primary));
      world.spawnParticle(Particle.DUST, location, count, randomOption(primary));
      world.spawnParticle(Particle.DUST, location, count, randomOption(primary));
      world.spawnParticle(Particle.DUST, location2, count, randomOption(primary));
      world.spawnParticle(Particle.DUST, location2, count, randomOption(primary));
      world.spawnParticle(Particle.DUST, location2, count, randomOption(primary));
   }

   private static Particle.DustOptions randomOption(Color primary) {
      int min = 2;
      return new Particle.DustOptions(primary, new Random().nextInt(10 - min + 1) + min);
   }

   public void particle(Location location) {
      this.particle(location, 120);
   }

   public void particle(Location location, int count) {
      if (location.getWorld() == null) {
         return;
      }
      World world = location.getWorld();
      location.setX(location.getX() - 0.5);
      location.setZ(location.getZ() - 0.5);
     scheduler.submit(() -> world.spawnParticle(Particle.DUST, location, count));
   }

}
