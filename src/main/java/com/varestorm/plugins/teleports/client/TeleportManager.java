package com.varestorm.plugins.teleports.client;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.ParticleManager;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class TeleportManager {

   private static Logger LOGGER;
   final static ChatColor GOLD = ChatColor.GOLD;
   final static ChatColor RED = ChatColor.RED;
   final static ChatColor BOLD = ChatColor.BOLD;
   final static ChatColor RESET = ChatColor.RESET;

   private final BossBar bossBar;
   private final Player player;
   private final Application application;
   private final List<BukkitTask> tasks;
   private final ParticleManager particles;
   private boolean teleporting, onCooldown;

   public TeleportManager(Player player, Application application, ParticleManager particles) {
      LOGGER = application.getLogger();
      this.player = player;
      this.application = application;
      this.tasks = new ArrayList<>();
      this.particles = particles;
      this.teleporting = false;
      this.onCooldown = false;
      this.bossBar = application.getServer().createBossBar("Recharging teleport particles", BarColor.YELLOW, BarStyle.SOLID);
      this.bossBar.addPlayer(player);
      this.bossBar.setVisible(false);
   }

   public boolean teleport(Telepoint telepoint) {
      Location location = telepoint.getSafeLocation();
      if (location == null) {
         this.sendMessage("Teleport was not safe...");
         return false;
      }
      return this.teleport(location);
   }

   public boolean teleport(Location location) {
      if (this.teleporting) {
         this.sendMessage("You are still teleporting, please wait...");
         return false;
      }
      if (this.onCooldown) {
         this.sendMessage(String.format("Recharging%s particles%s takes time! Please wait...", GOLD, RESET));
         return false;
      }
      if (location == null) {
         return false;
      }
      if (Telepoint.isInterdimensional(this.player, location.getWorld().getEnvironment())) {
         this.setCooldown();
      }
      this.doLoadChunk(location);
      this.application.runLater(() -> this.doTeleport(location), 1);
      this.application.runLaterAsync(() -> this.particles.smoke(location.getBlock().getLocation().clone()), 1);
      this.application.runLater(() -> this.player.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1), 2);
      this.application.runLater(() -> this.player.removePotionEffect(PotionEffectType.BLINDNESS), 2);
      return true;
   }

   public void start(Telepoint telepoint, boolean longer) {
      if (this.teleporting) {
         this.sendMessage("You are still teleporting, please wait...");
         return;
      }
      if (this.onCooldown) {
         this.sendMessage(String.format("Recharging%s particles%s takes time! Please wait...", GOLD, RESET));
         return;
      }
      this.player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100000, 1000, false));
      this.teleporting = true;
      if (telepoint == null) {
         this.sendMessage(RED + "Err!! Something went wrong...");
         return;
      }
      int ticks = 1;
      Location location = telepoint.getLocation();
      String title = String.format("%s%sTeleporting!", telepoint.getWorldColor(), BOLD).toUpperCase();
      this.tasks.add(this.application.runLaterAsync(() -> this.sendTitleStart(title), 1));
      for (int i = longer ? 10 : 5; i > 0; i--) {
         ticks = ticks + 10;
         this.tasks.add(this.application.runLaterAsync(() -> this.doLoadChunk(location), ticks));
         ticks = ticks + 5;
         this.application.runLaterAsync(() -> this.particles.smoke(telepoint.getSafeLocation(), Color.WHITE), ticks);
         ticks = ticks + 5;
         int countdown = i;
         this.tasks.add(this.application.runLaterAsync(() -> this.sendTitleCountdown(title, countdown), ticks));
      }
      ticks = ticks + 10;
      this.tasks.add(this.application.runLater(() -> this.sendTitleEnding(telepoint), ticks));
   }

   public void doLoadChunk(Location location) {
      Chunk chunk = location.getChunk();
      if (!chunk.isLoaded()) {
         LOGGER.info("Chuck load");
         chunk.load();
      }
   }

   void showTitle(String title, String subtitle) {
      this.showTitle(title, subtitle, true);
   }

   void showTitle(String title, String subtitle, boolean zero) {
      this.player.clearTitle();
      this.player.showTitle(Title.title(Component.text(title), Component.text(subtitle), Title.Times.times(zero ? Duration.ZERO : Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1))));
   }

   public void sendTitleStart(String title) {
      if (this.teleporting) {
         this.showTitle(title, String.format("Preparing to jump. %sDon't move!", RED));
         this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1);
         this.application.runLater(() -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 4), 3);
      }
   }

   public void sendTitleCountdown(String title, int countdown) {
      if (this.teleporting) {
         this.showTitle(title, String.format("Jumping in %d %s!", countdown, "second" + (countdown == 1 ? "" : "s")));
         this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1);
      }
   }

   public void sendTitleEnding(Telepoint telepoint) {
      if (!this.teleporting) {
         return;
      }
      boolean interdimensional = telepoint.isInterdimensional(this.player);
      if (this.stop()) {
         this.cancel();
         return;
      }
      if (!this.teleport(telepoint)) {
         return;
      }
      this.application.runLater(() -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 3), 1);
      this.application.runLater(() -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 2), 3);
      this.application.runLater(() -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 7), 5);
      this.application.runLater(() -> this.sendArrivalTitle(telepoint), 3);
      if (interdimensional) {
         this.setCooldown();
      }
   }

   public void sendArrivalTitle(Telepoint telepoint) {
      this.sendArrivalTitle(telepoint, true);
   }

   public void sendArrivalTitle(Telepoint telepoint, boolean lowercase) {
      String subtitle = String.format("You've arrived at %s", telepoint.getTitle());
      if (lowercase) {
         subtitle = subtitle.toLowerCase();
      }
      this.showTitle(Telepoint.getWorldName(telepoint.getWorld(), true).toUpperCase(), subtitle, false);
   }

   private void clearTasks() {
      for (BukkitTask task : this.tasks) {
         task.cancel();
      }
   }

   public boolean stop() {
      if (!this.teleporting) {
         return true;
      }
      this.player.removePotionEffect(PotionEffectType.BLINDNESS);
      this.clearTasks();
      this.teleporting = false;
      return false;
   }

   public void cancel() {
      if (this.stop()) {
         return;
      }
      this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, -10);
      this.showTitle(String.format("%s%sAborted!", RED, BOLD).toUpperCase(), "You moved!");
   }

   public void setCooldown() {
      this.onCooldown = true;
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.5), 5)); // 5
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.10), 10)); // 10
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.15), 20)); // 15
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.20), 30)); // 20
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.25), 40)); // 25
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.30), 50)); // 30
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.35), 60)); // 35
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.40), 70)); // 40
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.45), 80)); // 45
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.50), 90)); // 50
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.55), 100)); // 55
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.60), 110)); // 60
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.65), 120)); // 65
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.70), 130)); // 70
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.75), 140)); // 75
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.80), 150)); // 80
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.85), 160)); // 85
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.90), 170)); // 90
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.95), 180)); // 95
      this.tasks.add(this.application.runLater(() -> this.setProgress(1), 190)); // 100

      BukkitScheduler scheduler = this.application.getServer().getScheduler();
      int cooldownId = scheduler.scheduleSyncRepeatingTask(this.application, this::particle, 1, 1);
      this.application.runLaterAsync(() -> {
         this.resetCooldown();
         scheduler.cancelTask(cooldownId);
      }, 195); // 10
   }

   private void particle() {
      Location location = this.player.getEyeLocation();
      Random random = new Random();
      int min = 0;
      double rand = (double) (random.nextInt(10 - min + 1) + min) / 10;
      location.setY((location.getY() - 1) + rand);
      this.player.spawnParticle(Particle.REDSTONE, location, 2, new Particle.DustOptions(Color.fromRGB(255, 170, 0), 1));
   }

   private void setProgress(double percentage) {
      if (percentage == 1) {
         this.bossBar.setVisible(false);
         return;
      }
      this.bossBar.setVisible(true);
      this.bossBar.setProgress(percentage);
   }

   public void resetCooldown() {
      if (!this.onCooldown) {
         return;
      }
      this.clearTasks();
      this.onCooldown = false;
      this.sendMessage(GOLD + "You can teleport to other dimensions again!");
   }

   private void sendMessage(String message) {
      this.player.sendActionBar(Component.text(message));
   }

   private void doTeleport(Location location) {
      this.player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
   }

   public void disable() {
      this.cancel();
      this.bossBar.removeAll();
   }
}
