package com.wiredid.skytree.api;

import java.util.UUID;

public interface MonetizationService {

    /**
     * Get the active global money multiplier
     */
    double getGlobalMoneyMultiplier();

    /**
     * Set the global money multiplier for a duration
     */
    void setGlobalMoneyMultiplier(double multiplier, long durationMinutes);

    /**
     * Get player-specific booster
     */
    double getPlayerMultiplier(UUID playerId);

    /**
     * Add a booster to a player
     */
    void addPlayerBooster(UUID playerId, double multiplier, long durationMinutes);

    /**
     * Check if a booster is active
     */
    boolean hasActiveBooster(UUID playerId);
}
