package com.varestorm.plugins.teleports.client;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.ParticleManager;
import com.varestorm.plugins.teleports.teleporting.AnchorPoint;
import com.varestorm.plugins.teleports.teleporting.BedPoint;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

public class TeleportClient {

   private static Logger LOGGER;
   final static ChatColor BLUE = ChatColor.BLUE;
   final static ChatColor GOLD = ChatColor.GOLD;
   final static ChatColor YELLOW = ChatColor.YELLOW;
   final static ChatColor PURPLE = ChatColor.DARK_PURPLE;
   final static ChatColor RED = ChatColor.RED;
   final static ChatColor GREEN = ChatColor.GREEN;
   final static ChatColor GREY = ChatColor.GRAY;
   final static ChatColor ITALIC = ChatColor.ITALIC;
   final static ChatColor BOLD = ChatColor.BOLD;
   final static ChatColor RESET = ChatColor.RESET;

   private final Player player;
   private Telepoint home, back;
   private BedPoint bed;
   private AnchorPoint anchor;
   private final Map<String, Telepoint> list;
   private final List<UUID> requests;
   private final TeleportFile file;
   private final ParticleManager particles;
   private final TeleportManager manager;

   public TeleportClient(Player player, Application application) {
      LOGGER = application.getLogger();
      LOGGER.info(player.getName() + ".teleports loaded");
      this.player = player;
      this.list = new HashMap<>();
      this.requests = new ArrayList<>();
      this.file = new TeleportFile(application);
      this.particles = new ParticleManager(application);
      this.manager = new TeleportManager(player, application, this.particles);
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

   public TeleportManager getManager() {
      return this.manager;
   }

   public Player getPlayer() {
      return this.player;
   }

   public void getList() {
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
      String builder = String.format("%s%s", bar1 ? YELLOW : ChatColor.DARK_GRAY, bar1 ? "■" : "-") + String.format("%s%s", bar2 ? YELLOW : ChatColor.DARK_GRAY, bar2 ? "■" : "-") + String.format("%s%s", bar3 ? YELLOW : ChatColor.DARK_GRAY, bar3 ? "■" : "-") + String.format("%s%s", bar4 ? YELLOW : ChatColor.DARK_GRAY, bar4 ? "■" : "-");
      this.player.sendMessage(String.format("Anchor: %s%s %s[%s%s]", hasAnchor ? (charges > 0 ? GREEN : GOLD) : RED, hasAnchor ? (charges > 0 ? "set" : "empty") : "not set", GREY, builder, GREY));
      this.player.sendMessage("");
      this.player.sendMessage(String.format("%sTeleport list", PURPLE));
      this.player.sendMessage("------------------------------");
      System.out.println(this.list);
      if (!this.list.isEmpty()) {
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
   }

   public void set(String raw) {
      String name = raw.toLowerCase();
      if (name.isEmpty()) {
         this.sendMessage(String.format("%sPlease enter a valid name!", RED));
         return;
      }
      if (!name.matches("^[a-z]*$")) {
         this.sendMessage(String.format("%sYou can only use letters in the name!", RED));
         return;
      }
      Location location = this.player.getLocation();
      Telepoint telepoint = Telepoint.create(name, Telepoint.Type.CUSTOM, location);
      if (this.list.containsKey(name)) {
         this.list.put(telepoint.getName(), telepoint);
         ParticleManager.highlightBlock(location, Color.ORANGE);
         this.sendMessage(String.format("Teleport %s%s%s updated!", PURPLE, name, RESET));
         return;
      }
      if ((this.list.size() + 1) <= 5) {
         this.list.put(telepoint.getName(), telepoint);
         ParticleManager.highlightBlock(location, Color.GREEN);
         this.sendMessage(String.format("Teleport %s%s%s set!", PURPLE, name, RESET));
         return;
      }
      this.player.sendMessage(String.format("%sYou've reached the maximum of 5 teleports!", RED));
      this.player.sendMessage(String.format("%sYou can overwrite existing teleports with %s/tp set <name>", ITALIC, PURPLE));
      this.player.sendMessage(String.format("%sor delete teleports with %s/tp remove <name>", ITALIC, RED));
   }

   public void to(String name) {
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
      this.manager.start(telepoint, false);
   }

   public void test(String name) {
      Telepoint telepoint = this.list.get(name);
      if (telepoint == null) {
         this.sendMessage(String.format("Teleport %s%s%s not found!", PURPLE, name, RESET));
         return;
      }
      telepoint.getSafeLocation();
   }

   public boolean remove(String name) {
      this.list.remove(name);
      this.sendMessage(String.format("Teleport %s%s%s deleted%s!", PURPLE, name, RED, RESET));
      return true;
   }

   public List<String> names() {
      return new ArrayList<>(this.list.keySet());
   }

   private void sendMessage(String message) {
      this.player.sendActionBar(Component.text(message));
   }

   public void saveTelepoints() {
      List<Telepoint> telepoints = new ArrayList<>();
      if (this.back != null) telepoints.add(this.back);
      if (this.home != null) telepoints.add(this.home);
      if (this.bed != null) telepoints.add(this.bed.getTelepoint());
      if (this.anchor != null) telepoints.add(this.anchor.getTelepoint());
      telepoints.addAll(this.list.values());
      if (!this.file.saveTeleports(this.player, telepoints)) {
         LOGGER.warning(String.format("Telepoints of %s couldn't be saved!", this.player.getName()));
      }
      LOGGER.info(String.format("Telepoints of %s saved!", this.player.getName()));
      this.manager.disable();
   }

   public void setBack(Location location) {
      this.back = Telepoint.create("Back", Telepoint.Type.BACK, location);
   }

   public void toBack() {
      if (this.back == null) {
         this.sendMessage("You haven't teleported yet.");
         return;
      }
      this.manager.teleport(this.back.getLocation());
   }

   public void setHome() {
      Location location = this.player.getLocation();
      this.home = Telepoint.create("Your home", Telepoint.Type.HOME, location);
      this.player.sendMessage(String.format("You can use %s/home%s to teleport to %s%s.", PURPLE, RESET, this.home.getTitle().toLowerCase(), RESET));
   }

   public void testHome() {
      if (this.home == null) {
         return;
      }
      this.home.getSafeLocation();
   }

   public void toHome() {
      if (this.home == null) {
         this.player.sendMessage(String.format("You don't have a%s home%s yet! Use %s/home set%s to set one.", BLUE, RESET, BLUE, RESET));
         return;
      }
      this.manager.teleport(this.home);
      this.manager.sendArrivalTitle(this.home);
   }

   public boolean setBed(Location location) {
      Telepoint telepoint = Telepoint.create("Your bed", Telepoint.Type.BED, location);
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

   public void toBed() {
      if (this.bed == null) {
         this.player.sendMessage(String.format("You don't have a%s bed%s yet. Right-click on a bed to set your bed-spawn point.", GOLD, RESET));
         return;
      }
      if (this.bed.isBroken()) {
         this.player.sendMessage(String.format("Your a%s bed%s is broken. Right-click on a new bed to set your bed-spawn point.", GOLD, RESET));
         return;
      }
      Telepoint bed = this.bed.getTelepoint();
      if (this.manager.teleport(bed)) {
         this.manager.sendArrivalTitle(bed);
         return;
      }
      this.sendMessage(String.format("Can't find your%s bed%s.", GOLD, RESET));
   }

   public void brokeBed(Block block, Player breaker) {
      if (this.bed == null) {
         return;
      }
      Telepoint telepoint = Telepoint.create("Possible Bed", Telepoint.Type.BED, block.getLocation(), block.getWorld());
      if (!this.bed.getTelepoint().getLocation().equals(telepoint.getLocation())) {
         return;
      }
      if (!this.player.getUniqueId().equals(breaker.getUniqueId())) {
         this.player.sendMessage(String.format("Your%s bed%s is broken by %s%s%s...", GOLD, RESET, RED, breaker.getName(), RESET));
      }
      this.player.sendMessage(String.format("%s/bed%s doesn't work anymore until you right-click a new bed.", GOLD, RESET));
   }

   public void setAnchor(Location location) {
      Telepoint telepoint = Telepoint.create("Your respawn anchor", Telepoint.Type.ANCHOR, location);
      if (this.anchor != null && this.anchor.getTelepoint().getLocation().equals(telepoint.getLocation())) {
         // Anchor already clicked on
         return;
      }
      this.anchor = new AnchorPoint(telepoint);
      this.player.sendMessage(Telepoint.getTitle(location, "Respawn Anchor") + " location saved!");
      this.sendMessage(String.format("You can use %s/anchor%s to teleport to this%s respawn anchor%s.", PURPLE, RESET, PURPLE, RESET));
      this.anchor.newAnchorEffect();
      World world = location.getWorld();
      if (world == null) {
         return;
      }
      world.playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1, 1);
   }

   public void toAnchor() {
      if (this.anchor == null) {
         this.sendMessage(String.format("You don't have a%s respawn anchor%s yet!", PURPLE, RESET));
         return;
      }
      if (this.anchor.isBroken()) {
//         this.player.sendMessage(String.format("Your%s respawn anchor%s is broken.", PURPLE, RESET));
         this.sendMessage(String.format("%s%s/anchor %s%sis not possible until you place and charge a new one.", PURPLE, ITALIC, GREY, ITALIC));
         return;
      }
      if (this.anchor.hasNoCharges()) {
//         this.player.sendMessage(String.format("Your%s respawn anchor%s is out of%s charges%s.", PURPLE, RESET, YELLOW, RESET));
         this.sendMessage(String.format("%s%s/anchor %s%sis not possible without charges.", PURPLE, ITALIC, GREY, ITALIC));
         return;
      }
      if (!this.manager.teleport(this.anchor.getTelepoint())) {
         this.sendMessage(String.format("Can't teleport you to your%s respawn anchor%s.", PURPLE, RESET));
         return;
      }
      this.manager.sendArrivalTitle(this.anchor.getTelepoint());
      this.anchor.removeCharge();
      int charges = this.anchor.getCharges();
      int max = this.anchor.getMaximumCharges();
      if (charges < 2) {
         this.player.sendMessage(String.format("%sWarning!%s Your%s respawn anchor%s is almost out of charges... [%s%s/%s%s]", RED, RESET, PURPLE, RESET, GREY, charges, max, RESET));
      }
   }

   public void brokeAnchor(Block block, Player breaker) {
      if (this.anchor == null) {
         return;
      }
      Telepoint telepoint = Telepoint.create("Possible Anchor", Telepoint.Type.ANCHOR, block.getLocation(), block.getWorld());
      if (!this.anchor.getTelepoint().getLocation().equals(telepoint.getLocation())) {
         return;
      }
      if (!this.player.getUniqueId().equals(breaker.getUniqueId())) {
         this.player.sendMessage(String.format("Your%s respawn anchor%s was broken by %s%s%s.", PURPLE, RESET, RED, breaker.getName(), RESET));
      }
      this.player.sendMessage(String.format("%s/anchor %sis not possible until you place and charge a new one.", PURPLE, RESET));
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
      return !this.requests.isEmpty();
   }

   public boolean hasRequest(UUID uuid) {
      return this.requests.contains(uuid);
   }
}
