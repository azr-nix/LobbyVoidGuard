# RizzlersLobbyVoidGuard

High-performance, lightweight Lobby void protection plugin for the **RIZZLERS** Minecraft network.

## Features

- **Lobby-Exclusive Isolation**: Monitors only the designated Lobby world (default `world`). Has zero impact on Survival or other backend servers.
- **Micro-Optimized Event Handling**: Uses Paper/Purpur's `hasChangedPosition()` event check to skip expensive look/camera tick updates.
- **Async Teleportation**: Dispatches safe `teleportAsync()` chunk loads to prevent server lag spikes and tick drops.
- **Multi-Tick Re-Entrancy Guard**: Concurrently guards players in-flight against duplicate teleports or fall velocity accumulation.
- **Configurable Thresholds**: Easily customize world name, threshold Y, spawn coordinates, orientation, and rescue modes in `config.yml`.
- **Target Runtime**: Built for Purpur 26.3 with Java 25.

---

## Configuration (`config.yml`)

```yaml
# The exact name of your Lobby world folder/world.
# Only players in this world are monitored and rescued.
lobby-world: world

# Y-level threshold. When a player falls below this Y coordinate,
# they are automatically rescued and teleported.
rescue-below-y: 69.0

# Rescue behavior:
# true  -> Teleport the player to the safe spawn coordinates defined below.
# false -> Teleport the player back to their current X and Z at spawn.y.
teleport-to-spawn: true

# Safe platform spawn location. Ensure this location is a safe, solid platform.
spawn:
  x: 0.5
  y: 72.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0
```

---

## Deployment Instructions (Plugged.host / Pelican)

> [!IMPORTANT]
> Install this plugin **ONLY on the Lobby backend server**.
> Do **NOT** install it on the Velocity proxy or Survival backend servers.

1. **Back up the Lobby server**: Create a backup or snapshot of your Lobby backend server in the control panel.
2. **Access Panel**: Open your Lobby backend server instance in **Plugged.host / Pelican**.
3. **Open Files**: Navigate to the File Manager and open the `plugins/` directory.
4. **Upload JAR**: Upload `RizzlersLobbyVoidGuard-1.0.0.jar` into the `plugins/` folder.
5. **Verify World & Coordinates**:
   - Check your Lobby world folder name (e.g., `world` or `lobby`).
   - If not `world`, edit `plugins/RizzlersLobbyVoidGuard/config.yml` (generated upon first launch or created manually) to set `lobby-world`.
   - Verify that `spawn.x`, `spawn.y`, `spawn.z` point to a solid, safe block platform.
6. **Restart Server**: Restart the Lobby backend server from the panel.
7. **Verify Console Logs**: Check server startup logs for:
   ```
   [RizzlersLobbyVoidGuard] RizzlersLobbyVoidGuard v1.0.0 initialized.
   [RizzlersLobbyVoidGuard] Target World: world
   [RizzlersLobbyVoidGuard] Rescue Threshold: Y < 69.0
   [RizzlersLobbyVoidGuard] Confirmed lobby world 'world' is active and loaded.
   ```
8. **In-Game Verification**:
   - Join the Lobby backend.
   - Leap or fly below Y = 69.
   - Confirm immediate teleport to `(0.5, 72.0, 0.5)` with zero fall damage and zero residual downward momentum.

---

## Building from Source

This project compiles on GitHub Actions using Temurin JDK 25 and Purpur API 26.3:

```bash
mvn clean verify package
```

Output artifact: `target/RizzlersLobbyVoidGuard-1.0.0.jar`
