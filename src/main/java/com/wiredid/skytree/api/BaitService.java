package com.wiredid.skytree.api;

import com.wiredid.skytree.model.BaitData;
import com.wiredid.skytree.model.BaitType;

import java.util.UUID;

/**
 * Service for managing fishing bait system
 */
public interface BaitService {

    /**
     * Equip bait for fishing
     * 
     * @param playerId Player UUID
     * @param type     Bait type
     * @param quantity Quantity to equip
     * @return true if equipped
     */
    boolean equipBait(UUID playerId, BaitType type, int quantity);

    /**
     * Consume one bait (called on fishing)
     * 
     * @param playerId Player UUID
     * @return true if consumed
     */
    boolean consumeBait(UUID playerId);

    /**
     * Get active bait data
     * 
     * @param playerId Player UUID
     * @return BaitData or null if no bait equipped
     */
    BaitData getActiveBait(UUID playerId);

    /**
     * Remove active bait
     * 
     * @param playerId Player UUID
     */
    void removeBait(UUID playerId);

    /**
     * Get common fish bonus from bait
     * 
     * @param playerId Player UUID
     * @return Bonus multiplier (0.0-1.0)
     */
    double getCommonBonus(UUID playerId);

    /**
     * Get uncommon fish bonus from bait
     * 
     * @param playerId Player UUID
     * @return Bonus multiplier
     */
    double getUncommonBonus(UUID playerId);

    /**
     * Get rare fish bonus from bait
     * 
     * @param playerId Player UUID
     * @return Bonus multiplier
     */
    double getRareBonus(UUID playerId);

    /**
     * Get legendary fish bonus from bait
     * 
     * @param playerId Player UUID
     * @return Bonus multiplier
     */
    double getLegendaryBonus(UUID playerId);

    /**
     * Check if player has bait in inventory
     * 
     * @param playerId Player UUID
     * @param type     Bait type
     * @return Quantity owned
     */
    int getBaitCount(UUID playerId, BaitType type);

    /**
     * Give bait to player
     * 
     * @param playerId Player UUID
     * @param type     Bait type
     * @param quantity Amount
     */
    void giveBait(UUID playerId, BaitType type, int quantity);

    /**
     * Reload bait configuration
     */
    void reload();
}
