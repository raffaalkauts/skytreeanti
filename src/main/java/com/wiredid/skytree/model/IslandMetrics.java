package com.wiredid.skytree.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Island metrics and statistics
 */
public class IslandMetrics {
    private UUID islandId;
    private int uniqueVisitors;
    private Map<UUID, Long> visitorTimestamps; // UUID -> last visit time
    private long totalVisits;
    private double calculatedValue;
    private long lastValueUpdate;

    // Custom metrics
    private long totalBlocksPlaced;
    private long totalBlocksBroken;
    private long totalMobsKilled;
    private double totalMoneyEarned;

    public IslandMetrics(UUID islandId) {
        this.islandId = islandId;
        this.visitorTimestamps = new HashMap<>();
        this.uniqueVisitors = 0;
        this.totalVisits = 0;
        this.calculatedValue = 0;
        this.lastValueUpdate = 0;
        this.totalBlocksPlaced = 0;
        this.totalBlocksBroken = 0;
        this.totalMobsKilled = 0;
        this.totalMoneyEarned = 0;
    }

    /**
     * Record a visit to the island
     */
    public void recordVisit(UUID visitorUUID) {
        totalVisits++;
        long now = System.currentTimeMillis();

        if (!visitorTimestamps.containsKey(visitorUUID)) {
            uniqueVisitors++;
        }
        visitorTimestamps.put(visitorUUID, now);
    }

    /**
     * Update the calculated value of the island
     */
    public void updateValue(double newValue) {
        this.calculatedValue = newValue;
        this.lastValueUpdate = System.currentTimeMillis();
    }

    // Getters and setters
    public UUID getIslandId() {
        return islandId;
    }

    public int getUniqueVisitors() {
        return uniqueVisitors;
    }

    public long getTotalVisits() {
        return totalVisits;
    }

    public double getCalculatedValue() {
        return calculatedValue;
    }

    public long getLastValueUpdate() {
        return lastValueUpdate;
    }

    public long getTotalBlocksPlaced() {
        return totalBlocksPlaced;
    }

    public void incrementBlocksPlaced() {
        this.totalBlocksPlaced++;
    }

    public long getTotalBlocksBroken() {
        return totalBlocksBroken;
    }

    public void incrementBlocksBroken() {
        this.totalBlocksBroken++;
    }

    public long getTotalMobsKilled() {
        return totalMobsKilled;
    }

    public void incrementMobsKilled() {
        this.totalMobsKilled++;
    }

    public double getTotalMoneyEarned() {
        return totalMoneyEarned;
    }

    public void addMoneyEarned(double amount) {
        this.totalMoneyEarned += amount;
    }

    public Map<UUID, Long> getVisitorTimestamps() {
        return new HashMap<>(visitorTimestamps);
    }
}
