package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Island;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;

public class IslandBorderListener implements Listener {

    private final SkytreePlugin plugin;

    public IslandBorderListener(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("skytree.admin") || player.hasPermission("skytree.bypass")) {
            return;
        }

        Optional<Island> optIsland = plugin.getIslandService().getIslandAtLocation(to);
        if (optIsland.isPresent()) {
            Island island = optIsland.get();
            // Check if visitor entry is allowed
            if (!island.getSettings().getOrDefault("visitor_entry", true)) {
                if (!island.isMember(player.getUniqueId())
                        && island.getTrustLevel(player.getUniqueId()) == com.wiredid.skytree.model.TrustLevel.NONE) {
                    // Not allowed
                    event.setCancelled(true);
                    player.sendMessage("§c§l[Skytree] §cThis island is private!");
                    // Bounce back slightly
                    player.setVelocity(from.toVector().subtract(to.toVector()).normalize().multiply(0.5));
                }
            }
        }
    }
}
