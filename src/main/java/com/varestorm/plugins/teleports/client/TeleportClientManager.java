package com.varestorm.plugins.teleports.client;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportClientManager {

  private final Map<UUID, TeleportClient> clients = new HashMap<>();

  public TeleportClient get(UUID uuid) {
    return this.clients.get(uuid);
  }

  public TeleportClient get(Player player) {
    return this.clients.get(player.getUniqueId());
  }

  public void create(Plugin plugin, Player player) {
    this.clients.put(player.getUniqueId(), new TeleportClient(player, plugin));
  }

  public void createAll(Plugin plugin, Collection<? extends Player> players) {
    for (Player player : players) {
      this.create(plugin, player);
    }
  }

  public Collection<TeleportClient> all() {
    return this.clients.values();
  }

  public void saveAll() {
    for (TeleportClient client : this.clients.values()) {
      client.saveTelepoints();
    }
  }
}
