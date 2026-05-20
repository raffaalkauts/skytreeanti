package com.wiredid.skytree.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;

import java.util.Random;

/**
 * Custom ore generator for island world
 * When lava meets water, generates random ores instead of just cobblestone
 */
public class CustomOreGeneratorListener implements Listener {

    private final com.wiredid.skytree.SkytreePlugin plugin;
    private final String islandWorldName;
    private final Random random;

    public CustomOreGeneratorListener(com.wiredid.skytree.SkytreePlugin plugin) {
        this.plugin = plugin;
        this.islandWorldName = plugin.getConfig().getString("world.name", "skytree_world");
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockForm(BlockFormEvent event) {
        // Only apply in island world
        if (!event.getBlock().getWorld().getName().equals(islandWorldName)) {
            return;
        }

        // Check if forming cobblestone or stone
        Material newType = event.getNewState().getType();
        if (newType != Material.COBBLESTONE && newType != Material.STONE) {
            return;
        }

        // Generate random ore based on weighted probabilities
        Material generatedOre = generateRandomOre();
        event.getNewState().setType(generatedOre);
    }

    /**
     * Generate random ore with weighted probabilities from config
     */
    private Material generateRandomOre() {
        org.bukkit.configuration.ConfigurationSection sec = plugin.getConfig().getConfigurationSection("ore_generator");
        if (sec == null) {
            return Material.STONE;
        }

        int roll = random.nextInt(100);
        int currentWeight = 0;

        for (String key : sec.getKeys(false)) {
            int weight = sec.getInt(key);
            currentWeight += weight;
            if (roll < currentWeight) {
                Material mat = Material.matchMaterial(key.toUpperCase());
                return mat != null ? mat : Material.STONE;
            }
        }

        return Material.STONE;
    }
}
