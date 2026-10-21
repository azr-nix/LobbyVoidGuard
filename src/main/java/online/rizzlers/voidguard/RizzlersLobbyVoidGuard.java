package online.rizzlers.voidguard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RizzlersLobbyVoidGuard extends JavaPlugin implements Listener {

    private String lobbyWorldName;
    private double threshold;
    private boolean useSpawn;
    private double spawnX;
    private double spawnY;
    private double spawnZ;
    private float spawnYaw;
    private float spawnPitch;

    // Concurrently tracks players currently undergoing a rescue teleport to avoid duplicate triggers across ticks.
    private final Set<UUID> pendingRescues = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("==================================================");
        getLogger().info("RizzlersLobbyVoidGuard v" + getDescription().getVersion() + " initialized.");
        getLogger().info("Target World: " + lobbyWorldName);
        getLogger().info("Rescue Threshold: Y < " + threshold);
        getLogger().info("Rescue Mode: " + (useSpawn ? "Configured Spawn Coordinates" : "Preserve X/Z at Spawn Y"));
        getLogger().info(String.format(Locale.ROOT, "Spawn Location: X=%.2f, Y=%.2f, Z=%.2f (Yaw=%.1f, Pitch=%.1f)",
                spawnX, spawnY, spawnZ, spawnYaw, spawnPitch));

        World world = Bukkit.getWorld(lobbyWorldName);
        if (world == null) {
            getLogger().warning("Lobby world '" + lobbyWorldName + "' is not currently loaded into memory! " +
                    "Please verify 'lobby-world' in config.yml matches your server's lobby world name.");
        } else {
            getLogger().info("Confirmed lobby world '" + lobbyWorldName + "' is active and loaded.");
        }
        getLogger().info("==================================================");
    }

    @Override
    public void onDisable() {
        pendingRescues.clear();
        getLogger().info("RizzlersLobbyVoidGuard disabled.");
    }

    private void loadConfiguration() {
        reloadConfig();
        var c = getConfig();
        lobbyWorldName = c.getString("lobby-world", "world");
        threshold = c.getDouble("rescue-below-y", 69.0);
        useSpawn = c.getBoolean("teleport-to-spawn", true);
        spawnX = c.getDouble("spawn.x", 0.5);
        spawnY = c.getDouble("spawn.y", 72.0);
        spawnZ = c.getDouble("spawn.z", 0.5);
        spawnYaw = (float) c.getDouble("spawn.yaw", 0.0);
        spawnPitch = (float) c.getDouble("spawn.pitch", 0.0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // High performance check: immediately ignore camera/head-turn movements where coordinates have not changed.
        if (!event.hasChangedPosition()) {
            return;
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        // Fast world validation: ensure movement occurred in the configured lobby world.
        World world = to.getWorld();
        if (world == null || !world.getName().equalsIgnoreCase(lobbyWorldName)) {
            return;
        }

        // Threshold check: ignore movement if the player remains safely at or above threshold.
        if (to.getY() >= threshold) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Prevent race conditions and duplicate teleport dispatches across consecutive ticks.
        if (!pendingRescues.add(playerId)) {
            return;
        }

        // Immediately halt downward velocity and cancel accumulated fall distance before dispatch.
        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0.0f);

        Location target = useSpawn
                ? new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch)
                : new Location(world, to.getX(), spawnY, to.getZ(), to.getYaw(), to.getPitch());

        try {
            // Paper/Purpur asynchronous chunk-loading teleport to preserve server tick-rate stability.
            player.teleportAsync(target).thenAccept(success -> {
                try {
                    if (Boolean.TRUE.equals(success) && player.isOnline()) {
                        player.setFallDistance(0.0f);
                        player.setVelocity(new Vector(0, 0, 0));
                    }
                } finally {
                    pendingRescues.remove(playerId);
                }
            }).exceptionally(ex -> {
                try {
                    if (player.isOnline()) {
                        player.teleport(target);
                        player.setFallDistance(0.0f);
                        player.setVelocity(new Vector(0, 0, 0));
                    }
                } finally {
                    pendingRescues.remove(playerId);
                }
                return null;
            });
        } catch (Throwable t) {
            // Fallback for synchronous execution if async scheduling encounters an unexpected failure.
            try {
                if (player.isOnline()) {
                    player.teleport(target);
                    player.setFallDistance(0.0f);
                    player.setVelocity(new Vector(0, 0, 0));
                }
            } finally {
                pendingRescues.remove(playerId);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingRescues.remove(event.getPlayer().getUniqueId());
    }
}
