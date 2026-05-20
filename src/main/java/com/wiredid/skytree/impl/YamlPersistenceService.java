package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.PersistenceService;
import com.wiredid.skytree.api.Transaction;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.model.IslandMember;
import com.wiredid.skytree.model.IslandRole;
import com.wiredid.skytree.model.PlayerData;
import com.wiredid.skytree.model.MinionData;
import com.wiredid.skytree.model.MinionType;
import com.wiredid.skytree.model.MinionSkin;
import com.wiredid.skytree.model.ShardTransaction;
import com.wiredid.skytree.model.Rank;
import com.wiredid.skytree.model.Investment;
import com.wiredid.skytree.model.InvestmentType;
import com.wiredid.skytree.model.BaitData;
import com.wiredid.skytree.model.BaitType;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YAML-based persistence implementation
 */
public class YamlPersistenceService implements PersistenceService {

    private final SkytreePlugin plugin;
    private final File dataFolder;
    private final File islandsFolder;
    private final File playersFolder;
    private final File minionsFile;
    private final File balancesFile;
    private YamlConfiguration balancesConfig;
    private YamlConfiguration minionsConfig;

    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();

    public YamlPersistenceService(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.islandsFolder = new File(dataFolder, "islands");
        this.playersFolder = new File(dataFolder, "players");
        this.minionsFile = new File(dataFolder, "minions.yml");
        this.balancesFile = new File(dataFolder, "balances.yml");

        if (!islandsFolder.exists()) islandsFolder.mkdirs();
        if (!playersFolder.exists()) playersFolder.mkdirs();

        loadConfigs();
        startAsyncSaveTask();
    }

    private void loadConfigs() {
        try {
            if (!balancesFile.exists()) balancesFile.createNewFile();
            if (!minionsFile.exists()) minionsFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create data files!");
        }
        balancesConfig = YamlConfiguration.loadConfiguration(balancesFile);
        minionsConfig = YamlConfiguration.loadConfiguration(minionsFile);
    }

    private void startAsyncSaveTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveDirtyPlayers();
            saveBalances();
            saveMinionsConfig();
        }, 1200L, 1200L); // Every 1 minute
    }

    private synchronized void saveBalances() {
        try {
            balancesConfig.save(balancesFile);
        } catch (IOException e) {
            plugin.getLogger().severe(() -> "Failed to save balances: " + e.getMessage());
        }
    }

    private synchronized void saveMinionsConfig() {
        try {
            minionsConfig.save(minionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe(() -> "Failed to save minions: " + e.getMessage());
        }
    }

    private void saveDirtyPlayers() {
        for (UUID uuid : new HashSet<>(dirtyPlayers)) {
            PlayerData data = playerDataCache.get(uuid);
            if (data != null) {
                internalSavePlayerData(data);
            }
            dirtyPlayers.remove(uuid);
        }
    }

    @Override
    public Optional<Island> loadIsland(UUID ownerId) {
        File file = new File(islandsFolder, ownerId.toString() + ".yml");
        if (!file.exists()) return Optional.empty();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String islandIdRaw = config.getString("islandId");
        UUID islandId;
        try {
            if (islandIdRaw == null || islandIdRaw.isBlank()) {
                plugin.getLogger().warning("Skipping island file with missing islandId: " + file.getName());
                return Optional.empty();
            }
            islandId = UUID.fromString(islandIdRaw);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Skipping island file with invalid islandId '" + islandIdRaw + "': " + file.getName());
            return Optional.empty();
        }

        Location spawn = deserializeLocation(config.getString("spawn"));
        Location center = deserializeLocation(config.getString("center"));
        String gridId = config.getString("gridId");

        Island island = new Island(ownerId, islandId, spawn, center, gridId);
        island.setLevel(config.getInt("level"));
        island.setSize(config.getInt("size"));
        island.setBiome(config.getString("biome", "PLAINS"));
        island.setDescription(config.getString("description"));

        // Load Members
        ConfigurationSection memberSec = config.getConfigurationSection("members");
        if (memberSec != null) {
            for (String uuidStr : memberSec.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Skipping member with invalid UUID '" + uuidStr + "' in " + file.getName());
                    continue;
                }

                String roleRaw = memberSec.getString(uuidStr + ".role");
                IslandRole role;
                try {
                    role = IslandRole.valueOf(roleRaw);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Invalid member role '" + roleRaw + "' for " + uuidStr + " in " + file.getName() + ", defaulting to MEMBER");
                    role = IslandRole.MEMBER;
                }
                island.getMembers().add(new IslandMember(uuid, role));
            }
        }

        // Load Settings
        ConfigurationSection settingsSec = config.getConfigurationSection("settings");
        if (settingsSec != null) {
            for (String key : settingsSec.getKeys(false)) {
                island.getSettings().put(key, settingsSec.getBoolean(key));
            }
        }

        // Load Upgrades
        ConfigurationSection upgradeSec = config.getConfigurationSection("upgrades");
        if (upgradeSec != null) {
            for (String key : upgradeSec.getKeys(false)) {
                island.getUpgrades().put(key, upgradeSec.getInt(key));
            }
        }

        return Optional.of(island);
    }

    @Override
    public void saveIsland(Island island) {
        File file = new File(islandsFolder, island.getOwnerUUID().toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("islandId", island.getIslandId().toString());
        config.set("owner", island.getOwnerUUID().toString());
        config.set("spawn", serializeLocation(island.getSpawnLocation()));
        config.set("center", serializeLocation(island.getCenter()));
        config.set("gridId", island.getGridId());
        config.set("level", island.getLevel());
        config.set("size", island.getSize());
        config.set("biome", island.getBiome());
        config.set("description", island.getDescription());

        // Save Members
        for (IslandMember member : island.getMembers()) {
            String path = "members." + member.getUuid().toString();
            config.set(path + ".role", member.getRole().name());
        }

        // Save Settings
        for (Map.Entry<String, Boolean> entry : island.getSettings().entrySet()) {
            config.set("settings." + entry.getKey(), entry.getValue());
        }

        // Save Upgrades
        for (Map.Entry<String, Integer> entry : island.getUpgrades().entrySet()) {
            config.set("upgrades." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save island data for: " + island.getOwnerUUID());
        }
    }

    @Override
    public void deleteIsland(UUID ownerId) {
        File file = new File(islandsFolder, ownerId.toString() + ".yml");
        if (file.exists()) file.delete();
    }

    @Override
    public int getNextIslandId() {
        int nextId = balancesConfig.getInt("system.nextIslandId", 1);
        balancesConfig.set("system.nextIslandId", nextId + 1);
        saveBalances();
        return nextId;
    }

    @Override
    public void saveBalance(UUID uuid, double amount) {
        balancesConfig.set(uuid.toString(), amount);
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        return balancesConfig.contains(uuid.toString());
    }

    @Override
    public double loadBalance(UUID uuid) {
        return balancesConfig.getDouble(uuid.toString(), 0);
    }

    @Override
    public Map<UUID, Double> getAllBalances() {
        Map<UUID, Double> map = new HashMap<>();
        for (String key : balancesConfig.getKeys(false)) {
            if (key.equals("system")) continue;
            try {
                map.put(UUID.fromString(key), balancesConfig.getDouble(key));
            } catch (Exception e) {
                plugin.getLogger().warning("[Persistence] Invalid UUID key in balances: " + key);
            }
        }
        return map;
    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid);
        }

        PlayerData data = new PlayerData(uuid);
        File file = new File(playersFolder, uuid.toString() + ".yml");
        String path = "data";

        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            data.setGachaPity(config.getInt(path + ".gachaPity", 0));
            data.setThirst(config.getDouble(path + ".thirst", 100.0));

            String rankStr = config.getString(path + ".rank", "IOLITE");
            try {
                data.setRank(Rank.valueOf(rankStr));
            } catch (Exception e) {
                data.setRank(Rank.IOLITE);
            }

            data.setQuestPoints(config.getInt(path + ".questPoints", 0));
            data.setPrestigeLevel(config.getInt(path + ".prestigeLevel", 0));
            data.setCustomPrefix(config.getString(path + ".customPrefix"));
            data.setNickname(config.getString(path + ".nickname"));

            String rodIdStr = config.getString(path + ".activeRodId");
            if (rodIdStr != null && !rodIdStr.isEmpty()) {
                try {
                    data.setActiveRodId(UUID.fromString(rodIdStr));
                } catch (Exception e) {
                    plugin.getLogger().warning("[Persistence] Invalid rod ID for player " + uuid);
                }
            }

            // Load Homes
            ConfigurationSection homesSec = config.getConfigurationSection(path + ".homes");
            if (homesSec != null) {
                for (String homeName : homesSec.getKeys(false)) {
                    Location loc = deserializeLocation(homesSec.getString(homeName));
                    if (loc != null) data.addHome(homeName, loc);
                }
            }

            // Load Quest Progress
            ConfigurationSection qpSec = config.getConfigurationSection(path + ".questProgress");
            if (qpSec != null) {
                for (String qId : qpSec.getKeys(false)) {
                    data.getQuestProgress().put(qId, qpSec.getInt(qId));
                }
            }

            if (config.contains(path + ".completedQuests")) {
                data.getCompletedQuests().addAll(config.getStringList(path + ".completedQuests"));
            }

            data.setActiveQuestId(config.getString(path + ".activeQuestId"));

            // Load Stored Rods
            List<?> rodList = config.getList(path + ".storedRods");
            if (rodList != null) {
                for (Object obj : rodList) {
                    if (obj instanceof org.bukkit.inventory.ItemStack stack) {
                        data.getStoredRods().add(stack);
                    }
                }
            }

            if (config.contains(path + ".achievements")) {
                data.getAchievements().addAll(config.getStringList(path + ".achievements"));
            }

            if (config.contains(path + ".unlockedSkins")) {
                data.getUnlockedSkins().addAll(config.getStringList(path + ".unlockedSkins"));
            }

            List<?> vaultList = config.getList(path + ".vault");
            if (vaultList != null) {
                for (Object obj : vaultList) {
                    if (obj instanceof org.bukkit.inventory.ItemStack stack) {
                        data.getVaultItems().add(stack);
                    }
                }
            }

            if (config.contains(path + ".trashFilter")) {
                data.getTrashFilter().addAll(config.getStringList(path + ".trashFilter"));
            }

            // Load Kit Cooldowns
            ConfigurationSection kitSec = config.getConfigurationSection(path + ".kitCooldowns");
            if (kitSec != null) {
                for (String kid : kitSec.getKeys(false)) {
                    data.setKitCooldown(kid, kitSec.getLong(kid));
                }
            }

            // Load Settings
            ConfigurationSection settingsSec = config.getConfigurationSection(path + ".settings");
            if (settingsSec != null) {
                for (String key : settingsSec.getKeys(false)) {
                    data.getSettings().put(key, settingsSec.getBoolean(key));
                }
            }

            // Load History
            loadHistory(config, path + ".shardHistory", data.getShardHistory(), ShardTransaction.class);
            loadHistory(config, path + ".econHistory", data.getEconHistory(), Transaction.class);

            // Load Investments
            ConfigurationSection invSec = config.getConfigurationSection(path + ".investments");
            if (invSec != null) {
                for (String key : invSec.getKeys(false)) {
                    ConfigurationSection entry = invSec.getConfigurationSection(key);
                    if (entry == null) continue;
                    
                    Investment inv = new Investment(
                            UUID.fromString(entry.getString("id")),
                            uuid,
                            InvestmentType.valueOf(entry.getString("type")),
                            entry.getString("assetId"),
                            entry.getDouble("amount"),
                            entry.getInt("shares"),
                            entry.getLong("purchaseTime"),
                            entry.getDouble("purchasePrice"),
                            entry.getLong("maturityTime"),
                            entry.getBoolean("active"));
                    data.getInvestments().add(inv);
                }
            }

            // Load Active Bait
            ConfigurationSection bSec = config.getConfigurationSection(path + ".activeBait");
            if (bSec != null) {
                data.setActiveBait(new BaitData(
                        BaitType.valueOf(bSec.getString("type")),
                        bSec.getInt("quantity")));
            }

            // Load Minions
            ConfigurationSection minSec = config.getConfigurationSection(path + ".minions");
            if (minSec != null) {
                for (String key : minSec.getKeys(false)) {
                    ConfigurationSection entry = minSec.getConfigurationSection(key);
                    if (entry == null) continue;

                    MinionData m = new MinionData(
                            UUID.fromString(entry.getString("id")),
                            uuid,
                            UUID.fromString(entry.getString("islandId")),
                            MinionType.valueOf(entry.getString("type")),
                            entry.getInt("level"),
                            MinionSkin.valueOf(entry.getString("skin")),
                            deserializeLocation(entry.getString("loc")),
                            entry.getBoolean("active"),
                            entry.getBoolean("storageUnlocked"));

                    List<?> mStorageList = entry.getList("storage");
                    if (mStorageList != null) {
                        List<org.bukkit.inventory.ItemStack> storage = new ArrayList<>();
                        for (Object obj : mStorageList) {
                            if (obj instanceof org.bukkit.inventory.ItemStack stack) storage.add(stack);
                        }
                        m.setStorage(storage);
                    }
                    data.getMinions().add(m);
                }
            }

            data.setNormalInventory(loadInventory(config, path + ".normalInventory"));
            data.setIslandInventory(loadInventory(config, path + ".islandInventory"));
        }

        data.setPersistenceObserver(pd -> dirtyPlayers.add(pd.getUuid()));
        playerDataCache.put(uuid, data);
        return data;
    }

    private org.bukkit.inventory.ItemStack[] loadInventory(YamlConfiguration config, String path) {
        List<?> list = config.getList(path);
        org.bukkit.inventory.ItemStack[] inv = new org.bukkit.inventory.ItemStack[41];
        if (list != null) {
            for (int i = 0; i < Math.min(list.size(), 41); i++) {
                if (list.get(i) instanceof org.bukkit.inventory.ItemStack stack) inv[i] = stack;
            }
        }
        return inv;
    }

    private <T> void loadHistory(YamlConfiguration config, String path, List<T> list, Class<T> clazz) {
        ConfigurationSection sec = config.getConfigurationSection(path);
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection entry = sec.getConfigurationSection(key);
            if (entry == null) continue;
            if (clazz == ShardTransaction.class) {
                ((List<ShardTransaction>) list).add(new ShardTransaction(
                        ShardTransaction.TransactionType.valueOf(entry.getString("type")),
                        entry.getInt("amount"),
                        entry.getString("source"),
                        entry.getInt("balanceAfter")));
            } else if (clazz == Transaction.class) {
                ((List<Transaction>) list).add(new Transaction(
                        LocalDateTime.parse(entry.getString("time")),
                        entry.getDouble("amount"),
                        entry.getString("type"),
                        entry.getString("reason")));
            }
        }
    }

    @Override
    public void savePlayerData(PlayerData data) {
        playerDataCache.put(data.getUuid(), data);
        dirtyPlayers.add(data.getUuid());
    }

    @Override
    public void unloadPlayerData(UUID uuid) {
        PlayerData data = playerDataCache.remove(uuid);
        if (data != null && dirtyPlayers.contains(uuid)) {
            internalSavePlayerData(data);
            dirtyPlayers.remove(uuid);
        }
    }

    private void internalSavePlayerData(PlayerData data) {
        File file = new File(playersFolder, data.getUuid().toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        String path = "data";

        config.set(path + ".rank", data.getRank().name());
        config.set(path + ".gachaPity", data.getGachaPity());
        config.set(path + ".thirst", data.getThirst());
        config.set(path + ".questPoints", data.getQuestPoints());
        config.set(path + ".prestigeLevel", data.getPrestigeLevel());
        config.set(path + ".customPrefix", data.getCustomPrefix());
        config.set(path + ".nickname", data.getNickname());
        if (data.getActiveRodId() != null) config.set(path + ".activeRodId", data.getActiveRodId().toString());

        for (Map.Entry<String, Location> entry : data.getHomes().entrySet()) {
            config.set(path + ".homes." + entry.getKey(), serializeLocation(entry.getValue()));
        }

        config.set(path + ".questProgress", data.getQuestProgress());
        config.set(path + ".completedQuests", new ArrayList<>(data.getCompletedQuests()));
        config.set(path + ".activeQuestId", data.getActiveQuestId());
        config.set(path + ".storedRods", data.getStoredRods());
        config.set(path + ".achievements", new ArrayList<>(data.getAchievements()));
        config.set(path + ".unlockedSkins", new ArrayList<>(data.getUnlockedSkins()));
        config.set(path + ".vault", data.getVaultItems());
        config.set(path + ".trashFilter", new ArrayList<>(data.getTrashFilter()));

        for (Map.Entry<String, Long> entry : data.getKitCooldowns().entrySet()) {
            config.set(path + ".kitCooldowns." + entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Boolean> entry : data.getSettings().entrySet()) {
            config.set(path + ".settings." + entry.getKey(), entry.getValue());
        }

        // Save Shard History (Top 20)
        List<ShardTransaction> shards = data.getShardHistory();
        if (shards.size() > 20) shards = shards.subList(shards.size() - 20, shards.size());
        for (int i = 0; i < shards.size(); i++) {
            ShardTransaction st = shards.get(i);
            String hPath = path + ".shardHistory.tx" + i;
            config.set(hPath + ".type", st.getType().name());
            config.set(hPath + ".amount", st.getAmount());
            config.set(hPath + ".source", st.getSource());
            config.set(hPath + ".balanceAfter", st.getBalanceAfter());
        }

        // Save Econ History (Top 20)
        List<Transaction> econ = data.getEconHistory();
        if (econ.size() > 20) econ = econ.subList(econ.size() - 20, econ.size());
        for (int i = 0; i < econ.size(); i++) {
            Transaction tx = econ.get(i);
            String hPath = path + ".econHistory.tx" + i;
            config.set(hPath + ".time", tx.getTimestamp().toString());
            config.set(hPath + ".amount", tx.getAmount());
            config.set(hPath + ".type", tx.getType());
            config.set(hPath + ".reason", tx.getReason());
        }

        // Save Investments
        for (int i = 0; i < data.getInvestments().size(); i++) {
            Investment inv = data.getInvestments().get(i);
            String iPath = path + ".investments.inv" + i;
            config.set(iPath + ".id", inv.getInvestmentId().toString());
            config.set(iPath + ".type", inv.getType().name());
            config.set(iPath + ".assetId", inv.getAssetId());
            config.set(iPath + ".amount", inv.getAmount());
            config.set(iPath + ".shares", inv.getShares());
            config.set(iPath + ".purchaseTime", inv.getPurchaseTime());
            config.set(iPath + ".purchasePrice", inv.getPurchasePrice());
            config.set(iPath + ".maturityTime", inv.getMaturityTime());
            config.set(iPath + ".active", inv.isActive());
        }

        // Save Active Bait
        if (data.getActiveBait() != null) {
            config.set(path + ".activeBait.type", data.getActiveBait().getType().name());
            config.set(path + ".activeBait.quantity", data.getActiveBait().getQuantity());
        }

        // Save Minions
        for (int i = 0; i < data.getMinions().size(); i++) {
            MinionData m = data.getMinions().get(i);
            saveMinionToSection(config.createSection(path + ".minions.m" + i), m);
        }

        config.set(path + ".normalInventory", data.getNormalInventory());
        config.set(path + ".islandInventory", data.getIslandInventory());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data: " + data.getUuid());
        }
    }

    private void saveMinionToSection(ConfigurationSection sec, MinionData m) {
        sec.set("id", m.getMinionId().toString());
        sec.set("ownerId", m.getOwnerId().toString());
        sec.set("islandId", m.getIslandId().toString());
        sec.set("type", m.getType().name());
        sec.set("level", m.getLevel());
        sec.set("skin", m.getSkin().name());
        sec.set("active", m.isActive());
        sec.set("storageUnlocked", m.isStorageUnlocked());
        sec.set("loc", serializeLocation(m.getLocation()));
        sec.set("storage", m.getStorage());
    }

    @Override
    public void saveMinion(MinionData data) {
        minionsConfig.set("minions." + data.getMinionId().toString(), ""); // Placeholder
        saveMinionToSection(minionsConfig.getConfigurationSection("minions." + data.getMinionId().toString()), data);
    }

    @Override
    public void saveAllMinions(List<MinionData> minions) {
        minionsConfig.set("minions", null);
        for (MinionData m : minions) saveMinion(m);
    }

    @Override
    public List<MinionData> loadAllMinions() {
        List<MinionData> list = new ArrayList<>();
        ConfigurationSection sec = minionsConfig.getConfigurationSection("minions");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection entry = sec.getConfigurationSection(key);
                if (entry == null) continue;
                MinionData m = new MinionData(
                        UUID.fromString(key),
                        UUID.fromString(entry.getString("ownerId")),
                        UUID.fromString(entry.getString("islandId")),
                        MinionType.valueOf(entry.getString("type")),
                        entry.getInt("level"),
                        MinionSkin.valueOf(entry.getString("skin")),
                        deserializeLocation(entry.getString("loc")),
                        entry.getBoolean("active"),
                        entry.getBoolean("storageUnlocked"));
                
                List<?> mStorageList = entry.getList("storage");
                if (mStorageList != null) {
                    List<org.bukkit.inventory.ItemStack> storage = new ArrayList<>();
                    for (Object obj : mStorageList) {
                        if (obj instanceof org.bukkit.inventory.ItemStack stack) storage.add(stack);
                    }
                    m.setStorage(storage);
                }
                list.add(m);
            }
        }
        return list;
    }

    private String serializeLocation(Location loc) {
        if (loc == null) return null;
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private Location deserializeLocation(String str) {
        if (str == null || str.isEmpty()) return null;
        String[] parts = str.split(",");
        if (parts.length >= 4) {
            try {
                Location loc = new Location(plugin.getServer().getWorld(parts[0]),
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]));
                if (parts.length == 6) {
                    loc.setYaw(Float.parseFloat(parts[4]));
                    loc.setPitch(Float.parseFloat(parts[5]));
                }
                return loc;
            } catch (Exception e) {
                plugin.getLogger().warning("[Persistence] Failed to deserialize location: " + str);
            }
        }
        return null;
    }

    @Override
    public void shutdown() {
        saveDirtyPlayers();
        saveBalances();
        saveMinionsConfig();
    }
}
