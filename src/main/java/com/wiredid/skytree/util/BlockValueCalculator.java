package com.wiredid.skytree.util;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * Block value calculator for island level calculation
 */
public class BlockValueCalculator {

    private static final Map<Material, Integer> BLOCK_VALUES = new HashMap<>();

    static {
        // Precious Blocks
        BLOCK_VALUES.put(Material.DIAMOND_BLOCK, 100);
        BLOCK_VALUES.put(Material.EMERALD_BLOCK, 90);
        BLOCK_VALUES.put(Material.NETHERITE_BLOCK, 150);
        BLOCK_VALUES.put(Material.BEACON, 200);

        // Valuable Blocks
        BLOCK_VALUES.put(Material.GOLD_BLOCK, 50);
        BLOCK_VALUES.put(Material.IRON_BLOCK, 25);
        BLOCK_VALUES.put(Material.LAPIS_BLOCK, 20);
        BLOCK_VALUES.put(Material.REDSTONE_BLOCK, 15);
        BLOCK_VALUES.put(Material.COAL_BLOCK, 10);

        // Ores
        BLOCK_VALUES.put(Material.DIAMOND_ORE, 50);
        BLOCK_VALUES.put(Material.DEEPSLATE_DIAMOND_ORE, 50);
        BLOCK_VALUES.put(Material.EMERALD_ORE, 45);
        BLOCK_VALUES.put(Material.DEEPSLATE_EMERALD_ORE, 45);
        BLOCK_VALUES.put(Material.GOLD_ORE, 25);
        BLOCK_VALUES.put(Material.DEEPSLATE_GOLD_ORE, 25);
        BLOCK_VALUES.put(Material.IRON_ORE, 12);
        BLOCK_VALUES.put(Material.DEEPSLATE_IRON_ORE, 12);
        BLOCK_VALUES.put(Material.LAPIS_ORE, 10);
        BLOCK_VALUES.put(Material.DEEPSLATE_LAPIS_ORE, 10);

        // Special Blocks
        BLOCK_VALUES.put(Material.SPAWNER, 75);
        BLOCK_VALUES.put(Material.ENCHANTING_TABLE, 30);
        BLOCK_VALUES.put(Material.ANVIL, 20);
        BLOCK_VALUES.put(Material.BREWING_STAND, 15);

        // Farming
        BLOCK_VALUES.put(Material.FARMLAND, 2);
        BLOCK_VALUES.put(Material.WHEAT, 1);
        BLOCK_VALUES.put(Material.CARROTS, 1);
        BLOCK_VALUES.put(Material.POTATOES, 1);
        BLOCK_VALUES.put(Material.MELON, 2);
        BLOCK_VALUES.put(Material.PUMPKIN, 2);

        // Building Materials (low value)
        BLOCK_VALUES.put(Material.STONE, 1);
        BLOCK_VALUES.put(Material.COBBLESTONE, 1);
        BLOCK_VALUES.put(Material.DIRT, 1);
        BLOCK_VALUES.put(Material.GRASS_BLOCK, 1);
        BLOCK_VALUES.put(Material.OAK_LOG, 2);
        BLOCK_VALUES.put(Material.SPRUCE_LOG, 2);
        BLOCK_VALUES.put(Material.BIRCH_LOG, 2);
        BLOCK_VALUES.put(Material.JUNGLE_LOG, 2);
        BLOCK_VALUES.put(Material.ACACIA_LOG, 2);
        BLOCK_VALUES.put(Material.DARK_OAK_LOG, 2);
    }

    /**
     * Get the value of a block for island level calculation
     * 
     * @param material The block material
     * @return The block value (0 if not valued)
     */
    public static int getBlockValue(Material material) {
        return BLOCK_VALUES.getOrDefault(material, 0);
    }

    /**
     * Check if a block has value
     * 
     * @param material The block material
     * @return true if the block has value
     */
    public static boolean hasValue(Material material) {
        return BLOCK_VALUES.containsKey(material);
    }
}
