package com.wiredid.skytree.model;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.UUID;

/**
 * Action log entry for tracking block placement/breaks with rollback capability
 */
public class ActionLog {
    private UUID id;
    private UUID playerId;
    private UUID islandId;
    private Action action;
    private Material blockType;
    private Location location;
    private long timestamp;
    private String blockData; // For storing complex block states

    public enum Action {
        PLACE,
        BREAK
    }

    public ActionLog(UUID playerId, UUID islandId, Action action, Material blockType, Location location) {
        this.id = UUID.randomUUID();
        this.playerId = playerId;
        this.islandId = islandId;
        this.action = action;
        this.blockType = blockType;
        this.location = location;
        this.timestamp = System.currentTimeMillis();
        this.blockData = null;
    }

    public ActionLog(UUID playerId, UUID islandId, Action action, Material blockType, Location location,
            String blockData) {
        this(playerId, islandId, action, blockType, location);
        this.blockData = blockData;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getIslandId() {
        return islandId;
    }

    public Action getAction() {
        return action;
    }

    public Material getBlockType() {
        return blockType;
    }

    public Location getLocation() {
        return location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getBlockData() {
        return blockData;
    }

    /**
     * Check if this log is older than the specified duration in milliseconds
     */
    public boolean isOlderThan(long durationMs) {
        return (System.currentTimeMillis() - timestamp) > durationMs;
    }
}
