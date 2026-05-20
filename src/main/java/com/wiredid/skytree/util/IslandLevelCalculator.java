package com.wiredid.skytree.util;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Island;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class IslandLevelCalculator {

    private static SkytreePlugin plugin;
    private static Map<Material, Integer> blockValues = null;

    public static void setPlugin(SkytreePlugin p) {
        plugin = p;
        blockValues = null;
    }

    private static Map<Material, Integer> getBlockValues() {
        if (blockValues != null) return blockValues;

        blockValues = new HashMap<>();
        if (plugin == null) {
            blockValues.put(Material.DIRT, 1);
            blockValues.put(Material.STONE, 1);
            return blockValues;
        }

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("island.block_values");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    int value = section.getInt(key, 1);
                    blockValues.put(mat, value);
                } catch (IllegalArgumentException e) {
                    // skip unknown material
                }
            }
        }

        // Fallback defaults if config is empty
        if (blockValues.isEmpty()) {
            blockValues.put(Material.DIRT, 1);
            blockValues.put(Material.STONE, 1);
        }

        return blockValues;
    }

    public static int calculateIslandLevel(Island island) {
        int totalValue = 0;
        int radius = island.getSize() / 2;

        org.bukkit.Location center = island.getCenter();
        org.bukkit.World world = center.getWorld();

        if (world == null)
            return 0;

        Map<Material, Integer> values = getBlockValues();
        int defaultVal = plugin != null ? plugin.getConfig().getInt("island.default_block_value", 1) : 1;

        for (int x = center.getBlockX() - radius; x <= center.getBlockX() + radius; x++) {
            for (int z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; z++) {
                for (int y = 0; y < world.getMaxHeight(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();

                    if (type != Material.AIR) {
                        totalValue += values.getOrDefault(type, defaultVal);
                    }
                }
            }
        }

        int divisor = plugin != null ? plugin.getConfig().getInt("island.level_divisor", 100) : 100;
        return Math.max(1, totalValue / divisor);
    }

    public static int getBlockValue(Material material) {
        return getBlockValues().getOrDefault(material,
                plugin != null ? plugin.getConfig().getInt("island.default_block_value", 1) : 1);
    }
}

