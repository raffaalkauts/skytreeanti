package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.impl.SkytreeEconomyService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for player join/quit events to manage thirst system
 */
public class PlayerJoinQuitListener implements Listener {

    private final SkytreePlugin plugin;

    public PlayerJoinQuitListener(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Register player for thirst system
        if (plugin.getThirstService() != null) {
            plugin.getThirstService().registerPlayer(event.getPlayer());
        }

        // Load Island into cache (Critical for protection)
        if (plugin.getIslandService() != null) {
            plugin.getIslandService().getIsland(event.getPlayer().getUniqueId());
        }

        // Unlock recipes for the player
        if (plugin.getMythicItemManager() != null) {
            plugin.getMythicItemManager().unlockRecipes(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Unregister player from thirst system
        if (plugin.getThirstService() != null) {
            plugin.getThirstService().unregisterPlayer(event.getPlayer());
        }

        // Unload Economy Cache
        if (plugin.getEconomyService() instanceof SkytreeEconomyService) {
            ((SkytreeEconomyService) plugin.getEconomyService()).unloadBalance(event.getPlayer().getUniqueId());
        }

        // Clean up safe location cache
        if (plugin.getIslandSafetyListener() != null) {
            plugin.getIslandSafetyListener().cleanup(event.getPlayer().getUniqueId());
        }
    }
}
