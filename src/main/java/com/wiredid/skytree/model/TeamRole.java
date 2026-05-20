package com.wiredid.skytree.model;

/**
 * Team roles with hierarchical permissions
 */
public enum TeamRole {
    /**
     * Island owner, full control
     */
    LEADER(3),

    /**
     * Can manage members and resources
     */
    OFFICER(2),

    /**
     * Regular team member
     */
    MEMBER(1);

    private final int priority;

    TeamRole(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Check if this role has at least the specified role's priority
     */
    public boolean hasRole(TeamRole required) {
        return this.priority >= required.priority;
    }

    /**
     * Get role from string (case-insensitive)
     */
    public static TeamRole fromString(String str) {
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEMBER; // Default to lowest
        }
    }
}
