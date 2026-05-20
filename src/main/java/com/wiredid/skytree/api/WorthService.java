package com.wiredid.skytree.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Service for calculating item worth and updating lore dynamically
 */
public interface WorthService {

    /**
     * Gets the sell price of a single item unit
     * 
     * @param item The item to check
     * @return Unit price or 0 if not sellable
     */
    double getItemSellPrice(ItemStack item);

    /**
     * Updates the lore of an item stack to show its total worth
     * 
     * @param item The item to update
     */
    void updateItemLore(ItemStack item);

    /**
     * Updates lore for all items in player's inventory
     * 
     * @param player The player to update
     */
    void updateInventoryLore(Player player);

    /**
     * Removes worth lore from all items in player's inventory
     * 
     * @param player The player to update
     */
    void removeInventoryLore(Player player);

    boolean isSimilarIgnoringWorth(ItemStack item1, ItemStack item2);

    /**
     * Strip worth lore from a single item (used for dropped items).
     *
     * @param item The item to strip lore from
     */
    void stripSingleItemLore(ItemStack item);

    /**
     * Reload prices from configuration
     */
    void reload();
}
