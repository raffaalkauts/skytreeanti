package com.wiredid.skytree.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface GachaService {
    /**
     * Attempts to spin a gacha crate for a player.
     * 
     * @param player    The player spinning.
     * @param crateType The type of crate (basic, premium).
     * @return The result ItemStack, or null if failed.
     */
    ItemStack spinGacha(Player player, String crateType);

    /**
     * Spins gacha consuming an item instead of USDT.
     */
    ItemStack spinGachaFromItem(Player player, String crateType, ItemStack paidItem);

    /**
     * Gets the player's current pity count.
     * 
     * @param player The player.
     * @return Current pity count.
     */
    int getPity(Player player);

    /**
     * Checks if the item is a valid gacha crate.
     * 
     * @param item The item to check.
     * @return The crate type key if valid, null otherwise.
     */
    String getCrateType(ItemStack item);

    /**
     * Reload gacha configuration
     */
    void reload();
}
