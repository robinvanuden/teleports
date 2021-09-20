package com.varestorm.plugins.teleports;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.varestorm.plugins.teleports.commands.QuickTeleportCommand;
import com.varestorm.plugins.teleports.commands.TeleportCommand;
import com.varestorm.plugins.teleports.commands.TeleportPlayerCommand;
import com.varestorm.plugins.teleports.listener.PlayerListener;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public final class Application extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Teleport.init();

        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        QuickTeleportCommand quickTeleportCommand = new QuickTeleportCommand();
        this.registerCommand("list", quickTeleportCommand);
        this.registerCommand("spawn", quickTeleportCommand);
        this.registerCommand("bed", quickTeleportCommand);
        this.registerCommand("back", quickTeleportCommand);
        this.registerCommand("anchor", quickTeleportCommand);
        this.registerCommand("home", quickTeleportCommand);
        this.registerCommand("sethome", quickTeleportCommand);

        TeleportCommand teleportCommand = new TeleportCommand(this);
        this.registerCommand("tp", teleportCommand, teleportCommand);

        TeleportPlayerCommand teleportPlayerCommand = new TeleportPlayerCommand(this);
        this.registerCommand("tpp", teleportPlayerCommand, teleportPlayerCommand);

        for (Player player : this.getServer().getOnlinePlayers()) {
            this.setList(player);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for (Player player : this.getServer().getOnlinePlayers()) {
            try {
                if (this.saveFile(player, Teleport.getList(player.getUniqueId()).toJSON())) {
                    System.out.println("Error while saving teleports of P#" + player.getUniqueId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    public void setList(Player player) {
        JsonObject json = this.loadFile(player);
        if (json != null) {
            Teleport.setList(player, this, json);
        } else {
            Teleport.setList(player);
        }
    }

    private JsonObject loadFile(Player player) {
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
}
