package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles island safety features: Keep Inventory, Void Protection, and God
 * Wings flight restriction
 */
public class IslandSafetyListener implements Listener {

    private final SkytreePlugin plugin;
    private final String islandWorldName;
    private final Map<UUID, Location> lastSafeLocations;
    private final double voidThreshold = 0.0; // Y-level below which void protection activates

    public IslandSafetyListener(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.islandWorldName = plugin.getConfig().getString("world.name", "skytree_world");
        this.lastSafeLocations = new HashMap<>();

        // Start task to track safe locations
        startSafeLocationTracker();
    }

    /**
     * Keep Inventory on death in island world
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        // Only apply in island world
        if (!player.getWorld().getName().equals(islandWorldName)) {
            return;
        }

        // Keep inventory and experience
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);

        player.sendMessage("§a[Island] §fYour items and experience have been kept!");
    }

    /**
     * Void Protection - teleport player to last safe location
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Only apply in island world
        if (!player.getWorld().getName().equals(islandWorldName)) {
            return;
        }

        // Check if player is falling into void
        if (player.getLocation().getY() < voidThreshold) {
            Location safeLoc = lastSafeLocations.get(player.getUniqueId());

            if (safeLoc != null) {
                player.teleport(toGroundedSafeLocation(safeLoc));
                player.setFallDistance(0);
                player.sendMessage("§c[Island] §fVoid protection activated! Teleported to safety.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            } else {
                // Fallback: teleport to island spawn or world spawn
                Location spawn = player.getWorld().getSpawnLocation();
                player.teleport(spawn);
                player.setFallDistance(0);
                player.sendMessage("§c[Island] §fVoid protection activated!");
            }
        }
    }

    /**
     * Cancel fall damage after void protection teleport
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Cancel fall damage in island world if player was just teleported from void
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (player.getWorld().getName().equals(islandWorldName)) {
                Location safeLoc = lastSafeLocations.get(player.getUniqueId());
                if (safeLoc != null && player.getLocation().distance(safeLoc) < 5) {
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Track player's last safe ground location
     */
    private void startSafeLocationTracker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    // Only track in island world
                    if (!player.getWorld().getName().equals(islandWorldName)) {
                        continue;
                    }

                    // Only save location if player is on ground and not in creative/spectator
                    boolean onGround;
                    @SuppressWarnings("deprecation")
                    boolean tempOnGround = player.isOnGround();
                    onGround = tempOnGround;

                    if (onGround &&
                            player.getGameMode() != GameMode.CREATIVE &&
                            player.getGameMode() != GameMode.SPECTATOR) {

                        Location loc = toGroundedSafeLocation(player.getLocation());
                        // Make sure it's above void threshold
                        if (loc.getY() > voidThreshold + 5) {
                            lastSafeLocations.put(player.getUniqueId(), loc);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    /**
     * Clean up safe location cache when player leaves
     */
    public void cleanup(UUID playerId) {
        lastSafeLocations.remove(playerId);
    }

    private Location toGroundedSafeLocation(Location base) {
        Location grounded = base.getWorld().getHighestBlockAt(base).getLocation().add(0.5, 1.0, 0.5);
        grounded.setYaw(base.getYaw());
        grounded.setPitch(base.getPitch());

        // Fallback for edge cases where chunk has no valid ground block
        if (grounded.getBlock().getType() != Material.AIR) {
            return base.getWorld().getSpawnLocation();
        }
        return grounded;
    }
}
