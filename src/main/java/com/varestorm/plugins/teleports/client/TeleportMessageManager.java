package com.varestorm.plugins.teleports.client;

import com.varestorm.plugins.teleports.teleporting.Telepoint;
import com.varestorm.plugins.teleports.world.WorldManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;

public class TeleportMessageManager {

  private final Player player;

  public TeleportMessageManager(Player player) {
    this.player = player;
  }

  public void title(Component title, Component subtitle) {
    this.player.showTitle(
      Title.title(
        title,
        subtitle,
        Title.Times.times(
          Duration.ofMillis(500),
          Duration.ofSeconds(4),
          Duration.ofSeconds(1)
        )
      )
    );
  }

  // ============================ WORLDS ============================ //

  public void arrivalTitle(World world, Component subtitle) {
    this.title(WorldManager.getWorldName(world), subtitle);
  }

  public void arrivalTitle(Telepoint telepoint) {
    this.arrivalTitle(telepoint.getWorld(), Component.text("You've arrived at ").append(telepoint.getTitle()));
  }

  // ============================ TELEPORTING ============================ //
  private Component getTeleportTitle(TextColor color) {
    return Component.text("Teleporting!", color, TextDecoration.BOLD);
  }

  private Component getTeleportTitle(World world) {
    return this.getTeleportTitle(WorldManager.getWorldColor(world));
  }

  public void teleportStart(World world) {
    this.title(
      this.getTeleportTitle(world),
      Component.text("Preparing to jump. ").append(Component.text("Don't move!", NamedTextColor.RED))
    );
  }

  public void teleportCountdown(World world, int secondsLeft) {
    this.title(
      this.getTeleportTitle(world),
      Component.text(String.format("Jumping in %d %s!", secondsLeft, "second" + (secondsLeft == 1 ? "" : "s")))
    );
  }

  // ============================ MESSAGES ============================ //

  public void sendError(String message) {
    this.player.sendMessage(Component.text(message, NamedTextColor.RED));
  }

  public void sendInfo(String message) {
    this.player.sendMessage(Component.text(message, NamedTextColor.BLUE));
  }

  public void sendSuccess(String message) {
    this.player.sendMessage(Component.text(message, NamedTextColor.GREEN));
  }
}
