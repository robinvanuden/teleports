package com.varestorm.plugins.teleports.commands;

import com.varestorm.plugins.teleports.Application;
import com.varestorm.plugins.teleports.Teleport;
import com.varestorm.plugins.teleports.TeleportList;
import com.varestorm.plugins.teleports.Worlds;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

public class TeleportCommand implements CommandExecutor, TabCompleter {

    static final ChatColor RED = ChatColor.RED;
    static final ChatColor GREEN = ChatColor.GREEN;
    static final ChatColor RESET = ChatColor.RESET;
    static ChatColor BLUE = ChatColor.BLUE;

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
            if (args.length > 0) {
                if ("list".equals(args[0])) {
                    return this.list(player);
                }
                if (args.length > 1) {
                    switch (args[0]) {
                        case "to":
                            return this.to(player, args[1]);
                        case "set":
                            return this.set(player, args[1]);
                        case "remove":
                            return this.remove(player, args[1]);
                        default:
                            player.sendMessage("Unknown action: " + args[0]);
                    }
                }
            }
        }
        return false;
    }

    protected boolean set(Player player, String name) {
        name = name.toLowerCase();
        TeleportList list = Teleport.getList(player.getUniqueId());
        if (!list.canSave(name)) {
            player.sendMessage(String.format("You can't add more teleports. %s%s%S is the limit.", RED, TeleportList.MAX, RESET));
            return true;
        }
        list.set(name, player.getLocation());
        World world = player.getLocation().getWorld();
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Something went wrong while teleporting...");
            return true;
        }
        int left = list.left();
        ChatColor color;
        if (left < 2) {
            color = RED;
        } else if (left < 3) {
            color = ChatColor.GOLD;
        } else {
            color = GREEN;
        }
        player.sendMessage(String.format(
                "%s was saved. You have %s%s%s %s left.",
                Teleport.getName(name, world),
                color,
                Teleport.getList(player.getUniqueId()).left(),
                RESET,
                left == 1 ? "teleport" : "teleports"
        ));
        return true;
    }

    protected boolean remove(Player player, String name) {
        name = name.toLowerCase();
        Teleport.getList(player.getUniqueId()).remove(name);
        World world = player.getLocation().getWorld();
        if (world == null) {
            player.sendMessage(RED + "Something went wrong while teleporting...");
            return true;
        }
        player.sendMessage(Teleport.getName(name, world) + " was removed.");
        return true;
    }

    protected boolean list(Player player) {
        TeleportList tp_list = Teleport.getList(player.getUniqueId());
        if (tp_list == null) {
            tp_list = Teleport.setList(player);
        }
        tp_list.sout();
        Map<String, Location> list = tp_list.list();

        int charges = tp_list.getAnchorCharges();
        ChatColor color = GREEN;
        if (charges < 2) {
            color = RED;
        } else if (charges < 3) {
            color = ChatColor.GOLD;
        }

        player.sendMessage(String.format("%s%s%sTeleport list", ChatColor.RESET, ChatColor.GRAY, ChatColor.BOLD));
        player.sendMessage("---------------------------");
        player.sendMessage(String.format("Home %s" + RESET, (tp_list.getHome() == null) ? RED + "not set" : GREEN + "set"));
        player.sendMessage(String.format("Bed %s" + RESET, (tp_list.hasNoBed()) ? RED + "not set" : GREEN + "set"));
        player.sendMessage(String.format("Anchor %s" + RESET, (tp_list.hasNoAnchor()) ? RED + "not set" : String.format("%sset%s and has %s%s%s charges left.", GREEN, RESET, color, charges, RESET)));
        player.sendMessage("---------------------------");
        if (list.size() > 0) {
            int count = 1;
            for (Map.Entry<String, Location> entry : list.entrySet()) {
                player.sendMessage(String.format("%s. %s", count, Teleport.getName(entry.getKey(), Objects.requireNonNull(entry.getValue().getWorld()))));
                count++;
            }
        } else {
            player.sendMessage(ChatColor.ITALIC + "No custom teleports set");

        }
        return true;
    }

    protected boolean to(Player player, String name) {
        name = name.toLowerCase();
        Location teleport = Teleport.getList(player.getUniqueId()).get(name);
        if (teleport == null) {
            player.sendMessage(String.format("%sWarning!%s No teleport found.", RED, RESET));
            return true;
        }
        if (!Worlds.canSpawn(teleport)) {
            player.sendMessage(String.format("%sWarning!%s Your teleport is obstructed.", RED, RESET));
            return true;
        }
        World world = teleport.getWorld();
        if (world == null) {
            player.sendMessage(String.format("%sWarning!%s Something went wrong while teleporting.", RED, RESET));
            return true;
        }
        list.add(player.getUniqueId());
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 180, 10));
        this.teleport(player, name, teleport);
        return true;
    }

    public void teleport(Player player, String name, Location location) {
        if (location.getWorld() == null) {
            this.sendTitle(player, String.format("%s%s%sCanceled", RESET, RED, ChatColor.BOLD), "Error...");
            return;
        }
        final int[] count = {0};
        BukkitScheduler scheduler = this.application.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this.application, () -> {

            switch (count[0]) {
                case 1:
                    this.sendTitle(
                            player,
                            String.format("%s%sTeleporting start", ChatColor.GRAY, ChatColor.BOLD),
                            ChatColor.RESET + "Preparing teleport to " + Teleport.getName(name, location.getWorld()) + ". Don't move."
                    );
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1);
                    break;
                case 5:
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 4);
                    break;
                case 20:
                    this.sendTitle(player, "Teleporting", "Stay still for 5 seconds.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1);
                    break;
                case 40:
                    this.sendTitle(player, "Teleporting", "Stay still for 4 seconds.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1);
                    break;
                case 60:
                    this.sendTitle(player, "Teleporting", "Stay still for 3 seconds.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1);
                    break;
                case 80:
                    this.sendTitle(player, "Teleporting", "Stay still for 2 seconds.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1);
                    break;
                case 100:
                    this.sendTitle(player, "Teleporting", "Stay still for 1 second.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 1);
                    break;
                case 120:
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 5);
                    Teleport.instant(player, name, location, true);
                    break;
                case 122:
                    this.sendTitle(player, "GO!", "You arrived at your destination.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 2);
                    break;
                case 124:
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, 7);
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                    scheduler.cancelTasks(this.application);
                    break;
            }

            // Do something
            if (!list.contains(player.getUniqueId())) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 10, -10);
                this.sendTitle(player, String.format("%s%s%sCanceled", ChatColor.RESET, ChatColor.RED, ChatColor.BOLD), "You moved...");
                System.out.println("cancel");
                scheduler.cancelTasks(this.application);

                player.removePotionEffect(PotionEffectType.BLINDNESS);
            }
            count[0]++;
        }, 20L, 1L);
    }

    protected void sendTitle(Player player, String title, String sub_text) {
        player.sendTitle(
                String.format("%s%s%s", ChatColor.BLUE, ChatColor.BOLD, title),
                String.format("%s%s", ChatColor.ITALIC, sub_text),
                10, 20, 10
        );
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
                        if (args[1].equals("")) {
                            suggestions.addAll(Teleport.getList(player.getUniqueId()).list().keySet());
                        } else {
                            for (String c : Teleport.getList(player.getUniqueId()).list().keySet()) {
                                if (c.startsWith(args[1])) {
                                    suggestions.add(c);
                                }
                            }
                        }
                        break;

                }
            }
            if (args.length > 2) {
                suggestions.clear();
            }
        }
        return suggestions;
    }
}
