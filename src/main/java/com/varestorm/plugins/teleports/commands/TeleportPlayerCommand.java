package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.Teleport;
import com.varestorm.plugins.teleports.TeleportList;
import com.varestorm.plugins.teleports.Worlds;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
                        return this.list(player);
                    case "accept":
                        return this.accept(player);
                    case "deny":
                        return this.decline(player);
                }
                if (args.length > 1) {
                    if ("request".equals(args[0])) {
                        return this.request(player, args[1]);
                    } else {
                        player.sendMessage("Unknown action: " + args[0]);
                    }
                }
            }
        }
        return false;
    }

    protected boolean list(Player player) {
        TeleportList tp = Teleport.getList(player.getUniqueId());

        if (tp.requests().size() > 0) {
            StringBuilder builder = new StringBuilder();
            for (UUID uuid : tp.requests()) {
                Player p = this.application.getServer().getPlayer(uuid);
                if (p != null) {
                    builder.append(BLUE).append(player.getDisplayName()).append(RESET).append(", ");
                }
            }
            String players = builder.toString();
            player.sendMessage((tp.requests().size() == 1 ? "You got a request from " : "You got requests from ") + players.substring(0, players.length() - 2) + ".");
            return true;
        }
        player.sendMessage(ChatColor.ITALIC + "No requests from players.");
        return true;

    }

    protected boolean request(Player requester, String to) {
        Player receiver = this.application.getServer().getPlayer(to);
        if (receiver == null) {
            requester.sendMessage(RED + to + " is not online.");
            return true;
        }
        if (receiver.getUniqueId() == requester.getUniqueId()) {
            requester.sendMessage(RED + "Are you really trying to teleport to yourself?");
            return true;
        }
        String receiver_name = Teleport.getName(receiver.getDisplayName(), receiver.getWorld());
        String requester_name = Teleport.getName(requester.getDisplayName(), requester.getWorld());
        TeleportList tp = Teleport.getList(receiver.getUniqueId());
        if (tp.requests().contains(requester.getUniqueId())) {
            requester.sendMessage("You already made a request to " + receiver_name + ".");
            return true;
        }
        requester.sendMessage("Request to " + receiver_name + " was made.");
        receiver.sendMessage(requester_name + " has requested a teleport.");
        receiver.sendMessage(String.format("Use %s/tpp accept%s or %s/tpp deny.", GREEN, RESET, RED));
        tp.request(requester);
        return true;
    }

    protected boolean accept(Player player) {
        Location location = Worlds.getFreeLocation(player.getLocation());
        if (!Worlds.canSpawn(location)) {
            player.sendMessage(String.format("Your location seems%s obstructed%s. Go to a clear location and try again.", RED, RESET));
            return true;
        }
        TeleportList tp = Teleport.getList(player.getUniqueId());
        List<UUID> list = tp.requests();
        if (list.size() > 0) {
            StringBuilder builder = new StringBuilder();
            for (UUID uuid : list) {
                Player p = this.application.getServer().getPlayer(uuid);
                if (p != null) {
                    builder.append(BLUE).append(player.getDisplayName()).append(RESET).append(", ");
                    Teleport.instant(p, player.getDisplayName(), location, true);
                }
            }
            String players = builder.toString();
            player.sendMessage(GREEN + "Accepted" + RESET + (tp.requests().size() == 1 ? " the request of " : " requests of ") + players.substring(0, players.length() - 2) + ".");
            tp.decline();
        } else {
            player.sendMessage(RED + "No requests to accept.");
        }
        return true;
    }

    protected boolean decline(Player player) {
        TeleportList tp = Teleport.getList(player.getUniqueId());
        List<UUID> list = tp.requests();
        if (list.size() > 0) {
            for (UUID uuid : list) {
                Player p = this.application.getServer().getPlayer(uuid);
                if (p != null) {
                    p.sendMessage("Your request was " + RED + "denied" + RESET + " by " + BLUE + player.getDisplayName());
                }
            }
            player.sendMessage("All teleport requests are " + RED + "denied.");
            tp.decline();
        }
        return true;
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
