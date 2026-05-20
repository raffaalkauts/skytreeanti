package com.wiredid.skytree.model;

import java.util.UUID;

/**
 * Represents a member of an island
 */
public class IslandMember {
    private final UUID uuid;
    private IslandRole role;

    public IslandMember(UUID uuid, IslandRole role) {
        this.uuid = uuid;
        this.role = role;
    }

    public UUID getUuid() {
        return uuid;
    }

    public IslandRole getRole() {
        return role;
    }

    public void setRole(IslandRole role) {
        this.role = role;
    }
}

