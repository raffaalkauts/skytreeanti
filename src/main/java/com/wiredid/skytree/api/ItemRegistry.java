package com.wiredid.skytree.api;

import org.bukkit.inventory.ItemStack;

/**
 * Registry for all custom items in Skytree
 */
public interface ItemRegistry {

    /**
     * Gets a custom item by its ID
     * 
     * @param id The item ID (e.g., "crook_wood", "hammer_stone")
     * @return The ItemStack, or null if not found
     */
    ItemStack getItem(String id);

    /**
     * Checks if an ItemStack is a custom item
     * 
     * @param item The ItemStack to check
     * @return true if it's a custom item
     */
    boolean isCustomItem(ItemStack item);

    /**
     * Gets the ID of a custom item
     * 
     * @param item The ItemStack
     * @return The item ID, or null if not custom
     */
    String getItemId(ItemStack item);

    /**
     * Registers all custom items
     */
    void registerAllItems();

    /**
     * Reloads all custom items from registry/config
     */
    void reload();

    /**
     * Gets all registered item IDs
     * 
     * @return Set of item IDs
     */
    java.util.Set<String> getAllItemIds();
}
