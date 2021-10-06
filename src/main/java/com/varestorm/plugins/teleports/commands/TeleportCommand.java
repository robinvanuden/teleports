package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.client.TeleportClient;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeleportCommand implements CommandExecutor, TabCompleter {

    static final ChatColor RED = ChatColor.RED;
    static final ChatColor GREEN = ChatColor.GREEN;
    static final ChatColor RESET = ChatColor.RESET;

    final Application application;
    public static List<UUID> list;

    public TeleportCommand(Application application) {
        this.application = application;
        TeleportCommand.list = new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            TeleportClient client = this.application.getClient(player.getUniqueId());
            if (args.length > 0) {
                if ("test".equals(args[0])) {
                    player.sendMessage("Testing!");
                }
                if (args.length > 1) {
                    switch (args[0]) {
                        case "set":
                            return client.set(args[1]);
                        case "to":
                            return client.to(args[1]);
                        case "remove":
                            return client.remove(args[1]);
                        default:
                            player.sendMessage("This command is temporarily not available...");
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            commands.add("set");
            commands.add("remove");
            commands.add("to");
            commands.add("list");
            commands.add("test");
            commands.add("boss");

            if (args.length > 0) {
                if (args[0].equals("")) {
                    suggestions.addAll(commands);
                } else {
                    for (String c : commands) {
                        if (c.startsWith(args[0])) {
                            suggestions.add(c);
                        }
                    }
                }
            }
            if (args.length > 1) {
                suggestions.clear();
                switch (args[0]) {
                    case "to":
                    case "remove":
                        return this.application.getClient(player.getUniqueId()).names();
                }
            }
            if (args.length > 2) {
                // Third argument?
                player.sendMessage("End of the line");
            }
        }
        return suggestions;
    }
}
