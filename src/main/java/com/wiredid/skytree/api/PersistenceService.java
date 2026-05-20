package com.wiredid.skytree.api;

import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.model.PlayerData;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for persisting plugin data
 */
public interface PersistenceService {

    /**
     * Saves an island to storage
     * 
     * @param island The island to save
     */
    void saveIsland(Island island);

    /**
     * Loads an island by owner UUID
     * 
     * @param ownerId The owner's UUID
     * @return Optional containing the island if found
     */
    Optional<Island> loadIsland(UUID ownerId);

    /**
     * Deletes an island from storage
     * 
     * @param ownerId The owner's UUID
     */
    void deleteIsland(UUID ownerId);

    /**
     * Gets the next available grid ID for island placement
     * 
     * @return The next grid ID
     */
    int getNextIslandId();

    /**
     * Saves player balance
     * 
     * @param uuid    The player's UUID
     * @param balance The balance amount
     */
    void saveBalance(UUID uuid, double balance);

    /**
     * Loads player balance
     * 
     * @param uuid The player's UUID
     * @return The player's balance
     */
    double loadBalance(UUID uuid);

    /**
     * Check if player has an economy account
     */
    boolean hasAccount(UUID uuid);

    /**
     * loads all balances
     *
     * @return Map of UUID to balance
     */
    java.util.Map<UUID, Double> getAllBalances();

    /**
     * Saves player data (pity, thirst)
     * 
     * @param data The player data to save
     */
    void savePlayerData(PlayerData data);

    /**
     * Loads player data
     * 
     * @param uuid The player's UUID
     * @return The player data, or new default if not found
     */
    PlayerData loadPlayerData(UUID uuid);

    /**
     * Unloads player data from memory cache
     * 
     * @param uuid The player's UUID
     */
    void unloadPlayerData(UUID uuid);

    /**
     * Saves a minion's data.
     * 
     * @param data The minion data to save
     */
    void saveMinion(com.wiredid.skytree.model.MinionData data);

    /**
     * Saves all minions at once.
     * 
     * @param minions List of minion data
     */
    void saveAllMinions(java.util.List<com.wiredid.skytree.model.MinionData> minions);

    /**
     * Loads all minions from storage.
     * 
     * @return List of minion data
     */
    java.util.List<com.wiredid.skytree.model.MinionData> loadAllMinions();

    /**
     * Shutdown and cleanup
     */
    void shutdown();
}
