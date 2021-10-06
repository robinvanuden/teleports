package com.varestorm.plugins.teleports.client;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.Teleport;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import org.bukkit.*;
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
    static ChatColor BLUE = ChatColor.BLUE, GOLD = ChatColor.GOLD, AQUA = ChatColor.DARK_AQUA, RED = ChatColor.RED, ITALIC = ChatColor.ITALIC, BOLD = ChatColor.BOLD, RESET = ChatColor.RESET;

    private final Player player;
    private final Application application;
    protected Telepoint home, bed, anchor, back;
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
        this.bar = Bukkit.createBossBar("Teleport cooldown", BarColor.BLUE, BarStyle.SOLID);
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
                    this.bed = telepoint;
                }
                if (telepoint.getType().equals(Telepoint.Type.ANCHOR)) {
                    this.anchor = telepoint;
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
            this.player.sendMessage(String.format("%sPlease enter a valid name!", RED));
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
        this.player.sendMessage(String.format("%sor delete teleports with %s/tp remove <name>", ITALIC, BLUE));
        return true;
    }

    public boolean instant(String name) {
        if (this.isTeleporting) {
            this.player.sendMessage("You are still teleporting, please wait...");
            return false;
        }
        if (this.hasCooldown) {
            this.player.sendMessage(String.format("The %sTeleporter%s is recharging after interdimensional jump. Please wait...", BLUE, RESET));
            return false;
        }
        Telepoint telepoint = null;
        boolean useEnderpearls = true;
        switch (name) {
            case "back":
                useEnderpearls = false;
                telepoint = this.back;
                break;
            case "home":
                telepoint = this.home;
                break;
            case "bed":
                telepoint = this.bed;
                break;
            case "anchor":
                telepoint = this.anchor;
                break;
        }
        if (useEnderpearls && this.hasNoEnderPearls()) {
            this.player.sendMessage(String.format("You need %sEnder pearls%s to teleport!", AQUA, RESET));
            return false;
        }
        if (telepoint == null) {
            this.player.sendMessage("Teleport not found!");
            return false;
        }
        if (telepoint.getSafeLocation() == null) {
            this.player.sendMessage(String.format("Teleport %s not safe!", telepoint.getTitle()));
            return false;
        }
        if (Teleport.instant(telepoint, this)) {
            this.player.sendMessage(String.format("Teleported to %s%s%s!", BLUE, telepoint.getTitle(), RESET));
            return true;
        }
        return false;
    }

    public boolean to(String name) {
        if (this.isTeleporting) {
            this.player.sendMessage("You are still teleporting, please wait...");
            return false;
        }
        if (this.hasCooldown) {
            this.player.sendMessage("You are still on cooldown, please wait...");
            return false;
        }
        if (this.hasNoEnderPearls()) {
            this.player.sendMessage(String.format("You need %sEnder pearls%s to teleport!", AQUA, RESET));
            return false;
        }
        Telepoint telepoint = this.list.get(name);
        if (telepoint == null) {
            this.player.sendMessage(String.format("Teleport %s%s%s not found!", BLUE, name, RESET));
            return false;
        }
        if (!telepoint.getType().equals(Telepoint.Type.CUSTOM)) {
            this.player.sendMessage(String.format("Teleport %s%s%s not found!", BLUE, name, RESET));
            return false;
        }
        if (telepoint.getSafeLocation() == null) {
            this.player.sendMessage(String.format("Teleport %s%s%s not safe!", BLUE, name, RESET));
            return false;
        }

        return this.start(telepoint);
    }

    private boolean start(Telepoint telepoint) {
        this.player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 400, 1000, false));
        this.isTeleporting = true;
        if (telepoint != null) {
            BukkitScheduler scheduler = this.application.getServer().getScheduler();
            final int[] counter = {0};
            boolean interdimensional = false;
            String worldName = Telepoint.getTitle(telepoint.getWorld().getEnvironment(), "this dimension");
            World world = this.player.getLocation().getWorld();
            if (world != null) {
                World.Environment environment = world.getEnvironment();
                worldName = Telepoint.getTitle(environment, "this dimension");
                World.Environment environment2 = telepoint.getWorld().getEnvironment();
                interdimensional = (!environment.equals(environment2));
                if (interdimensional) {
                    switch (environment2) {
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
                    worldName = Telepoint.getTitle(environment2, worldName);
                }
            }
            String title = String.format(interdimensional ? "%s%sInterdimensional jump" : "%s%sTeleporting!", (!interdimensional ? BLUE : GOLD), BOLD);


            final boolean finalInterdimensional = interdimensional;
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
                                        0, 21, 0
                                );
                                this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 5);
                                scheduler.runTaskLater(this.application, () -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 2), 5);
                                scheduler.runTaskLater(this.application, () -> this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 7), 5);
                                if (finalInterdimensional) {
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
        return true;
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
                    String.format("%s%sCancelled!", RED, BOLD),
                    "You moved!",
                    0, 22, 0
            );
        }
    }

    public void setCooldown() {
        this.hasCooldown = true;
        this.bar.addPlayer(this.player);

        BukkitScheduler scheduler = this.application.getServer().getScheduler();
        final int[] count = {0};
        this.cooldownId = scheduler.scheduleSyncRepeatingTask(this.application, () -> {
            double percentage = 1.0 - ((double) (count[0] / 20) / 10);
            this.bar.setProgress(percentage);
            System.out.println(percentage);
            if (count[0] > 200) {
                this.hasCooldown = false;
                scheduler.cancelTask(this.cooldownId);
                System.out.println("CANCELLED!");
                this.bar.removePlayer(this.player);
                this.bar.removeAll();
            }
            count[0]++;
        }, 1, 1);
    }

    public boolean remove(String name) {
        this.list.remove(name);
        this.player.sendMessage(String.format("Teleport %s%s%s deleted%s!", BLUE, name, RED, RESET));
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

    public boolean removeEnderPearl() {
        ItemStack enderPearl = this.getEnderPearls();
        if (enderPearl == null) {
            return false;
        }
        enderPearl.setAmount(enderPearl.getAmount() - 1);
        return true;
    }

    protected ItemStack getEnderPearls() {
        int slot = this.player.getInventory().first(Material.ENDER_PEARL);
        return this.player.getInventory().getItem(slot);
    }

    public void saveTelepoints() {
        List<Telepoint> telepoints = new ArrayList<>();
        telepoints.add(this.back);
        telepoints.add(this.home);
        telepoints.add(this.bed);
        telepoints.add(this.anchor);
        telepoints.addAll(this.list.values());
        if (!this.file.saveTeleports(this.player, telepoints)) {
            LOGGER.warning(String.format("Telepoints of %s couldn't be saved!", this.player.getName()));
        }
    }

    public void setBack(Location location) {
        this.back = new Telepoint("back", Telepoint.Type.BACK, location.getWorld(), location);
    }
}
