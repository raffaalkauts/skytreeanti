package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.IslandService;
import com.wiredid.skytree.model.Island;
import org.bukkit.Location;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;

public class PortalListener implements Listener {

    private final SkytreePlugin plugin;
    private final IslandService islandService;
    private final org.bukkit.NamespacedKey lastWorldKey;

    public PortalListener(SkytreePlugin plugin, IslandService islandService) {
        this.plugin = plugin;
        this.islandService = islandService;
        this.lastWorldKey = new org.bukkit.NamespacedKey(plugin, "portal_last_world");
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        World fromWorld = from.getWorld();

        // Check if entering Nether (from Overworld or Skytree)
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                // Entering Nether: Save origin world
                player.getPersistentDataContainer().set(lastWorldKey, org.bukkit.persistence.PersistentDataType.STRING,
                        fromWorld.getName());
            } else if (fromWorld.getEnvironment() == World.Environment.NETHER) {
                // Leaving Nether: Check where they came from
                handleNetherReturn(event, player);
            }
        } else if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            handleEndPortal(event, player, fromWorld);
        }
    }

    private void handleNetherReturn(PlayerPortalEvent event, Player player) {
        String skytreeWorldName = plugin.getConfig().getString("world.name", "skytree_world");
        String lastWorld = player.getPersistentDataContainer().get(lastWorldKey,
                org.bukkit.persistence.PersistentDataType.STRING);

        // If they came from Skytree world, force return to Island
        if (skytreeWorldName.equals(lastWorld)) {
            Optional<Island> islandOpt = islandService.getIsland(player.getUniqueId());
            if (islandOpt.isPresent()) {
                event.setTo(islandOpt.get().getSpawnLocation());
                player.sendMessage("§7[Skytree] Returning to your island...");
            }
        }
        // If they came from normal world (or null), let vanilla handle it (usually
        // returns to world)
    }

    private void handleEndPortal(PlayerPortalEvent event, Player player, World fromWorld) {
        if (fromWorld.getEnvironment() == World.Environment.THE_END) {
            Optional<Island> islandOpt = islandService.getIsland(player.getUniqueId());
            if (islandOpt.isPresent()) {
                event.setTo(islandOpt.get().getSpawnLocation());
                player.sendMessage("§7[Skytree] Returning to your island...");
            }
        }
    }
}

