package com.wiredid.skytree.util;

import com.wiredid.skytree.model.Island;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

/**
 * Displays island borders with particles
 */
public class IslandBorderVisualizer {

    private final Plugin plugin;

    public IslandBorderVisualizer(Plugin plugin) {
        this.plugin = plugin;
    }

    public void showBorder(Player player, Island island) {
        Location center = island.getCenter();
        int radius = island.getSize() / 2;

        // Spawn particles in a square border
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100 || !player.isOnline()) { // 5 seconds
                    cancel();
                    return;
                }

                // Draw 4 lines of the border
                drawLine(player, center.clone().add(-radius, 0, -radius), center.clone().add(radius, 0, -radius));
                drawLine(player, center.clone().add(radius, 0, -radius), center.clone().add(radius, 0, radius));
                drawLine(player, center.clone().add(radius, 0, radius), center.clone().add(-radius, 0, radius));
                drawLine(player, center.clone().add(-radius, 0, radius), center.clone().add(-radius, 0, -radius));

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Every tick for smooth animation
    }

    private void drawLine(Player player, Location start, Location end) {
        double distance = start.distance(end);
        int particles = (int) (distance * 2); // 2 particles per block

        for (int i = 0; i <= particles; i++) {
            double ratio = (double) i / particles;
            Location point = start.clone().add(
                    (end.getX() - start.getX()) * ratio,
                    (end.getY() - start.getY()) * ratio,
                    (end.getZ() - start.getZ()) * ratio);

            // Spawn particle at player's y-level for visibility
            point.setY(player.getLocation().getY());

            player.spawnParticle(Particle.HAPPY_VILLAGER, point, 1, 0, 0, 0, 0);
        }
    }

    public void showCorners(Player player, Island island) {
        Location center = island.getCenter();
        int radius = island.getSize() / 2;

        // Show 4 corners with beacons
        spawnBeacon(player, center.clone().add(-radius, 0, -radius));
        spawnBeacon(player, center.clone().add(radius, 0, -radius));
        spawnBeacon(player, center.clone().add(radius, 0, radius));
        spawnBeacon(player, center.clone().add(-radius, 0, radius));
    }

    private void spawnBeacon(Player player, Location loc) {
        loc.setY(player.getLocation().getY());

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 60 || !player.isOnline()) {
                    cancel();
                    return;
                }

                player.spawnParticle(Particle.END_ROD, loc.clone().add(0, ticks * 0.1, 0), 3, 0.2, 0.2, 0.2, 0);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}

