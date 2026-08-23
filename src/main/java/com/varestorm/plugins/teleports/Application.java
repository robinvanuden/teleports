package com.varestorm.plugins.teleports;

import com.varestorm.plugins.teleports.client.TeleportClientManager;
import com.varestorm.plugins.teleports.commands.QuickTeleportCommand;
import com.varestorm.plugins.teleports.commands.TeleportCommand;
import com.varestorm.plugins.teleports.commands.TeleportPlayerCommand;
import com.varestorm.plugins.teleports.listener.PlayerListener;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class Application extends JavaPlugin {

  private TeleportClientManager clientManager;

  @Override
  public void onEnable() {
    // Plugin startup logic
    this.clientManager = new TeleportClientManager();

    this.getServer().getPluginManager().registerEvents(new PlayerListener(this, this.clientManager), this);
    QuickTeleportCommand quickTeleportCommand = new QuickTeleportCommand(this.clientManager);
    this.registerCommand("spawn", quickTeleportCommand);
    this.registerCommand("bed", quickTeleportCommand);
    this.registerCommand("back", quickTeleportCommand);
    this.registerCommand("anchor", quickTeleportCommand);
    this.registerCommand("home", quickTeleportCommand);

    TeleportCommand teleportCommand = new TeleportCommand(this.clientManager);
    this.registerCommand("tp", teleportCommand, teleportCommand);
    this.getServer().getPluginManager().registerEvents(teleportCommand, this);

    TeleportPlayerCommand teleportPlayerCommand = new TeleportPlayerCommand(this.clientManager);
    this.registerCommand("tpp", teleportPlayerCommand, teleportPlayerCommand);

    this.clientManager.createAll(this, this.getServer().getOnlinePlayers());
  }

  @Override
  public void onDisable() {
    // Plugin shutdown logic
    this.clientManager.saveAll();
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
      this.getLogger().warning(String.format("Failed to register command: /%s", name));
    }
  }

}
