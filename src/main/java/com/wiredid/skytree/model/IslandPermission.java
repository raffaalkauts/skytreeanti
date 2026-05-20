package com.wiredid.skytree.model;

/**
 * Island permissions for granular control
 */
public enum IslandPermission {
    /**
     * Place blocks
     */
    BUILD,

    /**
     * Break blocks
     */
    BREAK,

    /**
     * Open chests, furnaces, etc.
     */
    CONTAINER,

    /**
     * Interact with redstone (buttons, levers, pressure plates)
     */
    REDSTONE,

    /**
     * Open doors, trapdoors, fence gates
     */
    DOORS,

    /**
     * Interact with animals (breeding, leashing, shearing)
     */
    ANIMALS,

    /**
     * Attack mobs and players
     */
    PVP,

    /**
     * Use items (buckets, flint and steel, etc.)
     */
    USE_ITEMS;

    /**
     * Get default permissions for a trust level
     */
    public static IslandPermission[] getDefaultPerms(TrustLevel level) {
        return switch (level) {
            case NONE -> new IslandPermission[0];
            case VISITOR -> new IslandPermission[] { DOORS };
            case BUILDER -> new IslandPermission[] { BUILD, BREAK, DOORS, USE_ITEMS };
            case MODERATOR -> new IslandPermission[] { BUILD, BREAK, CONTAINER, REDSTONE, DOORS, ANIMALS, USE_ITEMS };
            case CO_OWNER -> values(); // All permissions
        };
    }
}
