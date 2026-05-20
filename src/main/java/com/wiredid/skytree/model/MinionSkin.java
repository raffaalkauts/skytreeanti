package com.wiredid.skytree.model;

/**
 * Cosmetic skins for minions
 */
public enum MinionSkin {
    STEVE,
    ALEX,
    ZOMBIE,
    SKELETON,
    CREEPER,
    ENDERMAN,
    VILLAGER,
    PIGLIN;

    /**
     * Get skin from string (case-insensitive)
     */
    public static MinionSkin fromString(String str) {
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STEVE; // Default
        }
    }

    /**
     * Check if skin requires unlock
     */
    public boolean requiresUnlock() {
        return this != STEVE && this != ALEX;
    }
}
