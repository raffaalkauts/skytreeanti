package com.wiredid.skytree.fishing;

import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manages rod storage for players
 */
public class RodStorage {

    private final com.wiredid.skytree.api.PersistenceService persistence;

    public RodStorage(com.wiredid.skytree.api.PersistenceService persistence) {
        this.persistence = persistence;
    }

    /**
     * Add a rod to player's storage
     */
    public void addRod(UUID playerUuid, ItemStack rod) {
        com.wiredid.skytree.model.PlayerData data = persistence.loadPlayerData(playerUuid);
        data.getStoredRods().add(rod);
        persistence.savePlayerData(data);
    }

    /**
     * Get all stored rods for a player
     */
    public List<ItemStack> getRods(UUID playerUuid) {
        com.wiredid.skytree.model.PlayerData data = persistence.loadPlayerData(playerUuid);
        return data.getStoredRods();
    }

    /**
     * Remove a specific rod by its rod ID
     */
    public ItemStack removeRod(UUID playerUuid, UUID rodId) {
        com.wiredid.skytree.model.PlayerData data = persistence.loadPlayerData(playerUuid);
        List<ItemStack> rods = data.getStoredRods();

        for (int i = 0; i < rods.size(); i++) {
            ItemStack rod = rods.get(i);
            UUID currentRodId = NbtUtils.getRodId(rod);
            if (currentRodId != null && currentRodId.equals(rodId)) {
                ItemStack removed = rods.remove(i);
                persistence.savePlayerData(data);
                return removed;
            }
        }
        return null;
    }

    /**
     * Clear all rods for a player
     */
    public void clearRods(UUID playerUuid) {
        com.wiredid.skytree.model.PlayerData data = persistence.loadPlayerData(playerUuid);
        data.getStoredRods().clear();
        persistence.savePlayerData(data);
    }
}
