package com.wiredid.skytree.model;

/**
 * Trust levels for island permissions
 * Determines what actions a player can perform on another player's island
 */
public enum TrustLevel {
    /**
     * No trust at all
     */
    NONE,

    /**
     * Can only view the island, no interactions
     */
    VISITOR,

    /**
     * Can build and break blocks
     */
    BUILDER,

    /**
     * Can build, break, and access containers
     */
    MODERATOR,

    /**
     * Full permissions except island deletion
     */
    CO_OWNER;

    /**
     * Get trust level from string (case-insensitive)
     */
    public static TrustLevel fromString(String str) {
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return VISITOR; // Default to lowest permission
        }
    }

    /**
     * Check if this trust level has at least the specified level
     */
    public boolean hasLevel(TrustLevel required) {
        return this.ordinal() >= required.ordinal();
    }
}
