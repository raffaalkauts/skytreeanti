package com.wiredid.skytree.model;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.Location;
import java.util.*;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Represents a player's island with all metadata
 */
public class Island {
    private static SkytreePlugin plugin;

    public static void setPlugin(SkytreePlugin p) {
        plugin = p;
    }

    private final UUID islandId;
    private final UUID ownerUUID;
    private final Set<IslandMember> members;
    private Location spawn;
    private Location center;
    private String gridId;
    private int level;
    private int size;
    private long createdAt;
    private String biome;
    private String description;
    private final Map<String, Boolean> settings;
    private final Map<String, Integer> upgrades;

    private final IslandWarps warps;
    private final Map<String, Integer> activeDailyQuests; // ID -> Progress
    private final Set<String> completedDailyQuests; // IDs completed today
    private final Map<UUID, TrustLevel> trustedPlayers;
    private final Set<UUID> alliedIslands;
    private int spawnerCount = -1; // -1 means uninitialized

    public Island(UUID ownerUUID, UUID islandId, Location spawn, Location center, String gridId) {
        this.ownerUUID = ownerUUID;
        this.islandId = islandId;
        this.spawn = spawn;
        this.center = center;
        this.gridId = gridId;
        this.members = new HashSet<>();
        this.level = 0;
        this.size = 50; // Default size
        this.createdAt = System.currentTimeMillis();
        this.biome = "PLAINS";
        this.description = "Welcome to my island!";
        this.settings = new java.util.concurrent.ConcurrentHashMap<>();
        this.upgrades = new java.util.concurrent.ConcurrentHashMap<>();

        this.warps = new IslandWarps();
        this.activeDailyQuests = new java.util.concurrent.ConcurrentHashMap<>();
        this.completedDailyQuests = java.util.concurrent.ConcurrentHashMap.newKeySet();
        this.trustedPlayers = new java.util.concurrent.ConcurrentHashMap<>();
        this.alliedIslands = java.util.concurrent.ConcurrentHashMap.newKeySet();

        // Default settings
        settings.put("pvp", false);
        settings.put("mob_spawning", true);
        settings.put("fire_spread", false);
        settings.put("tnt", false);
        settings.put("leaf_decay", true);
        settings.put("visitor_entry", true);
        settings.put("pistons", true);
        settings.put("mob_stacking", true);
        settings.put("spawner_stacking", true);
        settings.put("drop_protection", false);
    }

    // Getters
    public UUID getIslandId() {
        return islandId;
    }

    public UUID getId() {
        return islandId;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public Set<IslandMember> getMembers() {
        return members;
    }

    public Location getSpawnLocation() {
        return spawn;
    }

    public Location getCenter() {
        return center;
    }

    public String getGridId() {
        return gridId;
    }

    public int getLevel() {
        return level;
    }

    public int getSize() {
        return size;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getBiome() {
        return biome;
    }

    public Map<String, Boolean> getSettings() {
        return settings;
    }

    public Map<String, Integer> getUpgrades() {
        return upgrades;
    }

    public String getDescription() {
        return description;
    }

    public IslandWarps getWarps() {
        return warps;
    }

    public Map<String, Integer> getActiveDailyQuests() {
        return activeDailyQuests;
    }

    public Set<String> getCompletedDailyQuests() {
        return completedDailyQuests;
    }

    public int getSpawnerCount() {
        return spawnerCount;
    }

    public void setSpawnerCount(int spawnerCount) {
        this.spawnerCount = spawnerCount;
    }

    public Location getMin() {
        int radius = size / 2;
        return center.clone().add(-radius, 0, -radius);
    }

    public Location getMax() {
        int radius = size / 2;
        return center.clone().add(radius, 0, radius);
    }

    // Setters
    public void setSpawnLocation(Location spawn) {
        this.spawn = spawn;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setBiome(String biome) {
        this.biome = biome;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<UUID, TrustLevel> getTrustedPlayers() {
        return trustedPlayers;
    }

    public void trustPlayer(UUID uuid, TrustLevel level) {
        trustedPlayers.put(uuid, level);
    }

    public void untrustPlayer(UUID uuid) {
        trustedPlayers.remove(uuid);
    }

    public Set<UUID> getAlliedIslands() {
        return alliedIslands;
    }

    public void addAlly(UUID islandId) {
        alliedIslands.add(islandId);
    }

    public void removeAlly(UUID islandId) {
        alliedIslands.remove(islandId);
    }

    public boolean isAlly(UUID islandId) {
        return alliedIslands.contains(islandId);
    }

    public TrustLevel getTrustLevel(UUID uuid) {
        if (isMember(uuid))
            return TrustLevel.CO_OWNER;
        return trustedPlayers.getOrDefault(uuid, TrustLevel.NONE);
    }

    // Member management
    public void addMember(UUID uuid, IslandRole role) {
        members.add(new IslandMember(uuid, role));
    }

    public void removeMember(UUID uuid) {
        members.removeIf(m -> m.getUuid().equals(uuid));
    }

    public boolean isMember(UUID uuid) {
        return ownerUUID.equals(uuid) || members.stream().anyMatch(m -> m.getUuid().equals(uuid));
    }

    public IslandRole getRole(UUID uuid) {
        if (ownerUUID.equals(uuid))
            return IslandRole.OWNER;
        return members.stream()
                .filter(m -> m.getUuid().equals(uuid))
                .findFirst()
                .map(IslandMember::getRole)
                .orElse(null);
    }

    // Upgrade Limit Getters
    public int getMemberLimit() {
        int level = upgrades.getOrDefault("members", 0);
        int base = plugin != null ? plugin.getConfig().getInt("island.member_limit_base", 4) : 4;
        int perLevel = plugin != null ? plugin.getConfig().getInt("island.member_limit_per_level", 2) : 2;
        return base + (level * perLevel);
    }

    public int getSpawnerLimit() {
        int level = upgrades.getOrDefault("spawners", 0);
        int base = plugin != null ? plugin.getConfig().getInt("island.spawner_limit_base", 16) : 16;
        int perLevel = plugin != null ? plugin.getConfig().getInt("island.spawner_limit_per_level", 8) : 8;
        return base + (level * perLevel);
    }

    public int getMaxSizeUpgrade() {
        int level = upgrades.getOrDefault("size", 0);
        int base = plugin != null ? plugin.getConfig().getInt("island.size_upgrade_base", 50) : 50;
        int perLevel = plugin != null ? plugin.getConfig().getInt("island.size_upgrade_per_level", 25) : 25;
        return base + (level * perLevel);
    }

    public double getGeneratorMultiplier() {
        int level = upgrades.getOrDefault("generator", 0);
        double base = plugin != null ? plugin.getConfig().getDouble("island.generator_mult_base", 1.0) : 1.0;
        double perLevel = plugin != null ? plugin.getConfig().getDouble("island.generator_mult_per_level", 0.1) : 0.1;
        return base + (level * perLevel);
    }
}
