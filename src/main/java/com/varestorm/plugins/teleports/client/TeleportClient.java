package com.varestorm.plugins.teleports.client;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.Teleport;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.logging.Logger;

public class TeleportClient {

    private static Logger LOGGER;
    static ChatColor BLUE = ChatColor.BLUE, AQUA = ChatColor.DARK_AQUA, RED = ChatColor.RED, ITALIC = ChatColor.ITALIC, RESET = ChatColor.RESET;

    private final Player player;
    private final Application application;
    protected Telepoint home, bed, anchor, back;
    protected final Map<String, Telepoint> list;
    protected final List<UUID> requests;
    protected long anchor_cooldown = 0, cross_world_cooldown = 0;
    private final TeleportFile file;
    private BossBar bar;

    public boolean set(String raw) {
        String name = raw.toLowerCase().replaceAll("[$&+,:;=?@#|'<>.-^*()%!]", "");
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
        if (this.hasNoEnderPearls()) {
            this.player.sendMessage(String.format("You need %sEnder pearls%s to teleport!", AQUA, RESET));
            return true;
        }
        Telepoint telepoint;
        switch (name) {
            case "back":
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
            default:
                telepoint = null;
                break;
        }
        if (telepoint == null) {
            this.player.sendMessage(String.format("Teleport %s%s%s not found!", BLUE, name, RESET));
            return true;
        }
        if (telepoint.getSafeLocation() == null) {
            this.player.sendMessage(String.format("Teleport %s%s%s not safe!", BLUE, name, RESET));
            return true;
        }
        if (Teleport.instant(this.player, telepoint, this)) {
            this.player.sendMessage(String.format("Teleported to %s%s%s!", BLUE, telepoint.getTitle(), RESET));
        }
        return true;
    }

    public boolean to(String name) {
        if (this.hasNoEnderPearls()) {
            this.player.sendMessage(String.format("You need %sEnder pearls%s to teleport!", AQUA, RESET));
            return true;
        }
        Telepoint telepoint = this.list.get(name);
        if (telepoint == null) {
            this.player.sendMessage(String.format("Teleport %s%s%s not found!", BLUE, name, RESET));
            return true;
        }
        if (!telepoint.getType().equals(Telepoint.Type.CUSTOM)) {
            this.player.sendMessage(String.format("Teleport %s%s%s not found!", BLUE, name, RESET));
            return true;
        }
        if (telepoint.getSafeLocation() == null) {
            this.player.sendMessage(String.format("Teleport %s%s%s not safe!", BLUE, name, RESET));
            return true;
        }
        if (Teleport.start(this.player, telepoint, this)) {
            this.player.sendMessage(String.format("Teleporting to %s%s%s!", BLUE, telepoint.getTitle(), RESET));
        }
        return true;
    }

    public boolean remove(String name) {
        this.list.remove(name);
        this.player.sendMessage(String.format("Teleport %s%s%s deleted%s!", BLUE, name, RED, RESET));
        return true;
    }

    public List<String> names() {
        return new ArrayList<>(this.list.keySet());
    }

    public TeleportClient(Player player, Application application) {
        LOGGER = application.getLogger();
        LOGGER.info(player.getDisplayName() + ".teleports loaded");
        this.player = player;
        this.application = application;
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

    public Application getApplication() {
        return this.application;
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
