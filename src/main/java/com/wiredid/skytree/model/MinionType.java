package com.wiredid.skytree.model;

/**
 * Minion types with different AI behaviors
 */
public enum MinionType {
    /**
     * Auto-harvest and replant crops
     */
    FARMER,

    /**
     * Mine blocks in radius
     */
    MINER,

    /**
     * Chop trees and replant saplings
     */
    LUMBERJACK,

    /**
     * Auto-fish with rod
     */
    FISHER,

    /**
     * Auto-sieve gravel and sand
     */
    AUTO_SIEVE,

    /**
     * Kill mobs in radius
     */
    SLAYER,

    /**
     * Legacy cobblestone minion
     */
    COBBLE_MINION;

    /**
     * Get base speed multiplier for this minion type
     */
    public double getBaseSpeed() {
        return switch (this) {
            case FARMER -> 1.0;
            case MINER -> 0.8;
            case LUMBERJACK -> 0.9;
            case FISHER -> 0.7;
            case AUTO_SIEVE -> 1.2;
            case SLAYER -> 1.5;
            case COBBLE_MINION -> 0.8;
        };
    }

    /**
     * Get base range for this minion type
     */
    public int getBaseRange() {
        return switch (this) {
            case FARMER, AUTO_SIEVE, COBBLE_MINION -> 5;
            case MINER -> 3;
            case LUMBERJACK -> 7;
            case SLAYER -> 5;
            case FISHER -> 1; // Just the location
        };
    }
}
