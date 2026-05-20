package com.wiredid.skytree.model;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private volatile int gachaPity;
    private volatile double thirst;
    private final java.util.Map<String, Integer> questProgress;
    private final java.util.Set<String> completedQuests;
    private volatile String activeQuestId;
    private final java.util.Map<String, org.bukkit.Location> homes;
    private final java.util.List<org.bukkit.inventory.ItemStack> storedRods;
    private final java.util.Set<String> achievements;
    private final java.util.List<org.bukkit.inventory.ItemStack> vaultItems;
    private final java.util.Set<String> trashFilter;
    private volatile Rank rank;
    private volatile int questPoints;
    private volatile int prestigeLevel;
    private volatile String customPrefix;
    private final java.util.List<com.wiredid.skytree.model.ShardTransaction> shardHistory;
    private final java.util.List<com.wiredid.skytree.model.Investment> investments;
    private volatile com.wiredid.skytree.model.BaitData activeBait;
    private final java.util.List<com.wiredid.skytree.model.MinionData> minions;
    private final java.util.Set<String> unlockedSkins;
    private final java.util.List<com.wiredid.skytree.api.Transaction> econHistory;

    private final java.util.Map<String, Long> kitCooldowns;
    private final java.util.Map<String, Boolean> settings;
    private volatile String nickname;

    // Multi-Inventory System (41 slots: 36 main + 4 armor + 1 offhand)
    private final java.util.List<org.bukkit.inventory.ItemStack> normalInventory;
    private final java.util.List<org.bukkit.inventory.ItemStack> islandInventory;
    private volatile UUID activeRodId;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.rank = Rank.IOLITE;
        this.gachaPity = 0;
        this.thirst = 100.0;
        this.homes = new java.util.concurrent.ConcurrentHashMap<>();
        this.questProgress = new java.util.concurrent.ConcurrentHashMap<>();
        this.completedQuests = java.util.concurrent.ConcurrentHashMap.newKeySet();
        this.activeQuestId = null;
        this.storedRods = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.achievements = java.util.concurrent.ConcurrentHashMap.newKeySet();
        this.vaultItems = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.trashFilter = java.util.concurrent.ConcurrentHashMap.newKeySet();
        this.kitCooldowns = new java.util.concurrent.ConcurrentHashMap<>();
        this.settings = new java.util.concurrent.ConcurrentHashMap<>();
        this.shardHistory = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.investments = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.minions = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.unlockedSkins = java.util.concurrent.ConcurrentHashMap.newKeySet();
        this.econHistory = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.questPoints = 0;
        this.prestigeLevel = 0;
        this.customPrefix = null;
        this.activeRodId = null;

        // Initialize empty inventories (41 slots: 36 main + 4 armor + 1 offhand)
        this.normalInventory = new java.util.concurrent.CopyOnWriteArrayList<>(new org.bukkit.inventory.ItemStack[41]);
        this.islandInventory = new java.util.concurrent.CopyOnWriteArrayList<>(new org.bukkit.inventory.ItemStack[41]);

        // Default settings
        settings.put("scoreboard", true);
        settings.put("actionbar", true);
        settings.put("pms", true);
        settings.put("worth_display", true);
    }

    private transient java.util.function.Consumer<PlayerData> persistenceObserver;

    public void setPersistenceObserver(java.util.function.Consumer<PlayerData> observer) {
        this.persistenceObserver = observer;
    }

    private void markDirty() {
        if (persistenceObserver != null) {
            persistenceObserver.accept(this);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public java.util.Map<String, Long> getKitCooldowns() {
        return kitCooldowns;
    }

    public long getKitCooldown(String kitId) {
        return kitCooldowns.getOrDefault(kitId, 0L);
    }

    public void setKitCooldown(String kitId, long timestamp) {
        kitCooldowns.put(kitId, timestamp);
        markDirty();
    }

    public UUID getActiveRodId() {
        return activeRodId;
    }

    public void setActiveRodId(UUID activeRodId) {
        this.activeRodId = activeRodId;
        markDirty();
    }

    public int getGachaPity() {
        return gachaPity;
    }

    public void setGachaPity(int gachaPity) {
        this.gachaPity = gachaPity;
        markDirty();
    }

    public double getThirst() {
        return thirst;
    }

    public void setThirst(double thirst) {
        this.thirst = thirst;
        markDirty();
    }

    public java.util.Map<String, org.bukkit.Location> getHomes() {
        return homes;
    }

    public void addHome(String name, org.bukkit.Location location) {
        homes.put(name.toLowerCase(), location);
        markDirty();
    }

    public void removeHome(String name) {
        homes.remove(name.toLowerCase());
        markDirty();
    }

    public org.bukkit.Location getHome(String name) {
        return homes.get(name.toLowerCase());
    }

    public java.util.List<org.bukkit.inventory.ItemStack> getStoredRods() {
        return storedRods;
    }

    public java.util.Map<String, Integer> getQuestProgress() {
        return questProgress;
    }

    public java.util.Set<String> getCompletedQuests() {
        return completedQuests;
    }

    public String getActiveQuestId() {
        return activeQuestId;
    }

    public java.util.Set<String> getAchievements() {
        return achievements;
    }

    public void setActiveQuestId(String activeQuestId) {
        this.activeQuestId = activeQuestId;
        markDirty();
    }

    public java.util.List<org.bukkit.inventory.ItemStack> getVaultItems() {
        return vaultItems;
    }

    public java.util.Set<String> getTrashFilter() {
        return trashFilter;
    }

    public java.util.Map<String, Boolean> getSettings() {
        return settings;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
        markDirty();
    }

    public int getQuestPoints() {
        return questPoints;
    }

    public void setQuestPoints(int questPoints) {
        this.questPoints = questPoints;
        markDirty();
    }

    public void addQuestPoints(int amount) {
        this.questPoints += amount;
        markDirty();
    }

    public int getPrestigeLevel() {
        return prestigeLevel;
    }

    public void setPrestigeLevel(int prestigeLevel) {
        this.prestigeLevel = prestigeLevel;
        markDirty();
    }

    public String getCustomPrefix() {
        return customPrefix;
    }

    public void setCustomPrefix(String customPrefix) {
        this.customPrefix = customPrefix;
        markDirty();
    }

    public java.util.List<com.wiredid.skytree.model.ShardTransaction> getShardHistory() {
        return shardHistory;
    }

    public java.util.List<com.wiredid.skytree.model.Investment> getInvestments() {
        return investments;
    }

    public com.wiredid.skytree.model.BaitData getActiveBait() {
        return activeBait;
    }

    public void setActiveBait(com.wiredid.skytree.model.BaitData activeBait) {
        this.activeBait = activeBait;
        markDirty();
    }

    public java.util.List<com.wiredid.skytree.model.MinionData> getMinions() {
        return minions;
    }

    public void setMinions(java.util.List<com.wiredid.skytree.model.MinionData> minionList) {
        this.minions.clear();
        this.minions.addAll(minionList);
        markDirty();
    }

    public java.util.Set<String> getUnlockedSkins() {
        return unlockedSkins;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        markDirty();
    }

    public org.bukkit.inventory.ItemStack[] getNormalInventory() {
        return normalInventory.toArray(org.bukkit.inventory.ItemStack[]::new);
    }

    public void setNormalInventory(org.bukkit.inventory.ItemStack[] inv) {
        this.normalInventory.clear();
        this.normalInventory.addAll(java.util.Arrays.asList(inv));
        markDirty();
    }

    public org.bukkit.inventory.ItemStack[] getIslandInventory() {
        return islandInventory.toArray(org.bukkit.inventory.ItemStack[]::new);
    }

    public void setIslandInventory(org.bukkit.inventory.ItemStack[] inv) {
        this.islandInventory.clear();
        this.islandInventory.addAll(java.util.Arrays.asList(inv));
        markDirty();
    }

    public java.util.List<com.wiredid.skytree.api.Transaction> getEconHistory() {
        return econHistory;
    }
}
