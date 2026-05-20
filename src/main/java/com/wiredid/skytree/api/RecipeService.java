package com.wiredid.skytree.api;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Recipe service for all custom crafting
 */
public interface RecipeService {

    /**
     * Get sieve drops for a material with specific mesh
     */
    List<ItemStack> getSieveDrops(Material material, String meshType);

    /**
     * Get barrel composting result
     */
    ItemStack getBarrelResult(Material input);

    /**
     * Get crucible melting result
     */
    Material getCrucibleResult(Material input);

    /**
     * Check if material can be sieved
     */
    boolean canSieve(Material material);

    /**
     * Check if material can be composted
     */
    boolean canCompost(Material material);

    /**
     * Check if material can be melted
     */
    boolean canMelt(Material material);

    /**
     * Get all compression recipes (9 items -> 1 compressed)
     */
    Map<String, ItemStack> getCompressionRecipes();

    /**
     * Get furnace smelting result (supports vanilla and custom recipes)
     */
    ItemStack getFurnaceResult(ItemStack input);

    /**
     * Register a custom furnace recipe
     */
    void registerCustomFurnaceRecipe(Material input, ItemStack result);

    /**
     * Register a custom compression recipe
     */
    void registerCustomCompressionRecipe(String key, ItemStack result);

    /**
     * Get auto-crafter recipes
     */
    Map<String, AutoCrafterRecipe> getAutoCrafterRecipes();

    /**
     * Data class for auto-crafter recipes
     */
    class AutoCrafterRecipe {
        private final String id;
        private final List<ItemStack> inputs;
        private final ItemStack output;
        private final int processTime;

        public AutoCrafterRecipe(String id, List<ItemStack> inputs, ItemStack output, int processTime) {
            this.id = id;
            this.inputs = inputs;
            this.output = output;
            this.processTime = processTime;
        }

        public String getId() {
            return id;
        }

        public List<ItemStack> getInputs() {
            return inputs;
        }

        public ItemStack getOutput() {
            return output;
        }

        public int getProcessTime() {
            return processTime;
        }
    }
}
