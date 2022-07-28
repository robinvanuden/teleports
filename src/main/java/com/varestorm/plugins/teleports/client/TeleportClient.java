package com.varestorm.plugins.teleports.client;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.ParticleManager;
import com.varestorm.plugins.teleports.teleporting.AnchorPoint;
import com.varestorm.plugins.teleports.teleporting.BedPoint;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Logger;

public class TeleportClient {

   private static Logger LOGGER;
   static ChatColor BLUE = ChatColor.BLUE;
   static ChatColor GOLD = ChatColor.GOLD;
   static ChatColor YELLOW = ChatColor.YELLOW;
   static ChatColor PURPLE = ChatColor.DARK_PURPLE;
   static ChatColor RED = ChatColor.RED;
   static ChatColor GREEN = ChatColor.GREEN;
   static ChatColor GREY = ChatColor.GRAY;
   static ChatColor ITALIC = ChatColor.ITALIC;
   static ChatColor BOLD = ChatColor.BOLD;
   static ChatColor RESET = ChatColor.RESET;

   private final Player player;
   private final Application application;
   private Telepoint home, back;
   private BedPoint bed;
   private AnchorPoint anchor;
   private final Map<String, Telepoint> list;
   private final List<UUID> requests;
   private final TeleportFile file;
   private final BossBar bar;
   private boolean isTeleporting, hasCooldown;
   private final List<BukkitTask> tasks;
   private final ParticleManager particles;

   public TeleportClient(Player player, Application application) {
      LOGGER = application.getLogger();
      LOGGER.info(player.getDisplayName() + ".teleports loaded");
      this.tasks = new ArrayList<>();
      this.player = player;
      this.application = application;
      this.bar = Bukkit.createBossBar("Recharging teleport particles", BarColor.YELLOW, BarStyle.SOLID);
      this.isTeleporting = false;
      this.hasCooldown = false;
      this.list = new HashMap<>();
      this.requests = new ArrayList<>();
      this.file = new TeleportFile(application);
      this.particles = new ParticleManager(application);
      List<Telepoint> telepoints = this.file.loadTeleports(player);
      if (telepoints != null) {
         for (Telepoint telepoint : telepoints) {
            if (telepoint.getType().equals(Telepoint.Type.BACK)) {
               this.back = telepoint;
            }
            if (telepoint.getType().equals(Telepoint.Type.HOME)) {
               this.home = telepoint;
            }
            if (telepoint.getType().equals(Telepoint.Type.BED)) {
               this.bed = new BedPoint(telepoint);
            }
            if (telepoint.getType().equals(Telepoint.Type.ANCHOR)) {
               this.anchor = new AnchorPoint(telepoint);
               this.anchor.resetCharges();
            }
            if (telepoint.getType().equals(Telepoint.Type.CUSTOM)) {
               this.list.put(telepoint.getName(), telepoint);
            }
         }
      }
   }

   public boolean getList() {
      this.player.sendMessage(String.format("%s%sTeleports", PURPLE, BOLD));
      this.player.sendMessage("------------------------------");
      boolean hasHome = this.home != null;
      boolean hasBed = this.bed != null;
      if (hasBed) {
         hasBed = !this.bed.isBroken();
      }
      boolean hasAnchor = this.anchor != null;
      if (hasAnchor) {
         hasAnchor = !this.anchor.isBroken();
      }
      this.player.sendMessage(String.format("Home: %s%s%s!", hasHome ? GREEN : RED, hasHome ? "set" : "not set", RESET));
      this.player.sendMessage(String.format("Bed: %s%s%s!", hasBed ? GREEN : RED, hasBed ? "set" : "not set", RESET));
      int charges = 0;
      if (hasAnchor) {
         charges = this.anchor.getCharges();
      }
      boolean bar1 = charges > 0;
      boolean bar2 = charges > 1;
      boolean bar3 = charges > 2;
      boolean bar4 = charges > 3;
      String builder = String.format("%s%s", bar1 ? YELLOW : ChatColor.DARK_GRAY, bar1 ? "■" : "-") +
         String.format("%s%s", bar2 ? YELLOW : ChatColor.DARK_GRAY, bar2 ? "■" : "-") +
         String.format("%s%s", bar3 ? YELLOW : ChatColor.DARK_GRAY, bar3 ? "■" : "-") +
         String.format("%s%s", bar4 ? YELLOW : ChatColor.DARK_GRAY, bar4 ? "■" : "-");
      this.player.sendMessage(String.format("Anchor: %s%s %s[%s%s]", hasAnchor ? (charges > 0 ? GREEN : GOLD) : RED, hasAnchor ? (charges > 0 ? "set" : "empty") : "not set", GREY, builder, GREY));
      this.player.sendMessage("");
      this.player.sendMessage(String.format("%sTeleport list", PURPLE));
      this.player.sendMessage("------------------------------");
      System.out.println(this.list);
      if (this.list.size() > 0) {
         int count = 1;
         for (Map.Entry<String, Telepoint> entry : this.list.entrySet()) {
            Telepoint telepoint = entry.getValue();
            System.out.println(telepoint);
            this.player.sendMessage(String.format("%s. %s", count, telepoint.getTitle()));
            count++;
         }
      } else {
         this.player.sendMessage(String.format("Your list is empty. Use %s/tp set [name]%s to create a teleport.", PURPLE, RESET));
      }
      return true;
   }

   public boolean set(String raw) {
      String name = raw.toLowerCase();
      System.out.println(name);
      if (!name.matches("^[a-zA-Z]*$")) {
         this.sendMessage(String.format("%sYou can only use letters in the name!", RED));
         return true;
      }
      if (name.isEmpty()) {
         this.sendMessage(String.format("%sPlease enter a valid name!", RED));
         return true;
      }
      Location location = this.player.getLocation();
      Telepoint telepoint = Telepoint.create(name, Telepoint.Type.CUSTOM, location);
      if (this.list.containsKey(name)) {
         this.list.remove(name);
         this.list.put(name, telepoint);
         ParticleManager.highlightBlock(location, Color.ORANGE);
         this.sendMessage(String.format("Teleport %s%s%s updated!", PURPLE, name, RESET));
         return true;
      }
      if ((this.list.size() + 1) <= 5) {
         this.list.put(name, telepoint);
         ParticleManager.highlightBlock(location, Color.GREEN);
         this.sendMessage(String.format("Teleport %s%s%s set!", PURPLE, name, RESET));
         return true;
      }

      this.player.sendMessage(String.format("%sYou've reached the maximum of 5 teleports!", RED));
      this.player.sendMessage(String.format("%sYou can overwrite existing teleports with %s/tp set <name>", ITALIC, PURPLE));
      this.player.sendMessage(String.format("%sor delete teleports with %s/tp remove <name>", ITALIC, RED));
      return true;
   }

   public void instant(String name) {
      if (this.isTeleporting) {
         this.sendMessage("You are still teleporting, please wait...");
         return;
      }
      if (this.hasCooldown) {
         this.sendMessage(String.format("Recharging%s particles%s takes time! Please wait...", GOLD, RESET));
         return;
      }
      Telepoint telepoint = null;
      switch (name) {
         case "home":
            telepoint = this.home;
            break;
         case "bed":
            if (this.bed != null) {
               telepoint = this.bed.getTelepoint();
            }
            break;
         case "anchor":
            if (this.anchor != null) {
               telepoint = this.anchor.getTelepoint();
            }
            break;
      }
      if (telepoint == null) {
         ChatColor color;
         switch (name) {
            case "anchor":
               color = PURPLE;
               break;
            case "bed":
               color = RED;
               break;
            default:
               color = BLUE;
               break;
         }
         this.sendMessage(String.format("You don't have a %s%s%s yet!", color, name, RESET));
         return;
      }
      if (this.anchor != null && this.anchor.hasNoCharges() && name.equals("anchor")) {
         this.player.sendMessage(String.format("Your %sRespawn Anchor%s is out of charges...", PURPLE, RESET));
         this.player.sendMessage(String.format("%s%s/anchor %s%sis not possible without charges.", PURPLE, ITALIC, GREY, ITALIC));
         return;
      }
      if (this.anchor != null && this.anchor.isBroken() && name.equals("anchor")) {
         this.player.sendMessage(String.format("Your %sRespawn Anchor%s is broken...", PURPLE, RESET));
         this.player.sendMessage(String.format("%s%s/anchor %s%sis not possible until you place and charge a new one.", PURPLE, ITALIC, GREY, ITALIC));
         return;
      }
      if (this.bed != null && this.bed.isBroken() && name.equals("bed")) {
         this.player.sendMessage(String.format("Your %sBed%s is broken...", GOLD, RESET));
         this.player.sendMessage(String.format("%s%s/bed%s%s doesn't work anymore until you place or find a new bed.", GOLD, ITALIC, GREY, ITALIC));
         return;
      }
      if (telepoint.getSafeLocation() == null) {
         this.sendMessage(String.format("Teleport %s is not safe!", telepoint.getTitle()));
         return;
      }
      if (telepoint.isInterdimensional(this.player)) {
         this.setCooldown();
      }
      if (this.anchor != null && name.equals("anchor")) {
         if (this.anchor.hasNoCharges()) {
            this.player.sendMessage(String.format("Your %sRespawn Anchor%s is out of charges...", PURPLE, RESET));
            this.player.sendMessage(String.format("%s%s/anchor %s%sis not possible without charges.", PURPLE, ITALIC, GREY, ITALIC));
            return;
         }
         System.out.println("anchor");
         this.anchor.removeCharge();
      }
      if (!this.teleportInstant(telepoint)) {
         this.sendMessage(String.format("Can't teleport you to %s", telepoint.getTitle().toLowerCase()));
         return;
      }
      if (this.anchor != null && this.anchor.hasNoCharges() && name.equals("anchor")) {
         this.player.sendMessage(String.format("Your %sRespawn Anchor%s is out of charges...", PURPLE, RESET));
         this.player.sendMessage(String.format("%s%s/anchor %s%sis not possible without charges.", PURPLE, ITALIC, GREY, ITALIC));
      } else if (this.anchor != null && this.anchor.getCharges() < 2 && name.equals("anchor")) {
         this.player.sendMessage(String.format("%sWarning!%s Your %sRespawn Anchor%s is almost out of charges...", RED, RESET, PURPLE, RESET));
         this.player.sendMessage(String.format("%s%s/anchor %s%swill not be possible without charges.", PURPLE, ITALIC, GREY, ITALIC));
      }
   }

   public void toBack() {
      if (this.hasCooldown) {
         this.sendMessage(String.format("Recharging%s particles%s takes time! Please wait...", GOLD, RESET));
         return;
      }
      if (this.back == null) {
         this.sendMessage("You haven't teleported yet.");
         return;
      }
      if (this.back.isInterdimensional(this.player)) {
         this.setCooldown();
      }
      if (!this.teleportInstant(this.back)) {
         this.sendMessage(String.format("Can't teleport you to %s", this.back.getTitle().toLowerCase()));
      }
   }

   public void to(String name) {
      if (this.isTeleporting) {
         this.sendMessage("You are still teleporting, please wait...");
         return;
      }
      if (this.hasCooldown) {
         this.sendMessage(String.format("Recharging%s particles%s takes time! Please wait...", GOLD, RESET));
         return;
      }
      Telepoint telepoint = this.list.get(name);
      if (telepoint == null) {
         this.sendMessage(String.format("Teleport %s%s%s not found!", PURPLE, name, RESET));
         return;
      }
      if (!telepoint.getType().equals(Telepoint.Type.CUSTOM)) {
         this.sendMessage(String.format("Teleport %s not found!", telepoint.getTitle()));
         return;
      }
      if (telepoint.getSafeLocation() == null) {
         this.player.sendMessage(String.format("Teleport %s is not safe!", telepoint.getTitle()));
         return;
      }

      this.start(telepoint);
   }

   public void test(String name) {
      Telepoint telepoint = this.list.get(name);
      if (telepoint == null) {
         this.sendMessage(String.format("Teleport %s%s%s not found!", PURPLE, name, RESET));
         return;
      }
      telepoint.getSafeLocation();
   }

   private void start(Telepoint telepoint) {
      this.player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 400, 1000, false));
      this.isTeleporting = true;
      if (telepoint != null) {
         boolean interdimensional = telepoint.isInterdimensional(this.player);
         String title = String.format("%s%sTeleporting!", (!interdimensional ? PURPLE : GOLD), BOLD).toUpperCase();
         this.tasks.add(this.application.runLaterAsync(() -> this.sendTitleStart(title), 1));
         this.tasks.add(this.application.runLaterAsync(() -> this.sendTitleCountdown(title, 5), 20));
         this.tasks.add(this.application.runLaterAsync(() -> this.sendTitleCountdown(title, 4), 40));
         this.tasks.add(this.application.runLaterAsync(() -> this.sendTitleCountdown(title, 3), 60));
         this.tasks.add(this.application.runLaterAsync(() -> this.sendTitleCountdown(title, 2), 80));
         this.tasks.add(this.application.runLaterAsync(() -> this.sendTitleCountdown(title, 1), 100));
         this.tasks.add(this.application.runLater(() -> this.sendTitleEnding(telepoint), 120));
      }
   }

   public void sendTitleStart(String title) {
      if (this.isTeleporting) {
         this.player.sendTitle(
            title,
            String.format("Preparing to jump. %sDon't move!", RED),
            0, 60, 0
         );
         this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1);
         this.application.runLater(() -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 4), 3);
      }
   }

   public void sendTitleCountdown(String title, int countdown) {
      if (this.isTeleporting) {
         this.player.sendTitle(
            title,
            String.format("Jumping in %d %s!", countdown, "second" + (countdown == 1 ? "" : "s")),
            0, 22, 0
         );
         this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1);
      }
   }

   public void sendTitleEnding(Telepoint telepoint) {
      if (this.isTeleporting) {
         World currentWorld = this.player.getWorld();
         if (this.stop()) {
            if (this.teleportInstant(telepoint)) {
               this.player.sendTitle(
                  Telepoint.getWorldName(telepoint.getWorld(), true).toUpperCase(),
                  String.format("You've arrived at %s", telepoint.getTitle()),
                  0, 20, 30
               );
               this.application.runLater(() -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 3), 1);
               this.application.runLater(() -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 2), 3);
               this.application.runLater(() -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 7), 5);
               if (!currentWorld.getUID().equals(telepoint.getWorld().getUID())) {
                  this.setCooldown();
               }
            }
         } else {
            this.cancel();
         }
      }
   }

   private void clearTasks() {
      for (BukkitTask task : this.tasks) {
         task.cancel();
      }
   }

   public boolean stop() {
      if (this.isTeleporting) {
         this.player.removePotionEffect(PotionEffectType.BLINDNESS);
         this.clearTasks();
         this.isTeleporting = false;
         return true;
      }
      return false;
   }

   public void cancel() {
      if (this.stop()) {
         this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, -10);
         this.player.sendTitle(
            String.format("%s%sAborted!", RED, BOLD).toUpperCase(),
            "You moved!",
            0, 22, 0
         );
      }
   }

   public void setCooldown() {
      this.hasCooldown = true;
      this.bar.setProgress(0);
      this.bar.addPlayer(this.player);

      this.tasks.add(this.application.runLater(() -> this.setProgress(0.1), 20)); // 1
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.2), 40)); // 2
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.3), 60)); // 3
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.4), 80)); // 4
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.5), 100)); // 5
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.6), 120)); // 6
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.7), 140)); // 7
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.8), 160)); // 8
      this.tasks.add(this.application.runLater(() -> this.setProgress(0.9), 180)); // 9
      this.tasks.add(this.application.runLater(() -> this.setProgress(1), 200)); // 10

      BukkitScheduler scheduler = this.application.getServer().getScheduler();
      int cooldownId = scheduler.scheduleSyncRepeatingTask(this.application, this::particle, 1, 1);
      this.application.runLaterAsync(() -> {
         this.resetCooldown();
         scheduler.cancelTask(cooldownId);
      }, 205); // 10
   }

   public boolean isCoolingDown() {
      return this.hasCooldown;
   }

   private void setProgress(double progress) {
      if (!this.hasCooldown) {
         return;
      }
      this.bar.setProgress(progress);
   }

   public void resetCooldown() {
      if (!this.hasCooldown) {
         return;
      }
      this.bar.removePlayer(this.player);
      this.clearTasks();
      this.bar.removeAll();
      this.hasCooldown = false;
      this.sendMessage(GOLD + "You can teleport to other dimensions again!");
   }

   private void particle() {
      Location location = this.player.getEyeLocation();
      Random random = new Random();
      int min = 0;
      double rand = (double) (random.nextInt(10 - min + 1) + min) / 10;
      location.setY((location.getY() - 1) + rand);
      this.player.spawnParticle(Particle.REDSTONE, location, 2, new Particle.DustOptions(Color.fromRGB(255, 170, 0), 1));
   }

   public boolean remove(String name) {
      this.list.remove(name);
      this.sendMessage(String.format("Teleport %s%s%s deleted%s!", PURPLE, name, RED, RESET));
      return true;
   }

   public List<String> names() {
      return new ArrayList<>(this.list.keySet());
   }

   public Player getPlayer() {
      return this.player;
   }

   private void sendMessage(String message) {
      this.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
   }

   public void saveTelepoints() {
      List<Telepoint> telepoints = new ArrayList<>();
      if (this.back != null)
         telepoints.add(this.back);
      if (this.home != null)
         telepoints.add(this.home);
      if (this.bed != null)
         telepoints.add(this.bed.getTelepoint());
      if (this.anchor != null)
         telepoints.add(this.anchor.getTelepoint());
      telepoints.addAll(this.list.values());
      if (!this.file.saveTeleports(this.player, telepoints)) {
         LOGGER.warning(String.format("Telepoints of %s couldn't be saved!", this.player.getName()));
      }
      LOGGER.info(String.format("Telepoints of %s saved!", this.player.getName()));
   }

   public void setBack(Location location) {
      this.back = Telepoint.create("Back", Telepoint.Type.BACK, location);
   }

   public void setHome() {
      Location location = this.player.getLocation();
      this.home = Telepoint.create("Home", Telepoint.Type.HOME, location);
      this.sendMessage(Telepoint.getTitle(location, "Home") + " location saved!");
      this.player.sendMessage(String.format("You can use %s/home%s to teleport to your %s%s.", PURPLE, RESET, this.home.getTitle(), RESET));
      this.particles.smoke(this.home.getLocation());
   }

   public boolean setBed(Location location) {
      Telepoint telepoint = Telepoint.create("bed", Telepoint.Type.BED, location);
      if (this.bed != null && this.bed.getTelepoint().getLocation().equals(telepoint.getLocation())) {
         // Bed already clicked on
         return false;
      }
      this.bed = new BedPoint(telepoint);
      this.sendMessage(Telepoint.getTitle(location, "Bed") + " location saved!");
      this.player.sendMessage(String.format("You can use %s/bed%s to teleport to this%s bed%s.", GOLD, RESET, GOLD, RESET));
      this.particles.particle(telepoint.getLocation());
      return true;
   }

   public void setAnchor(Location location) {
      Telepoint telepoint = Telepoint.create("anchor", Telepoint.Type.ANCHOR, location);
      if (this.anchor != null && this.anchor.getTelepoint().getLocation().equals(telepoint.getLocation())) {
         // Anchor already clicked on
         return;
      }
      this.anchor = new AnchorPoint(telepoint);
      this.player.sendMessage(Telepoint.getTitle(location, "Respawn Anchor") + " location saved!");
      this.sendMessage(String.format("You can use %s/anchor%s to teleport to this %sRespawn Anchor%s.", PURPLE, RESET, PURPLE, RESET));
      this.anchor.newAnchorEffect();
      World world = location.getWorld();
      if (world == null) {
         return;
      }
      world.playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1, 1);
   }

   public void testHome() {
      LOGGER.info("testHome: test");
      this.home.getSafeLocation();
   }

   public void testBed() {
      LOGGER.info("testBed: test");
      this.bed.getTelepoint().getSafeLocation();
   }

   public void testAnchor() {
      LOGGER.info("testAnchor: test");
      if (this.anchor == null) {
         return;
      }
      this.anchor.getTelepoint().getSafeLocation();
      this.anchor.newAnchorEffect();
   }

   public boolean brokeBed(Block block, Player breaker) {
      if (this.player.getUniqueId().equals(breaker.getUniqueId())) {
         this.player.sendMessage(String.format("You broke your %sBed%s...", GOLD, RESET));
         this.player.sendMessage(String.format("%s%s/bed%s%s doesn't work anymore until you place or find a new bed.", GOLD, ITALIC, GREY, ITALIC));

         return true;
      }
      Telepoint telepoint = Telepoint.create("Possible Bed", Telepoint.Type.BED, block.getLocation(), block.getWorld());
      if (this.bed != null && this.bed.getTelepoint().getLocation().equals(telepoint.getLocation())) {
         this.player.sendMessage(String.format("%s%s%s broke your %sBed%s...", RED, breaker.getDisplayName(), RESET, GOLD, RESET));
         this.player.sendMessage(String.format("%s%s/bed%s%s doesn't work anymore until you place or find a new bed.", GOLD, ITALIC, GREY, ITALIC));
         return true;
      }
      return false;
   }

   public boolean brokeAnchor(Block block, Player breaker) {
      if (this.player.getUniqueId().equals(breaker.getUniqueId())) {
         this.player.sendMessage(String.format("You broke your %sRespawn Anchor%s...", PURPLE, RESET));
         this.player.sendMessage(String.format("%s%s/anchor %s%sis not possible until you place and charge a new one.", PURPLE, ITALIC, GREY, ITALIC));
         return true;
      }
      Telepoint telepoint = Telepoint.create("Possible Anchor", Telepoint.Type.ANCHOR, block.getLocation(), block.getWorld());
      if (this.anchor != null && this.anchor.getTelepoint().getLocation().equals(telepoint.getLocation())) {
         this.player.sendMessage(String.format("%s%s%s broke your %sRespawn Anchor%s...", RED, breaker.getDisplayName(), RESET, PURPLE, RESET));
         this.player.sendMessage(String.format("%s%s/anchor %s%sis not possible until you place and charge a new one.", PURPLE, ITALIC, GREY, ITALIC));
         return true;
      }
      return false;
   }

   public List<UUID> getRequests() {
      return this.requests;
   }

   public void addRequest(UUID uuid) {
      this.requests.add(uuid);
   }

   public void removeRequest(UUID uuid) {
      this.requests.remove(uuid);
   }

   public boolean hasRequests() {
      return this.requests.size() > 0;
   }

   public boolean hasRequest(UUID uuid) {
      return this.requests.contains(uuid);
   }

   public boolean teleportInstant(Telepoint telepoint) {
      Player player = this.getPlayer();
      player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 400, 1000, false));
      this.setBack(player.getLocation());
      Location location = telepoint.getSafeLocation();
      if (player.teleport(location)) {
         this.particles.smoke(location);
         this.application.runLater(() -> player.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1), 2);
         this.application.runLater(() -> player.removePotionEffect(PotionEffectType.BLINDNESS), 3);
      }
      return true;
   }

}
