# Teleports

This Paper plugin allows players to teleport to specific locations within the game world and between
worlds. It provides a convenient way for players to quickly travel to different areas without having
to manually input coordinates or use complex commands.

This project is based on [PaperMC](https://papermc.io/).

## Build and installation

Requirements: Java 25 and a compatible Paper server (API version 26.2).

Build the plugin with the Gradle wrapper:

```sh
./gradlew build
```

On Windows, use `gradlew.bat build` instead. Copy the plugin JAR from `build/libs/` into your
server's `plugins/` directory, then restart the server.

## Commands

These commands are intended for players in game.

| Command | Description |
| --- | --- |
| `/spawn [overworld\|nether\|end]` | Teleport to a world's spawn; defaults to the current world. |
| `/home set` | Set your home at your current location. |
| `/home` | Teleport to your home. |
| `/bed` | Teleport to your bed. |
| `/anchor` | Teleport to your respawn anchor. |
| `/back` | Return to your previous location. |
| `/tp set <name>` | Save your current location under a name. |
| `/tp to <name>` | Teleport to a saved location. |
| `/tp force <name>` | Force a teleport to a saved location. |
| `/tp remove <name>` | Remove a saved location. |
| `/tp list` | List your saved teleports. |
| `/tp help` | Show teleport help. |
| `/tpp request <player>` | Request to teleport to another player. |
| `/tpp list` | List received requests. |
| `/tpp accept <player>` | Accept a player's request to teleport to you. |
| `/tpp deny <player>` | Deny a player's request. |

`/teleport` is an alias for the default `/tp` command in Minecraft.

## Disclaimer

This plugin is a hobby project and is not intended for commercial use. The developers of this plugin
are not responsible for any damages or issues that may arise from the use of this plugin.
By using this plugin, you acknowledge and agree to assume all risks and responsibilities associated
with its use.
