package com.varestorm.plugins.teleports.client;

import com.varestorm.plugins.teleports.teleporting.Telepoint;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TeleportManager {

  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private final BossBar bossBar;
  private final Player player;
  private final Plugin plugin;
  private final List<ScheduledFuture<?>> tasks = new ArrayList<>();
  private final TeleportMessageManager messageManager;
  private boolean teleporting;
  boolean onCooldown;

  public TeleportManager(Player player, Plugin plugin) {
    this.player = player;
    this.plugin = plugin;
    this.teleporting = false;
    this.onCooldown = false;
    this.bossBar = player.getServer().createBossBar("Recharging teleport", BarColor.PURPLE, BarStyle.SEGMENTED_10);
    this.bossBar.addPlayer(player);
    this.bossBar.setVisible(false);
    this.messageManager = new TeleportMessageManager(player);
  }

  public boolean isOnCooldown() {
    return this.onCooldown;
  }

  public void start(Telepoint telepoint, boolean longer, Runnable callback) {
    if (this.teleporting) {
      this.messageManager.sendError("You are still teleporting, please wait...");
      return;
    }
    if (this.onCooldown) {
      this.sendMessage(Component.text("Recharging")
        .appendSpace()
        .append(Component.text("particles", NamedTextColor.GOLD))
        .appendSpace()
        .append(Component.text("takes time! Please wait...")));
      return;
    }
    this.teleporting = true;
    if (telepoint == null) {
      this.messageManager.sendError("Error!! Something went wrong...");
      return;
    }
    // Start teleport
    this.tasks.add(scheduler.schedule(() -> this.sendTitleStart(telepoint), 0, TimeUnit.MILLISECONDS));
    int milliseconds = 1000;
    // Loop seconds
    for (int i = longer ? 10 : 5; i > 0; i--) {
      int countdown = i;
      this.tasks.add(scheduler.schedule(() -> this.sendTitleCountdown(telepoint, countdown), milliseconds, TimeUnit.MILLISECONDS));
      milliseconds = milliseconds + 1000;
    }
    this.tasks.add(scheduler.schedule(() -> this.sendTitleEnding(telepoint), milliseconds, TimeUnit.MILLISECONDS));
    this.tasks.add(scheduler.schedule(callback, milliseconds + 10, TimeUnit.MILLISECONDS));
  }

  public void sendTitleStart(Telepoint telepoint) {
    scheduler.schedule(() -> this.player.playSound(this.player, Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1), 0, TimeUnit.MILLISECONDS);
    scheduler.schedule(() -> this.player.playSound(this.player, Sound.BLOCK_NOTE_BLOCK_BIT, 10, 4), 50, TimeUnit.MILLISECONDS);
    this.messageManager.teleportStart(telepoint.getWorld());
    this.player.getServer().getScheduler().runTask(this.plugin, () -> this.player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, -1, 1000, false)));
  }

  public void sendTitleCountdown(Telepoint telepoint, int countdown) {
    this.messageManager.teleportCountdown(telepoint.getWorld(), countdown);
    this.player.playSound(this.player, Sound.BLOCK_NOTE_BLOCK_BIT, 10, countdown < 3 ? 1 : 3);
  }

  public void sendTitleEnding(Telepoint telepoint) {
    scheduler.schedule(() -> this.player.playSound(this.player, Sound.BLOCK_NOTE_BLOCK_BIT, 10, 3), 200, TimeUnit.MILLISECONDS);
    scheduler.schedule(() -> this.player.playSound(this.player, Sound.BLOCK_NOTE_BLOCK_BIT, 10, 2), 400, TimeUnit.MILLISECONDS);
    scheduler.schedule(() -> this.player.playSound(this.player, Sound.BLOCK_NOTE_BLOCK_BIT, 10, 7), 600, TimeUnit.MILLISECONDS);
    this.messageManager.arrivalTitle(telepoint);
    this.teleporting = false;
    this.player.getServer().getScheduler().runTask(this.plugin, () -> this.player.removePotionEffect(PotionEffectType.BLINDNESS));
    if (telepoint.isInterdimensional(this.player)) {
      this.setCooldown();
    }
  }

  private void clearTasks() {
    for (ScheduledFuture<?> task : this.tasks) {
      task.cancel(false);
    }
    this.tasks.clear();
  }

  public boolean stop() {
    if (!this.teleporting) {
      return true;
    }
    this.player.removePotionEffect(PotionEffectType.BLINDNESS);
    this.teleporting = false;
    this.clearTasks();
    return false;
  }

  public void cancel() {
    if (!this.teleporting) {
      return;
    }
    if (this.stop()) {
      return;
    }
    this.player.playSound(this.player, Sound.BLOCK_NOTE_BLOCK_BIT, 10, -10);
    this.player.clearTitle();
    this.player.showTitle(Title.title(
      Component.text("ABORTED!", NamedTextColor.RED, TextDecoration.BOLD),
      Component.text("You moved!", NamedTextColor.GRAY),
      Title.Times.times(
        Duration.ZERO,
        Duration.ofSeconds(4),
        Duration.ofSeconds(1)
      )
    ));
  }

  public void setCooldown() {
    this.onCooldown = true;
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.5f), 500, TimeUnit.MILLISECONDS)); // 5
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.10f), 1000, TimeUnit.MILLISECONDS)); // 10
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.15f), 1500, TimeUnit.MILLISECONDS)); // 15
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.20f), 2000, TimeUnit.MILLISECONDS)); // 20
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.25f), 2500, TimeUnit.MILLISECONDS)); // 25
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.30f), 3000, TimeUnit.MILLISECONDS)); // 30
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.35f), 3500, TimeUnit.MILLISECONDS)); // 35
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.40f), 4000, TimeUnit.MILLISECONDS)); // 40
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.45f), 4500, TimeUnit.MILLISECONDS)); // 45
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.50f), 5000, TimeUnit.MILLISECONDS)); // 50
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.55f), 5500, TimeUnit.MILLISECONDS)); // 55
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.60f), 6000, TimeUnit.MILLISECONDS)); // 60
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.65f), 6500, TimeUnit.MILLISECONDS)); // 65
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.70f), 7000, TimeUnit.MILLISECONDS)); // 70
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.75f), 7500, TimeUnit.MILLISECONDS)); // 75
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.80f), 8000, TimeUnit.MILLISECONDS)); // 80
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.85f), 8500, TimeUnit.MILLISECONDS)); // 85
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.90f), 9000, TimeUnit.MILLISECONDS)); // 90
    this.tasks.add(scheduler.schedule(() -> this.setProgress(0.95f), 9500, TimeUnit.MILLISECONDS)); // 95
    this.tasks.add(scheduler.schedule(() -> this.setProgress(1f), 10000, TimeUnit.MILLISECONDS)); // 100
    ScheduledFuture<?> repeated = scheduler.scheduleAtFixedRate(this::particle, 1, 10, TimeUnit.MILLISECONDS);
    scheduler.schedule(() -> {
      this.resetCooldown();
      repeated.cancel(true);
    }, 10000, TimeUnit.MILLISECONDS);
  }

  private void particle() {
    Location location = this.player.getEyeLocation();
    Random random = new Random();
    int min = 0;
    double rand = (double) (random.nextInt(10 - min + 1) + min) / 10;
    location.setY((location.getY() - 1) + rand);
    this.player.spawnParticle(Particle.DUST, location, 2, new Particle.DustOptions(Color.fromRGB(255, 170, 0), 1));
  }

  private void setProgress(float percentage) {
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
    this.sendMessage(Component.text("You can teleport to other dimensions again!", NamedTextColor.GOLD));
  }

  private void sendMessage(Component message) {
    this.player.sendActionBar(message);
  }

  public void disable() {
    this.cancel();
    this.bossBar.removeAll();
    scheduler.shutdown();
  }
}
