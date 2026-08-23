package com.varestorm.plugins.teleports.client;

import com.varestorm.plugins.teleports.ParticleManager;
import com.varestorm.plugins.teleports.commands.TeleportCommand;
import com.varestorm.plugins.teleports.teleporting.AnchorPoint;
import com.varestorm.plugins.teleports.teleporting.BedPoint;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.logging.Logger;

public class TeleportClient {

  private static Logger LOGGER;

  private final Player player;
  private Telepoint home, back;
  private BedPoint bed;
  private AnchorPoint anchor;
  private final Map<String, Telepoint> list;
  private final List<UUID> requests;
  private final TeleportFile file;
  private final ParticleManager particles;
  private final TeleportManager manager;
  private final TeleportMessageManager messageManager;

  public TeleportClient(Player player, Plugin application) {
    LOGGER = application.getLogger();
    this.player = player;
    this.list = new HashMap<>();
    this.requests = new ArrayList<>();
    this.file = new TeleportFile(application.getDataFolder());
    this.particles = new ParticleManager();
    this.manager = new TeleportManager(player);
    this.messageManager = new TeleportMessageManager(player);
    this.loadTelepoints();
  }

  private void loadTelepoints() {
    List<Telepoint> telepoints = this.file.loadTeleports(this.player);
    if (telepoints == null) {
      return;
    }
    for (Telepoint telepoint : telepoints) {
      switch (telepoint.getType()) {
        case BACK -> this.back = telepoint;
        case HOME -> this.home = telepoint;
        case BED -> this.bed = new BedPoint(telepoint);
        case CUSTOM -> this.list.put(telepoint.getName(), telepoint);
        case ANCHOR -> {
          this.anchor = new AnchorPoint(telepoint);
          this.anchor.resetCharges();
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
    this.sendMessage(
      Component.text("Teleports", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
        .appendNewline()
        .append(Component.text("------------------------------"))
    );
    boolean hasHome = this.home != null;
    boolean hasBed = this.bed != null;
    if (hasBed) {
      hasBed = !this.bed.isBroken();
    }
    boolean hasAnchor = this.anchor != null;
    if (hasAnchor) {
      hasAnchor = !this.anchor.isBroken();
    }
    this.sendMessage(Component.text("Home: ")
      .append(Component.text(hasHome ? "set" : "not set", hasHome ? NamedTextColor.GREEN : NamedTextColor.RED))
      .append(Component.text("!")));
    this.sendMessage(Component.text("Bed: ")
      .append(Component.text(hasBed ? "set" : "not set", hasBed ? NamedTextColor.GREEN : NamedTextColor.RED))
      .append(Component.text("!")));

    int charges = 0;
    if (hasAnchor) {
      charges = this.anchor.getCharges();
    }
    boolean bar1 = charges > 0;
    boolean bar2 = charges > 1;
    boolean bar3 = charges > 2;
    boolean bar4 = charges > 3;

    Component filled = Component.text("■", NamedTextColor.YELLOW);
    Component empty = Component.text("-", NamedTextColor.DARK_GRAY);

    this.sendMessage(
      Component.text("Anchor: [")
        .append(bar1 ? filled : empty)
        .append(bar2 ? filled : empty)
        .append(bar3 ? filled : empty)
        .append(bar4 ? filled : empty)
        .append(Component.text("]"))
        .appendNewline());

    if (this.list.isEmpty()) {
      this.sendMessage(Component.text("Your list is empty. Use ")
        .append(Component.text("/tp set [name]", NamedTextColor.DARK_PURPLE))
        .append(Component.text(" to create a teleport.")));
      return;
    }
    int count = 1;
    Component list = Component.text("Teleport list", NamedTextColor.DARK_PURPLE)
      .appendNewline()
      .append(Component.text("------------------------------"));
    for (Map.Entry<String, Telepoint> entry : this.list.entrySet()) {
      Telepoint telepoint = entry.getValue();
      System.out.println(telepoint);
      list = list.appendNewline()
        .append(Component.text("%s. ".formatted(count), NamedTextColor.WHITE))
        .append(telepoint.getTitle());
      count++;
    }
    this.sendMessage(list);
  }

  public void set(String raw) {
    String name = raw.toLowerCase();
    if (name.isEmpty()) {
      this.messageManager.sendError("Please enter a valid name!");
      return;
    }
    if (!name.matches("^[a-z]*$")) {
      this.messageManager.sendError("You can only use letters in the name!");
      return;
    }
    Location location = this.player.getLocation();
    Telepoint telepoint = Telepoint.create(name, Telepoint.Type.CUSTOM, location);
    if (this.list.containsKey(name)) {
      this.list.put(telepoint.getName(), telepoint);
      ParticleManager.highlightBlock(location, Color.ORANGE);
      this.sendActionBar(Component.text("Teleport %s updated!".formatted(name), NamedTextColor.BLUE));
      return;
    }
    if ((this.list.size() + 1) <= 5) {
      this.list.put(telepoint.getName(), telepoint);
      ParticleManager.highlightBlock(location, Color.GREEN);
      this.sendActionBar(Component.text("Teleport %s set!".formatted(name), NamedTextColor.GREEN));
      return;
    }
    this.sendMessage(Component.text("You've reached the maximum of 5 teleports!", NamedTextColor.RED));
    this.sendMessage(Component.text("You can overwrite existing teleports with ", NamedTextColor.WHITE, TextDecoration.ITALIC)
      .append(Component.text("/tp set <name>", NamedTextColor.DARK_PURPLE)));
    this.sendMessage(Component.text("or delete teleports with ", NamedTextColor.WHITE, TextDecoration.ITALIC)
      .append(Component.text("/tp remove <name>", NamedTextColor.RED)));
  }

  public void to(String name, boolean forced) {
    Telepoint telepoint = this.list.get(name);
    if (telepoint == null) {
      this.messageManager.sendError("Teleport not found!");
      return;
    }
    if (!telepoint.getType().equals(Telepoint.Type.CUSTOM)) {
      this.messageManager.sendError("Teleport not found!");
      return;
    }
    if (!forced && telepoint.getSafeLocation() == null) {
      this.messageManager.sendError("Teleport is not found!");
      return;
    }
    this.manager.start(telepoint, false, () -> {
      this.teleportWithTitle(telepoint);
    });
  }

  public boolean toSpawn(World world) {
    Telepoint spawn = Telepoint.create("Spawn", Telepoint.Type.CUSTOM, world.getSpawnLocation(), world);
    if (spawn.isInterdimensional(this.getPlayer())) {
      this.manager.start(spawn, true, () -> {
        this.teleportWithTitle(spawn);
      });
      return true;
    }
    return this.teleportWithTitle(spawn);
  }

  public boolean remove(String name) {
    this.messageManager.sendError("Teleport %s deleted!".formatted(name));
    return this.list.remove(name) != null;
  }

  public List<String> names() {
    return new ArrayList<>(this.list.keySet());
  }

  public void saveTelepoints() {
    List<Telepoint> telepoints = new ArrayList<>();
    if (this.back != null) telepoints.add(this.back);
    if (this.home != null) telepoints.add(this.home);
    if (this.bed != null) telepoints.add(this.bed.telepoint());
    if (this.anchor != null) telepoints.add(this.anchor.getTelepoint());
    telepoints.addAll(this.list.values());
    if (!this.file.saveTeleports(this.player, telepoints)) {
      LOGGER.warning(String.format("Telepoints of %s couldn't be saved!", this.player.getName()));
    }
    LOGGER.info(String.format("Telepoints of %s saved!", this.player.getName()));
    this.manager.disable();
  }

  public void setBack(Location location) {
    this.back = Telepoint.create("back", Telepoint.Type.BACK, location);
  }

  public void toBack() {
    if (this.back == null) {
      this.messageManager.sendError("You haven't teleported yet.");
      return;
    }
    this.teleport(this.back);
  }

  public void setHome() {
    Location location = this.player.getLocation();
    this.home = Telepoint.create("your home", Telepoint.Type.HOME, location);
    this.sendActionBar(Component.text("Home set!", NamedTextColor.GREEN)
      .append(Component.text(" Use ", NamedTextColor.WHITE))
      .append(TeleportCommand.TeleportCommandTexts.home())
      .append(Component.text(" to teleport to ", NamedTextColor.WHITE))
      .append(this.home.getTitle())
    );
  }

  public void toHome() {
    if (this.home == null) {
      this.messageManager.sendError("You don't have a home yet. Use /home set.");
      return;
    }
    this.teleportWithTitle(this.home);
  }

  public boolean setBed(Location location) {
    Telepoint telepoint = Telepoint.create("your bed", Telepoint.Type.BED, location);
    if (this.bed != null && this.bed.telepoint().getLocation().equals(telepoint.getLocation())) {
      // Bed already clicked on
      return false;
    }
    this.bed = new BedPoint(telepoint);
    this.sendActionBar(Telepoint.getTitle(location, "Bed")
      .append(Component.text(" location saved!"))
    );
    this.sendMessage(Component.text("Use ")
      .append(TeleportCommand.TeleportCommandTexts.bed())
      .append(Component.text(" to teleport to this bed."))
    );
    this.particles.particle(telepoint.getLocation());
    return true;
  }

  public void toBed() {
    if (this.bed == null) {
      this.messageManager.sendError("You don't have abed yet. Right-click on a bed to set your bed-spawn point.");
      return;
    }
    if (this.bed.isBroken()) {
      this.messageManager.sendError("Your bed is broken. Right-click on a new bed to set your bed-spawn point.");
      return;
    }
    if (!this.teleportWithTitle(this.bed.telepoint())) {
      this.messageManager.sendError("Can't find your bed.");
    }
  }

  public void brokeBed(Block block, Player breaker) {
    if (this.bed == null) {
      return;
    }
    Telepoint telepoint = Telepoint.create("Possible Bed", Telepoint.Type.BED, block.getLocation(), block.getWorld());
    if (!this.bed.telepoint().getLocation().equals(telepoint.getLocation())) {
      return;
    }
    if (!this.player.getUniqueId().equals(breaker.getUniqueId())) {
      this.sendMessage(Component.text("Your bed is broken by %s...".formatted(breaker.getName())));
    }
    this.sendMessage((TeleportCommand.TeleportCommandTexts.bed())
      .append(Component.text(" doesn't work anymore until you right-click a new bed."))
    );
  }

  public void setAnchor(Location location) {
    Telepoint telepoint = Telepoint.create("your respawn anchor", Telepoint.Type.ANCHOR, location);
    if (this.anchor != null && this.anchor.getTelepoint().getLocation().equals(telepoint.getLocation())) {
      // Anchor already clicked on
      return;
    }
    this.anchor = new AnchorPoint(telepoint);
    this.sendMessage(Telepoint.getTitle(location, "Respawn Anchor").append(Component.text(" location saved!")));
    this.sendActionBar(Component.text("You can use /anchor to teleport to this respawn anchor."));
    this.anchor.newAnchorEffect();
    World world = location.getWorld();
    if (world == null) {
      return;
    }
    world.playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1, 1);
  }

  public void toAnchor() {
    if (this.anchor == null) {
      this.messageManager.sendError("You don't have a respawn anchor yet!");
      return;
    }
    if (this.anchor.isBroken()) {
      this.messageManager.sendError("Your respawn anchor is broken.");
      return;
    }
    if (this.anchor.hasNoCharges()) {
      this.messageManager.sendError("Your respawn anchor is out of charge!");
      return;
    }
    if (!this.teleportWithTitle(this.anchor.getTelepoint())) {
      this.messageManager.sendError("Can't teleport you to your respawn anchor.");
      return;
    }
    this.anchor.removeCharge();
    int charges = this.anchor.getCharges();
    int max = this.anchor.getMaximumCharges();
    if (charges < 2) {
      this.sendMessage(Component.text("Warning! Your respawn anchor is almost out of charges... [%s/%s]".formatted(charges, max), NamedTextColor.GOLD));
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
      this.messageManager.sendError("Your respawn anchor was broken by %s.".formatted(breaker.getName()));
    }
    this.sendMessage(
      TeleportCommand.TeleportCommandTexts.anchor()
        .append(Component.text(" is not possible until you place and charge a new one.", NamedTextColor.DARK_PURPLE))
    );
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

  private void sendMessage(Component component) {
    this.player.sendMessage(component);
  }

  private void sendActionBar(Component component) {
    this.player.sendActionBar(component);
  }

  public boolean teleportWithTitle(Telepoint telepoint) {
    this.messageManager.arrivalTitle(telepoint);
    return this.teleport(telepoint);
  }

  public boolean teleport(Telepoint telepoint) {
    Location location = telepoint.getSafeLocation();
    if (location == null) {
      this.messageManager.sendError("Teleport was not safe...");
      return false;
    }
    return this.teleport(location);
  }

  public boolean teleport(Location location) {
    Location oldLocation = this.player.getLocation().clone();
    this.player.teleportAsync(location).thenAcceptAsync(success -> {
      if (success) {
        // New location
        this.player.playSound(this.player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        Particle.PORTAL.builder().location(location).receivers(32, true);
        this.player.removePotionEffect(PotionEffectType.BLINDNESS);
        // Old location
        this.player.playSound(oldLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        Particle.PORTAL.builder().location(oldLocation).receivers(16, true);
        this.setBack(oldLocation);
      }
    });
    return true;
  }
}
