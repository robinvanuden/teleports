package com.varestorm.plugins.teleports.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.teleporting.Telepoint;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class TeleportFile {

    static Logger LOGGER;
    Application application;

    public TeleportFile(Application application) {
        this.application = application;
        LOGGER = application.getLogger();
    }

    private String loadFile(Player player) {
        File file = this.application.getDataFolder().getParentFile();
        File folder = new File(file.getAbsolutePath() + "/teleports");
        if (!folder.isDirectory() && !folder.mkdirs()) {
            System.out.println("Error while create directory /teleport");
        }
        File json_file = new File(folder.getAbsolutePath() + "/" + player.getUniqueId() + ".json");
        if (!json_file.exists()) {
            return null;
        }
        StringBuilder contents = new StringBuilder();
        try {
            Scanner scanner = new Scanner(json_file);
            while (scanner.hasNextLine()) {
                contents.append(scanner.nextLine());
            }
            return contents.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Telepoint> loadTeleports(Player player) {
        try {
            String contents = this.loadFile(player);
            if (contents == null) {
                return null;
            }
            JsonElement element = new JsonParser().parse(contents);
            if (element == null) {
                return null;
            }
            List<Telepoint> points = new ArrayList<>();
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                String obj = array.get(i).getAsString();
                Telepoint telepoint = Telepoint.fromJson(obj);
                if (telepoint != null) {
                    points.add(telepoint);
                }
            }
            return points;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean saveTeleports(Player player, List<Telepoint> telepoints) {
        File file = this.application.getDataFolder().getParentFile();
        File folder = new File(file.getAbsolutePath() + "/teleports");
        if (!folder.isDirectory() && !folder.mkdirs()) {
            System.out.println("Could load teleport directory");
            return false;
        }
        File json_file = new File(folder.getAbsolutePath() + "/" + player.getUniqueId() + ".json");
        if (!json_file.exists()) {
            try {
                if (!json_file.createNewFile()) {
                    LOGGER.warning(String.format("Could save player file of %s at %s%n", player.getDisplayName(), json_file.getAbsolutePath()));
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        JsonArray array = new JsonArray();
        for (Telepoint telepoint : telepoints) {
            if (telepoint != null) {
                array.add(telepoint.toString());
            }
        }
        try {
            String content = array.toString();
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
