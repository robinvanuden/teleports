package com.varestorm.plugins.teleports;

import com.varestorm.plugins.teleports.client.TeleportClient;
import com.varestorm.plugins.teleports.commands.QuickTeleportCommand;
import com.varestorm.plugins.teleports.commands.TeleportCommand;
import com.varestorm.plugins.teleports.commands.TeleportPlayerCommand;
import com.varestorm.plugins.teleports.listener.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Application extends JavaPlugin {

   private Map<UUID, TeleportClient> clients;

   public enum WorldType {VARESTORM, NETHER, END}

   private BukkitScheduler scheduler = Bukkit.getScheduler();

   @Override
   public void onEnable() {
      // Plugin startup logic
      this.clients = new HashMap<>();
      this.scheduler = this.getServer().getScheduler();

      this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
      QuickTeleportCommand quickTeleportCommand = new QuickTeleportCommand(this);
      this.registerCommand("spawn", quickTeleportCommand);
      this.registerCommand("bed", quickTeleportCommand);
      this.registerCommand("back", quickTeleportCommand);
      this.registerCommand("anchor", quickTeleportCommand);
      this.registerCommand("home", quickTeleportCommand);

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
      this.registerCommand(name.toLowerCase(), executor, null);
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
      this.clients.put(player.getUniqueId(), new TeleportClient(player, this));
   }

   public World getWorld(String name) {
      return this.getServer().getWorld(name);
   }

   public World getWorld(World.Environment environment) {
      for (World world : this.getServer().getWorlds()) {
         if (world.getEnvironment().equals(environment)) {
            return world;
         }
      }
      return null;
   }

   public World getWorld(WorldType type) {
      return switch (type) {
         case VARESTORM -> this.getWorld(World.Environment.NORMAL);
         case NETHER -> this.getWorld(World.Environment.NETHER);
         case END -> this.getWorld(World.Environment.THE_END);
      };
   }

   public void runAsync(Runnable runnable) {
      this.scheduler.runTaskAsynchronously(this, runnable);
   }

   public BukkitTask runLaterAsync(Runnable runnable, long delayTicks) {
      return this.scheduler.runTaskLaterAsynchronously(this, runnable, delayTicks);
   }

   public BukkitTask runLater(Runnable runnable, long delayTicks) {
      return this.scheduler.runTaskLater(this, runnable, delayTicks);
   }

   public TeleportClient getClient(UUID uuid) {
      return this.clients.get(uuid);
   }

   public TeleportClient getClient(Player player) {
      return this.getClient(player.getUniqueId());
   }

   public Collection<TeleportClient> getClients() {
      return this.clients.values();
   }
}
