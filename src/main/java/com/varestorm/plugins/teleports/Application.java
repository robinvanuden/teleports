package com.varestorm.plugins.teleports;

import com.google.gson.JsonObject;
import com.varestorm.plugins.teleports.client.TeleportClient;
import com.varestorm.plugins.teleports.commands.QuickTeleportCommand;
import com.varestorm.plugins.teleports.commands.TeleportCommand;
import com.varestorm.plugins.teleports.commands.TeleportPlayerCommand;
import com.varestorm.plugins.teleports.listener.PlayerListener;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Application extends JavaPlugin {

    private static Application application;
    private Map<UUID, TeleportClient> clients;

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.clients = new HashMap<>();
        application = this;

        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        QuickTeleportCommand quickTeleportCommand = new QuickTeleportCommand(this);
        this.registerCommand("spawn", quickTeleportCommand);
        this.registerCommand("bed", quickTeleportCommand);
        this.registerCommand("back", quickTeleportCommand);
        this.registerCommand("anchor", quickTeleportCommand);
        this.registerCommand("home", quickTeleportCommand);
        this.registerCommand("sethome", quickTeleportCommand);
        this.registerCommand("testhome", quickTeleportCommand);

        TeleportCommand teleportCommand = new TeleportCommand(this);
        this.registerCommand("tp", teleportCommand, teleportCommand);
        this.getServer().getPluginManager().registerEvents(teleportCommand, this);

        TeleportPlayerCommand teleportPlayerCommand = new TeleportPlayerCommand(this);
        this.registerCommand("tpp", teleportPlayerCommand, teleportPlayerCommand);

        for (Player player : this.getServer().getOnlinePlayers()) {
            this.setClient(player);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for (TeleportClient client : this.clients.values()) {
            client.saveTelepoints();
        }
    }

    private void registerCommand(String name, CommandExecutor executor) {
        this.registerCommand(name, executor, null);
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter completer) {
        PluginCommand command = this.getServer().getPluginCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            if (completer != null) {
                command.setTabCompleter(completer);
            }
        } else {
            this.getLogger().warning(String.format("Failed registering command: /%s", name));
        }
    }

    public void setClient(Player player) {
        if (!this.clients.containsKey(player.getUniqueId())) {
            this.clients.put(player.getUniqueId(), new TeleportClient(player, this));
        }
    }

    public boolean saveFile(Player player, JsonObject object) {
        File file = this.getDataFolder().getParentFile();
        File folder = new File(file.getAbsolutePath() + "/teleport");
        if (!folder.isDirectory() && !folder.mkdirs()) {
            System.out.println("Couldn't save teleport directory");
            return true;
        }
        File json_file = new File(folder.getAbsolutePath() + "/" + player.getUniqueId() + ".json");
        if (!json_file.exists()) {
            try {
                if (!json_file.createNewFile()) {
                    System.out.printf("Could save player file of %s at %s%n", player.getUniqueId(), json_file.getAbsolutePath());
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return true;
            }
        }
        try {
            String content = object.toString();
            FileWriter writer = new FileWriter(json_file);
//            System.out.println(content);
            writer.write(content);
            writer.close();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    public static World getWorld(String name) {
        return application.getServer().getWorld(name);
    }

    public static void runAsync(Runnable runnable) {
        application.getServer().getScheduler().runTaskAsynchronously(application, runnable);
    }

    public static void runLaterAsync(Runnable runnable, long delay) {
        application.getServer().getScheduler().runTaskLaterAsynchronously(application, runnable, delay);
    }

    public static void runLater(Runnable runnable, long delay) {
        application.getServer().getScheduler().runTaskLater(application, runnable, delay);
    }

    public TeleportClient getClient(UUID uuid) {
        return this.clients.get(uuid);
    }
    public TeleportClient getClient(Player player) {
        return this.getClient(player.getUniqueId());
    }
}
