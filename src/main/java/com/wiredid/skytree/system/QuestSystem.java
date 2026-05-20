package com.wiredid.skytree.system;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.PlayerData;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * Data-driven Quest system for guided progression
 */
public class QuestSystem {

    private final SkytreePlugin plugin;
    private final Map<String, QuestData> questRegistry = new LinkedHashMap<>();
    private final Map<String, CategoryData> categories = new LinkedHashMap<>();
    private boolean isInternalAction = false;

    public QuestSystem(SkytreePlugin plugin) {
        this.plugin = plugin;
        loadQuests();
    }

    public void reload() {
        loadQuests();
    }

    public void loadQuests() {
        questRegistry.clear();
        categories.clear();

        File file = new File(plugin.getDataFolder(), "quests.yml");
        if (!file.exists()) {
            plugin.saveResource("quests.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Load categories
        ConfigurationSection catSection = config.getConfigurationSection("categories");
        if (catSection != null) {
            for (String id : catSection.getKeys(false)) {
                ConfigurationSection sec = catSection.getConfigurationSection(id);
                categories.put(id, new CategoryData(
                        id,
                        sec.getString("name", id),
                        Material.valueOf(sec.getString("icon", "PAPER"))));
            }
        }

        // Load quests
        ConfigurationSection questSection = config.getConfigurationSection("quests");
        if (questSection != null) {
            for (String id : questSection.getKeys(false)) {
                ConfigurationSection sec = questSection.getConfigurationSection(id);
                questRegistry.put(id, new QuestData(
                        id,
                        sec.getString("category", "beginner"),
                        sec.getString("name", id),
                        sec.getString("description", ""),
                        QuestType.valueOf(sec.getString("type", "BLOCK_BREAK")),
                        sec.getInt("target", 1),
                        sec.getString("requires"),
                        sec.getConfigurationSection("rewards"),
                        sec.getString("hint", "")));
            }
        }
    }

    public enum QuestType {
        ISLAND_CREATE, BLOCK_BREAK, HAMMER_USE, SIEVE_USE,
        SHOP_BUY, MONEY_EARN, TEAM_INVITE,
        ISLAND_LEVEL, MOB_SPAWN, ISLAND_VISIT, FISHING
    }

    public static class CategoryData {
        public final String id, name;
        public final Material icon;

        public CategoryData(String id, String name, Material icon) {
            this.id = id;
            this.name = name;
            this.icon = icon;
        }
    }

    public static class QuestData {
        public final String id, category, name, description, requires, hint;
        public final QuestType type;
        public final int target;
        public final ConfigurationSection rewards;

        public QuestData(String id, String category, String name, String description, QuestType type, int target,
                String requires, ConfigurationSection rewards, String hint) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.description = description;
            this.type = type;
            this.target = target;
            this.requires = requires;
            this.rewards = rewards;
            this.hint = hint;
        }
    }

    public void startQuest(Player player, String questId) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
        if (questRegistry.containsKey(questId)) {
            data.setActiveQuestId(questId);
            data.getQuestProgress().putIfAbsent(questId, 0);
            plugin.getPersistenceService().savePlayerData(data);
        }
    }

    public void addProgress(Player player, QuestType type, int amount) {
        if (isInternalAction)
            return;
        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
        String activeId = data.getActiveQuestId();
        if (activeId == null)
            return;

        QuestData quest = questRegistry.get(activeId);
        if (quest == null || quest.type != type)
            return;

        if (data.getCompletedQuests().contains(activeId))
            return;

        int current = data.getQuestProgress().getOrDefault(activeId, 0);
        int newProgress = Math.min(current + amount, quest.target);
        data.getQuestProgress().put(activeId, newProgress);

        player.sendMessage("§e§l[Quest] §7Progress: §e" + newProgress + "/" + quest.target + " §7" + quest.name);

        if (newProgress >= quest.target) {
            completeQuest(player, quest, data);
        } else {
            plugin.getPersistenceService().savePlayerData(data);
        }
    }

    public void syncProgress(Player player) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
        String activeId = data.getActiveQuestId();
        if (activeId == null || data.getCompletedQuests().contains(activeId))
            return;

        QuestData quest = questRegistry.get(activeId);
        if (quest == null)
            return;

        int current = data.getQuestProgress().getOrDefault(activeId, 0);
        int synced = current;

        switch (quest.type) {
            case MONEY_EARN -> {
                double balance = plugin.getEconomyService().getBalance(player.getUniqueId());
                synced = (int) Math.max(current, balance);
            }
            case ISLAND_LEVEL -> {
                int level = plugin.getIslandService().getIsland(player.getUniqueId())
                        .map(i -> (int) i.getLevel()).orElse(0);
                synced = Math.max(current, level);
            }
            case BLOCK_BREAK -> {
                // Cannot easily sync retroactively without stats
                int mined = player.getStatistic(org.bukkit.Statistic.MINE_BLOCK, org.bukkit.Material.COBBLESTONE);
                if (mined > 0) synced = Math.max(current, mined);
            }
            default -> {
            }
        }

        if (synced > current) {
            int amountToAdd = synced - current;
            addProgress(player, quest.type, amountToAdd);
        }
    }

    private void completeQuest(Player player, QuestData quest, PlayerData data) {
        data.getCompletedQuests().add(quest.id);
        data.setActiveQuestId(null);

        player.sendMessage("§a§l✓ QUEST COMPLETE!");
        player.sendMessage("§e§l" + quest.name);

        applyRewards(player, quest.rewards);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // Auto-Advance logic
        String nextQuest = findNextInChain(quest.id);
        if (nextQuest != null) {
            startQuest(player, nextQuest);
            player.sendMessage(
                    "§a§l[Quest] §7Automatically started next quest: §e" + questRegistry.get(nextQuest).name);
        } else {
            plugin.getPersistenceService().savePlayerData(data);
        }
    }

    private void applyRewards(Player player, ConfigurationSection rewards) {
        if (rewards == null)
            return;

        isInternalAction = true;
        try {
            if (rewards.contains("money")) {
                double amount = rewards.getDouble("money");
                plugin.getEconomyService().addBalance(player.getUniqueId(), amount);
                player.sendMessage("§a+§e\u20AE " + amount + " §areward!");
            }

            if (rewards.contains("shards")) {
                int amount = rewards.getInt("shards");
                plugin.getShardService().addShards(player.getUniqueId(), amount);
                player.sendMessage("§a+§b" + amount + " Shards §areward!");
            }

            if (rewards.contains("commands")) {
                List<String> cmds = rewards.getStringList("commands");
                for (String cmd : cmds) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
                }
            }

            if (rewards.contains("xp")) {
                player.giveExp(rewards.getInt("xp"));
            }

            if (rewards.contains("message")) {
                player.sendMessage(ComponentUtil.parse(rewards.getString("message")));
            }

            if (rewards.contains("items")) {
                List<?> items = rewards.getList("items");
                if (items != null) {
                    for (Object obj : items) {
                        if (obj instanceof Map<?, ?> rawMap) {
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> map = (Map<String, Object>) rawMap;
                                Material mat = Material.valueOf((String) map.get("material"));
                                int amt = (int) map.getOrDefault("amount", 1);
                                player.getInventory().addItem(new ItemStack(mat, amt)).values().forEach(
                                        leftover -> player.getWorld().dropItem(player.getLocation(), leftover));
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        } finally {
            isInternalAction = false;
        }
    }

    private String findNextInChain(String currentId) {
        for (QuestData q : questRegistry.values()) {
            if (currentId.equals(q.requires)) {
                return q.id;
            }
        }
        return null;
    }

    public Map<String, QuestData> getQuests() {
        return questRegistry;
    }

    public Map<String, CategoryData> getCategories() {
        return categories;
    }

    public QuestData getActiveQuest(UUID uuid) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(uuid);
        return questRegistry.get(data.getActiveQuestId());
    }

    public int getProgress(UUID uuid, String questId) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(uuid);
        return data.getQuestProgress().getOrDefault(questId, 0);
    }

    public boolean isCompleted(UUID uuid, String questId) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(uuid);
        return data.getCompletedQuests().contains(questId);
    }

    public boolean canStart(UUID uuid, QuestData quest) {
        if (quest.requires == null)
            return true;
        return isCompleted(uuid, quest.requires);
    }
}
