package com.varestorm.plugins.teleports.client;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.Teleport;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;
import java.util.logging.Logger;

public class TeleportClient {

    private static Logger LOGGER;
    static ChatColor BLUE = ChatColor.BLUE, GOLD = ChatColor.GOLD, PURPLE = ChatColor.DARK_PURPLE, AQUA = ChatColor.DARK_AQUA, RED = ChatColor.RED, ITALIC = ChatColor.ITALIC, BOLD = ChatColor.BOLD, RESET = ChatColor.RESET;

    private final Player player;
    private final Application application;
    protected Telepoint home, back;
    protected BedPoint bed;
    protected AnchorPoint anchor;
    protected final Map<String, Telepoint> list;
    protected final List<UUID> requests;
    protected long anchor_cooldown = 0, cross_world_cooldown = 0;
    private final TeleportFile file;
    private final BossBar bar;
    private boolean isTeleporting, hasCooldown;
    private int teleportId, cooldownId;

    public TeleportClient(Player player, Application application) {
        LOGGER = application.getLogger();
        LOGGER.info(player.getDisplayName() + ".teleports loaded");
        this.player = player;
        this.application = application;
        this.bar = Bukkit.createBossBar("Recharging teleport particles", BarColor.YELLOW, BarStyle.SOLID);
        this.isTeleporting = false;
        this.hasCooldown = false;
        this.list = new HashMap<>();
        this.requests = new ArrayList<>();
        this.file = new TeleportFile(application);
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
                }
                if (telepoint.getType().equals(Telepoint.Type.CUSTOM)) {
                    this.list.put(telepoint.getName(), telepoint);
                }
            }
        }
    }

    public boolean set(String raw) {
        String name = raw.toLowerCase().replaceAll("[$&+,:;=?@#|'<> .-^*()%!]", "");
        if (name.isEmpty()) {
            this.sendMessage(String.format("%sPlease enter a valid name!", RED));
            return true;
        }
        Location location = this.player.getLocation();
        location.setX(location.getBlockX() + 0.5);
        location.setY(location.getBlockY() + 0.5);
        location.setZ(location.getBlockZ() + 0.5);
        Telepoint telepoint = new Telepoint(name, Telepoint.Type.CUSTOM, location.getWorld(), location);
        if (this.list.containsKey(name)) {
            this.list.put(name, telepoint);
            this.player.sendMessage(String.format("Teleport %s%s%s updated!", BLUE, name, RESET));
            return true;
        }
        if ((this.list.size() + 1) <= 5) {
            this.list.put(name, telepoint);
            this.player.sendMessage(String.format("Teleport %s%s%s set!", BLUE, name, RESET));
            return true;
        }

        this.player.sendMessage(String.format("%sYou've reached the maximum of 5 teleports!", RED));
        this.player.sendMessage(String.format("%sYou can overwrite existing teleports with %s/tp set <name>", ITALIC, BLUE));
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
        Telepoint telepoint;
        switch (name) {
            case "home":
                telepoint = this.home;
                break;
            case "bed":
                telepoint = this.bed.getTelepoint();
                break;
            case "anchor":
                telepoint = this.anchor.getTelepoint();
                break;
            default:
                this.sendMessage("Teleport doesn't exist.");
                return;
        }
        if (this.hasNoEnderPearls()) {
            this.sendMessage(String.format("You need %sender pearls%s to teleport!", AQUA, RESET));
            return;
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
        if (telepoint.getSafeLocation() == null) {
            this.sendMessage(String.format("Teleport %s is not safe!", telepoint.getTitle()));
            return;
        }
        if (telepoint.isInterdimensional(this.player)) {
            this.setCooldown();
        }
        if (name.equals("anchor")) {
            if (this.anchor.hasNoCharges()) {
                this.sendMessage(String.format("Your %sRespawn Anchor%s is out of charges... Teleporting to your anchor is not possible.", PURPLE, RESET));
                return;
            }
            System.out.println("anchor");
            this.anchor.removeCharge();
        }
        if (!Teleport.instant(telepoint, this)) {
            this.sendMessage(String.format("Can't teleport you to %s", telepoint.getTitle().toLowerCase()));
            return;
        }
        this.removeEnderPearl();
        if (this.anchor.hasNoCharges()) {
            this.sendMessage(String.format("Your %sRespawn Anchor%s is out of charges... Teleporting to your anchor is not possible.", PURPLE, RESET));
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
        if (!Teleport.instant(this.back, this)) {
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
        if (this.hasNoEnderPearls()) {
            this.sendMessage(String.format("You need %sEnder pearls%s to teleport!", AQUA, RESET));
            return;
        }
        Telepoint telepoint = this.list.get(name);
        if (telepoint == null) {
            this.sendMessage(String.format("Teleport %s%s%s not found!", BLUE, name, RESET));
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

    private void start(Telepoint telepoint) {
        this.player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 400, 1000, false));
        this.isTeleporting = true;
        if (telepoint != null) {
            BukkitScheduler scheduler = this.application.getServer().getScheduler();
            final int[] counter = {0};
            boolean interdimensional = telepoint.isInterdimensional(this.player);
            String worldName = Telepoint.getTitle(telepoint.getWorld().getEnvironment(), "this dimension");
            World world = this.player.getLocation().getWorld();
            if (world != null) {
                worldName = Telepoint.getTitle(world.getEnvironment(), "this dimension");
                if (interdimensional) {
                    switch (telepoint.getWorld().getEnvironment()) {
                        case NORMAL:
                            worldName = "the overworld";
                            break;
                        case NETHER:
                            worldName = "the Nether";
                            break;
                        case THE_END:
                            worldName = "the End";
                            break;
                        case CUSTOM:
                            worldName = "a different place";
                            break;
                    }
                    worldName = Telepoint.getTitle(telepoint.getWorld().getEnvironment(), worldName);
                }
            }
            String title = String.format("%s%sTeleporting!", (!interdimensional ? BLUE : GOLD), BOLD);

            final String finalWorldName = worldName;
            this.teleportId = scheduler.scheduleSyncRepeatingTask(this.application, () -> {
//                System.out.println(counter[0]);
                int time = 5 - (counter[0] / 20);
                switch (counter[0]) {
                    case 1:
                        this.player.sendTitle(
                                title,
                                String.format("Jumping to %s", finalWorldName),
                                10, 40, 0
                        );
                        break;
                    case 2:
                        this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1);
                        break;
                    case 6:
                        this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 4);
                        break;
                    case 20:
                    case 40:
                    case 60:
                    case 80:
                    case 100:
                        int countdown = time + 1;
                        this.player.sendTitle(
                                title,
                                String.format("Jumping in %d %s!", countdown, "second" + (countdown == 1 ? "" : "s")),
                                0, 22, 0
                        );
                        this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1);
                        break;
                    case 120:
                        if (this.stop()) {
                            if (Teleport.to(telepoint, this)) {
                                this.player.sendTitle(
                                        title,
                                        String.format("You've arrived at %s", telepoint.getTitle()),
                                        0, 20, 30
                                );
                                Application.runLaterAsync(() -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 3), 5);
                                Application.runLaterAsync(() -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 2), 10);
                                Application.runLaterAsync(() -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 7), 15);
                                if (interdimensional) {
                                    this.setCooldown();
                                }
                            }
                        } else {
                            this.cancel();
                        }
                        break;
                }
                counter[0]++;
            }, 10, 1);
        }
    }

    public boolean stop() {
        if (this.isTeleporting) {
            this.player.removePotionEffect(PotionEffectType.BLINDNESS);
            this.application.getServer().getScheduler().cancelTask(this.teleportId);
            this.isTeleporting = false;
            return true;
        }
        return false;
    }

    public void cancel() {
        if (this.stop()) {
            this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, -10);
            this.player.sendTitle(
                    String.format("%s%sAborted!", RED, BOLD),
                    "You moved!",
                    0, 22, 0
            );
        }
    }

    public void setCooldown() {
        this.hasCooldown = true;
        this.bar.setProgress(0);
        this.bar.addPlayer(this.player);

        BukkitScheduler scheduler = this.application.getServer().getScheduler();
        final int[] count = {0};
        this.cooldownId = scheduler.scheduleSyncRepeatingTask(this.application, () -> {
            switch (count[0]) {
                case 20: // 1
                case 40: // 2
                case 60: // 3
                case 80: // 4
                case 100: // 5
                case 120: // 6
                case 140: // 7
                case 160: // 8
                case 180: // 9
                case 200: // 10
                    double percentage = (double) (count[0] / 20) / 10;
                    this.bar.setProgress(percentage);
                    break;
                case 202:
                    this.hasCooldown = false;
                    scheduler.cancelTask(this.cooldownId);
                    this.sendMessage(GOLD + "You can teleport to other dimensions again!");
                    this.bar.removePlayer(this.player);
                    this.bar.removeAll();
                    break;
            }
            this.particle();
            count[0]++;
        }, 1, 1);
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
        this.sendMessage(String.format("Teleport %s%s%s deleted%s!", BLUE, name, RED, RESET));
        return true;
    }

    public List<String> names() {
        return new ArrayList<>(this.list.keySet());
    }

    public Application getApplication() {
        return this.application;
    }

    public Player getPlayer() {
        return this.player;
    }

    public boolean hasNoEnderPearls() {
        PlayerInventory inventory = this.player.getInventory();
        return !inventory.contains(Material.ENDER_PEARL);
    }

    public void removeEnderPearl() {
        ItemStack enderPearl = this.getEnderPearls();
        if (enderPearl != null) {
            enderPearl.setAmount(enderPearl.getAmount() - 1);
        }
    }

    protected ItemStack getEnderPearls() {
        int slot = this.player.getInventory().first(Material.ENDER_PEARL);
        return this.player.getInventory().getItem(slot);
    }

    private void sendMessage(String message) {
        this.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
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
    }

    public void setBack(Location location) {
        this.back = new Telepoint("Back", Telepoint.Type.BACK, location.getWorld(), location);
    }

    public void setHome() {
        Location location = this.player.getLocation();
        this.home = new Telepoint("Home", Telepoint.Type.HOME, location.getWorld(), location);
        this.sendMessage(Telepoint.getTitle(location, "Home") + " location saved!");
        this.player.sendMessage(String.format("You can use %s/home%s to teleport to your %s%s.", AQUA, RESET, this.home.getTitle(), RESET));
        Teleport.smoke(this.home.getSafeLocation());
    }

    public boolean setBed(Location location) {
        Telepoint telepoint = new Telepoint("bed", Telepoint.Type.BED, location.getWorld(), location);
        if (this.bed != null && this.bed.getTelepoint().getSafeLocation().equals(telepoint.getSafeLocation())) {
            // Bed already clicked on
            return false;
        }
        this.bed = new BedPoint(telepoint);
        this.sendMessage(Telepoint.getTitle(location, "Bed") + " location saved!");
        this.player.sendMessage(String.format("You can use %s/bed%s to teleport to this %s%s.", GOLD, RESET, telepoint.getTitle(), RESET));
        Location bed = telepoint.getSafeLocation();
        bed.setY(bed.getBlockY() - 1);
        Teleport.particle(bed);
        return true;
    }

    public void setAnchor(Location location) {
        Telepoint telepoint = new Telepoint("anchor", Telepoint.Type.ANCHOR, location.getWorld(), location);
        if (this.anchor != null && this.anchor.getTelepoint().getSafeLocation().equals(telepoint.getSafeLocation())) {
            // Anchor already clicked on
            return;
        }
        this.anchor = new AnchorPoint(telepoint);
        this.player.sendMessage(Telepoint.getTitle(location, "Respawn Anchor") + " location saved!");
        this.sendMessage(String.format("You can use %s/anchor%s to teleport to this %s%s.", PURPLE, RESET, telepoint.getTitle(), RESET));
        Location a = telepoint.getSafeLocation();
        a.setY(a.getBlockY() - 1);
        int charges = this.anchor.getCharges();
        int count = (charges * 100) + 1;
        Teleport.particle(a, count);
        Color color = Color.PURPLE;
        switch (charges) {
            case 0:
                color = Color.fromRGB(60, 0, 172);
                break;
            case 1:
            case 2:
                color = Color.fromRGB(87, 6, 195);
                break;
            case 3:
            case 4:
                color = Color.fromRGB(139, 45, 220);
        }

        Teleport.smoke(telepoint.getSafeLocation(), color, true, count);
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1, 1);
    }

    public void testHome() {
        if (this.home == null || this.home.getSafeLocation() == null) {
            this.player.sendMessage("No home found...");
        }
        Teleport.particle(this.home.getSafeLocation());
    }

    public boolean brokeBed(Block block, Player breaker) {
        if (this.player.getUniqueId().equals(breaker.getUniqueId())) {
            return false;
        }
        Telepoint telepoint = new Telepoint("Possible Bed", Telepoint.Type.BED, block.getWorld(), block.getLocation());
        if (this.bed.getTelepoint().getSafeLocation().equals(telepoint.getSafeLocation())) {
            this.player.sendMessage(String.format("%s%s%s broke your bed... %s/bed%s doesn't work anymore until you find a new bed.", RED, breaker.getDisplayName(), RESET, GOLD, RESET));
            return true;
        }
        return false;
    }

    public boolean brokeAnchor(Block block, Player breaker) {
        if (this.player.getUniqueId().equals(breaker.getUniqueId())) {
            return false;
        }
        Telepoint telepoint = new Telepoint("Possible Anchor", Telepoint.Type.ANCHOR, block.getWorld(), block.getLocation());
        if (this.anchor.getTelepoint().getSafeLocation().equals(telepoint.getSafeLocation())) {
            this.player.sendMessage(String.format("%s%s%s broke your respawn anchor... %s/anchor%s doesn't work anymore until you find a new anchor.", RED, breaker.getDisplayName(), RESET, PURPLE, RESET));
            return true;
        }
        return false;
    }
}
