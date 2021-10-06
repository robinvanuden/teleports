package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeleportPlayerCommand implements CommandExecutor, TabCompleter {

    static final ChatColor RED = ChatColor.RED;
    static final ChatColor GREEN = ChatColor.GREEN;
    static final ChatColor RESET = ChatColor.RESET;
    static final ChatColor BLUE = ChatColor.BLUE;

    final Application application;
    public static List<UUID> list;

    public TeleportPlayerCommand(Application application) {
        this.application = application;
        TeleportPlayerCommand.list = new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 0) {
                switch (args[0]) {
                    case "list":
                    case "accept":
                    case "deny":
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
            commands.add("list");
            commands.add("request");
            commands.add("accept");
            commands.add("deny");

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
                if ("request".equals(args[0])) {
                    List<String> players = new ArrayList<>();
                    for (Player p : this.application.getServer().getOnlinePlayers()) {
                        if (p.getUniqueId() != player.getUniqueId()) {
                            players.add(p.getDisplayName());
                        }
                    }
                    if (args[1].equals("")) {
                        suggestions.addAll(players);
                    } else {
                        for (String c : players) {
                            if (c.startsWith(args[1])) {
                                suggestions.add(c);
                            }
                        }
                    }
                }
            }
            if (args.length > 2) {
                suggestions.clear();
            }
        }
        return suggestions;
    }
}
