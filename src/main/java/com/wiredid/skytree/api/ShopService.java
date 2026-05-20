package com.wiredid.skytree.api;

import org.bukkit.entity.Player;

/**
 * Service for managing the shop system
 */
public interface ShopService {

    /**
     * Opens the main shop GUI for a player
     * 
     * @param player The player to open the shop for
     */
    void openShop(Player player);

    /**
     * Opens a specific shop category for a player
     * 
     * @param player   The player to open the category for
     * @param category The category name
     */
    void openCategory(Player player, String category);

    /**
     * Opens a specific shop category page for a player with a specific mode
     * 
     * @param player     The player to open the category for
     * @param categoryId The category ID
     * @param page       The page number
     * @param mode       The mode (BUY/SELL)
     */
    void openCategory(Player player, String categoryId, int page, String mode);

    /**
     * Reloads the shop configuration and rebuilds inventories
     */
    void reload();

    /**
     * @return The current shop configuration
     */
    org.bukkit.configuration.file.FileConfiguration getShopConfig();
}
