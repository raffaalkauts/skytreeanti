package com.wiredid.skytree.api;

import com.wiredid.skytree.model.MinionData;
import com.wiredid.skytree.model.MinionSkin;
import com.wiredid.skytree.model.MinionType;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing minions
 */
public interface MinionService {

    /**
     * Place a minion at a location
     * 
     * @param playerId Player UUID
     * @param location Placement location
     * @param type     Minion type
     * @return MinionData if successful, null otherwise
     */
    MinionData placeMinion(UUID playerId, Location location, MinionType type);

    /**
     * Remove a minion
     * 
     * @param minionId Minion UUID
     * @return true if removed
     */
    boolean removeMinion(UUID minionId);

    /**
     * Upgrade minion to next level
     * 
     * @param minionId Minion UUID
     * @param playerId Player UUID (for payment)
     * @return true if upgraded
     */
    boolean upgradeMinion(UUID minionId, UUID playerId);

    /**
     * Change minion skin
     * 
     * @param minionId Minion UUID
     * @param skin     New skin
     * @param playerId Player UUID (to check unlock status)
     * @return true if skin changed
     */
    boolean changeSkin(UUID minionId, MinionSkin skin, UUID playerId);

    /**
     * Get minion data
     * 
     * @param minionId Minion UUID
     * @return MinionData or null
     */
    MinionData getMinionData(UUID minionId);

    /**
     * Get minion at location
     * 
     * @param location Location to check
     * @return MinionData or null
     */
    MinionData getMinionAtLocation(Location location);

    /**
     * Get all minions on an island
     * 
     * @param islandId Island UUID
     * @return List of minion data
     */
    List<MinionData> getAllMinionsByIsland(UUID islandId);

    /**
     * Get all minions owned by player
     * 
     * @param playerId Player UUID
     * @return List of minion data
     */
    List<MinionData> getPlayerMinions(UUID playerId);

    /**
     * Add item to minion storage
     * 
     * @param minionId Minion UUID
     * @param item     Item to add
     * @return true if added
     */
    boolean addToStorage(UUID minionId, ItemStack item);

    /**
     * Clear minion storage
     * 
     * @param minionId Minion UUID
     * @return List of items that were in storage
     */
    List<ItemStack> clearStorage(UUID minionId);

    /**
     * Execute minion AI (called by scheduled task)
     * 
     * @param minionId Minion UUID
     */
    void executeMinionAI(UUID minionId);

    /**
     * Check if player has unlocked a skin
     * 
     * @param playerId Player UUID
     * @param skin     Skin to check
     * @return true if unlocked
     */
    boolean hasSkinUnlocked(UUID playerId, MinionSkin skin);

    /**
     * Unlock a skin for player
     * 
     * @param playerId Player UUID
     * @param skin     Skin to unlock
     * @return true if unlocked
     */
    boolean unlockSkin(UUID playerId, MinionSkin skin);

    /**
     * Saves minion data to persistence
     * 
     * @param data Minion data
     */
    void saveMinionData(MinionData data);
}
