package com.varestorm.plugins.teleports;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.varestorm.plugins.teleports.commands.QuickTeleportCommand;
import com.varestorm.plugins.teleports.commands.TeleportCommand;
import com.varestorm.plugins.teleports.commands.TeleportPlayerCommand;
import com.varestorm.plugins.teleports.listener.PlayerListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

public final class Application extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Teleport.init();

        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        QuickTeleportCommand quickTeleportCommand = new QuickTeleportCommand();
        Objects.requireNonNull(this.getServer().getPluginCommand("list")).setExecutor(quickTeleportCommand);
        Objects.requireNonNull(this.getServer().getPluginCommand("spawn")).setExecutor(quickTeleportCommand);
        Objects.requireNonNull(this.getServer().getPluginCommand("bed")).setExecutor(quickTeleportCommand);
        Objects.requireNonNull(this.getServer().getPluginCommand("back")).setExecutor(quickTeleportCommand);
        Objects.requireNonNull(this.getServer().getPluginCommand("anchor")).setExecutor(quickTeleportCommand);
        Objects.requireNonNull(this.getServer().getPluginCommand("home")).setExecutor(quickTeleportCommand);
        Objects.requireNonNull(this.getServer().getPluginCommand("sethome")).setExecutor(quickTeleportCommand);

        TeleportCommand teleportCommand = new TeleportCommand(this);
        Objects.requireNonNull(this.getServer().getPluginCommand("tp")).setExecutor(teleportCommand);
        Objects.requireNonNull(this.getServer().getPluginCommand("tp")).setTabCompleter(teleportCommand);

        TeleportPlayerCommand teleportPlayerCommand = new TeleportPlayerCommand(this);
        Objects.requireNonNull(this.getServer().getPluginCommand("tpp")).setExecutor(teleportPlayerCommand);
        Objects.requireNonNull(this.getServer().getPluginCommand("tpp")).setTabCompleter(teleportPlayerCommand);

        for (Player player : this.getServer().getOnlinePlayers()) {
            this.setList(player);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for (Player player : this.getServer().getOnlinePlayers()) {
            try {
                this.saveFile(player, Teleport.getList(player.getUniqueId()).toJSON());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setList(Player player) {
        JsonObject json = this.loadFile(player);
        if (json != null) {
            Teleport.setList(player, this, json);
        } else {
            Teleport.setList(player);
        }
    }

    protected JsonObject loadFile(Player player) {
        File file = this.getDataFolder().getParentFile();
        File folder = new File(file.getAbsolutePath() + "/teleport");
        if (!folder.isDirectory() && !folder.mkdirs()) {
            System.out.println("Error while create directory /teleport");
        }
        File json_file = new File(folder.getAbsolutePath() + "/" + player.getUniqueId() + ".json");
        if (json_file.exists()) {
            StringBuilder contents = new StringBuilder();
            try {
                Scanner scanner = new Scanner(json_file);
                while (scanner.hasNextLine()) {
                    contents.append(scanner.nextLine());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                JsonElement element = new JsonParser().parse(contents.toString());
                if (element != null) {
                    return element.getAsJsonObject();
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public boolean saveFile(Player player, JsonObject object) {
        File file = this.getDataFolder().getParentFile();
        File folder = new File(file.getAbsolutePath() + "/teleport");
        if (!folder.isDirectory() && !folder.mkdirs()) {
            System.out.println("Could save teleport directory");
            return false;
        }
        File json_file = new File(folder.getAbsolutePath() + "/" + player.getUniqueId() + ".json");
        if (!json_file.exists()) {
            try {
                if (!json_file.createNewFile()) {
                    System.out.println(String.format("Could save player file of %s at %s", player.getUniqueId(), json_file.getAbsolutePath()));
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        try {
            String content = object.toString();
            FileWriter writer = new FileWriter(json_file);
//            System.out.println(content);
            writer.write(content);
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
