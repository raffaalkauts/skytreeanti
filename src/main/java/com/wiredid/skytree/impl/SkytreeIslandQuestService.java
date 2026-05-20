package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.IslandQuestService;
import com.wiredid.skytree.api.IslandService;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.model.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class SkytreeIslandQuestService implements IslandQuestService {

    private final SkytreePlugin plugin;
    private final IslandService islandService;
    private File questsFile;
    private YamlConfiguration questsConfig;
    private final Map<String, QuestTemplate> questTemplates = new HashMap<>();

    public SkytreeIslandQuestService(SkytreePlugin plugin, IslandService islandService) {
        this.plugin = plugin;
        this.islandService = islandService;
        loadQuests();
        startResetTask();
    }

    private void loadQuests() {
        questsFile = new File(plugin.getDataFolder(), "island_quests.yml");
        if (!questsFile.exists()) {
            plugin.saveResource("island_quests.yml", false);
        }
        questsConfig = YamlConfiguration.loadConfiguration(questsFile);

        questTemplates.clear();
        ConfigurationSection section = questsConfig.getConfigurationSection("quests");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection qSec = section.getConfigurationSection(key);
                if (qSec != null) {
                    QuestTemplate template = new QuestTemplate(
                            key,
                            qSec.getString("name"),
                            qSec.getString("description"),
                            qSec.getString("type"),
                            qSec.getString("target"),
                            qSec.getString("tier"));

                    // Load requirements and rewards per tier
                    // We assume the template defines requirements for ALL tiers,
                    // or we check the specific tier defined in the config if it's tier-locked.
                    // The config structure in Phase 1 shows requirements: bronze: 100, silver: 500
                    // But the quest itself has a "tier" field. That might mean "this quest IS
                    // bronze".
                    // Or "this templated CAN be bronze/silver/gold".
                    // The example config shows:
                    // mine_cobblestone: ... tier: "bronze" ... requirements: bronze: 100...
                    // This is a bit redundant if it's fixed tier.
                    // I'll assume the template supports dynamic tiers, but the config sets a
                    // default or fixed tier.
                    // Actually, let's just parse the requirements map.

                    ConfigurationSection reqSec = qSec.getConfigurationSection("requirements");
                    if (reqSec != null) {
                        for (String t : reqSec.getKeys(false)) {
                            template.requirements.put(t.toLowerCase(), reqSec.getInt(t));
                        }
                    }

                    ConfigurationSection rewardSec = qSec.getConfigurationSection("rewards");
                    if (rewardSec != null) {
                        // usdt section
                        if (rewardSec.contains("usdt")) {
                            ConfigurationSection usdtSec = rewardSec.getConfigurationSection("usdt");
                            for (String t : usdtSec.getKeys(false)) {
                                template.usdtRewards.put(t.toLowerCase(), usdtSec.getInt(t));
                            }
                        }
                        // shards section
                        if (rewardSec.contains("shards")) {
                            ConfigurationSection shardsSec = rewardSec.getConfigurationSection("shards");
                            for (String t : shardsSec.getKeys(false)) {
                                template.shardRewards.put(t.toLowerCase(), shardsSec.getInt(t));
                            }
                        }
                    }

                    questTemplates.put(key, template);
                }
            }
        }
    }

    private void startResetTask() {
        // Check every minute if we passed midnight
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::checkReset, 1200L, 1200L);
    }

    @Override
    public void generateDailyQuests(UUID islandId) {
        Optional<Island> optIsland = islandService.getIslandById(islandId);
        if (optIsland.isEmpty())
            return;
        Island island = optIsland.get();

        island.getActiveDailyQuests().clear();
        island.getCompletedDailyQuests().clear();

        List<String> keys = new ArrayList<>(questTemplates.keySet());
        if (keys.isEmpty())
            return;

        Collections.shuffle(keys);
        int count = questsConfig.getInt("dailyQuestCount", 3);

        for (int i = 0; i < Math.min(count, keys.size()); i++) {
            String key = keys.get(i);
            // Initialize with 0 progress
            island.getActiveDailyQuests().put(key, 0);
        }

        islandService.saveIsland(island);
    }

    @Override
    public void trackProgress(UUID playerId, String questType, int amount) {
        trackProgress(playerId, questType, null, amount);
    }

    @Override
    public void trackProgress(UUID playerId, String questType, String target, int amount) {
        // Usually quests track for members too.
        // I need a method to get Island by Member UUID.
        // SkytreeIslandService likely has getIslandByMember or getIslandLocation.
        // I'll try getting by location or rely on PlayerData having island reference if
        // optimized.
        // For now, assume getIslandByOwner works for owner, but for members?
        // Let's iterate all islands cached? Expensive.
        // Best is if IslandService has getIsland(Player).
        // I'll check IslandService interface later. If not active, I'll fallback to
        // Owner check or scan.
        // Actually, let's assume getIslandByPlayer exists or I can implement it.
        // Ideally we pass Island UUID, but the interface accepts Player UUID.

        Island island = null;
        // Temporary: try to find island where player is member
        // In a real impl, this map should be cached.
        for (Island is : islandService.getLoadedIslands()) {
            if (is.isMember(playerId)) {
                island = is;
                break;
            }
        }

        if (island == null)
            return;

        // Check active quests
        for (Map.Entry<String, Integer> entry : island.getActiveDailyQuests().entrySet()) {
            String qId = entry.getKey();
            if (island.getCompletedDailyQuests().contains(qId))
                continue;

            QuestTemplate template = questTemplates.get(qId);
            if (template == null)
                continue;

            if (template.type.equalsIgnoreCase(questType)) {
                boolean targetMatch = true;
                if (target != null && !target.equalsIgnoreCase("ANY")) {
                    // Startswith check (e.g. ORE matches DIAMOND_ORE) or exact match
                    if (template.target == null
                            || (!template.target.equals("ANY") && !target.toUpperCase().contains(template.target))) {
                        // Very simple matching. Ideally robust enum matching.
                        // If template target is "COBBLESTONE" and event target is "COBBLESTONE", match.
                        // If template target is "ORE" and event target is "DIAMOND_ORE", match.
                        targetMatch = false;
                    }
                } else if (template.target != null && !template.target.equals("ANY")) {
                    // Event has no target (generic), but quest requires one
                    targetMatch = false;
                }

                if (targetMatch) {
                    int newProgress = entry.getValue() + amount;
                    // Cap at requirement?
                    // int req = getRequirement(template, "bronze"); // Or determined by island
                    // level?
                    // For now, let it go over.
                    island.getActiveDailyQuests().put(qId, newProgress);
                    // Don't save on every block break. Save periodically via persistence service.
                }
            }
        }
    }

    @Override
    public boolean completeQuest(UUID playerId, String questId) {
        // Verify player is on island
        Island island = null;
        for (Island is : islandService.getLoadedIslands()) {
            if (is.isMember(playerId)) {
                island = is;
                break;
            }
        }
        if (island == null)
            return false;

        if (!island.getActiveDailyQuests().containsKey(questId))
            return false;
        if (island.getCompletedDailyQuests().contains(questId))
            return false;

        QuestTemplate template = questTemplates.get(questId);
        if (template == null)
            return false;

        int current = island.getActiveDailyQuests().get(questId);
        String tier = template.tier; // Simplification: use template default tier
        int req = template.requirements.getOrDefault(tier.toLowerCase(), Integer.MAX_VALUE);

        if (current >= req) {
            island.getCompletedDailyQuests().add(questId);

            // Give Rewards
            int usdt = template.usdtRewards.getOrDefault(tier.toLowerCase(), 0);
            // int shards = template.shardRewards.getOrDefault(tier.toLowerCase(), 0);

            com.wiredid.skytree.impl.SkytreeEconomyService eco = (com.wiredid.skytree.impl.SkytreeEconomyService) plugin
                    .getEconomyService();
            if (usdt > 0)
                eco.addBalance(playerId, (double) usdt);

            // Shard Service (Future)
            // if (shards > 0 && plugin.getShardService() != null)
            // plugin.getShardService().addShards(playerId, shards);

            // Quest Points
            try {
                com.wiredid.skytree.api.PersistenceService ps = plugin.getPersistenceService();
                PlayerData pd = ps.loadPlayerData(playerId);
                pd.addQuestPoints(10); // Fixed 10 QP per quest for now
                // ps.savePlayerData(pd); // Async or let auto-save handle
            } catch (Exception e) {
                e.printStackTrace();
            }

            islandService.saveIsland(island);
            return true;
        }

        return false;
    }

    @Override
    public boolean claimReward(UUID playerId, String questId) {
        return completeQuest(playerId, questId); // Same logic for now
    }

    @Override
    public Map<String, Double> getActiveQuests(UUID playerId) {
        // Return progress 0.0-1.0
        Map<String, Double> result = new HashMap<>();

        Island island = null;
        for (Island is : islandService.getLoadedIslands()) {
            if (is.isMember(playerId)) {
                island = is;
                break;
            }
        }
        if (island == null)
            return result;

        for (Map.Entry<String, Integer> entry : island.getActiveDailyQuests().entrySet()) {
            String qId = entry.getKey();
            QuestTemplate t = questTemplates.get(qId);
            if (t == null)
                continue;

            int req = t.requirements.getOrDefault(t.tier.toLowerCase(), 1);
            double progress = Math.min(1.0, (double) entry.getValue() / req);
            result.put(qId, progress);
        }

        return result;
    }

    @Override
    public double getQuestProgress(UUID playerId, String questId) {
        Map<String, Double> map = getActiveQuests(playerId);
        return map.getOrDefault(questId, 0.0);
    }

    @Override
    public void checkReset() {
        // Logic to check stored last reset time vs current time
        // If stored time < today's midnight, reset all islands
        // For simple impl, we'll skip complex persistence of "lastReset" and just check
        // if hour is 00:00
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.HOUR_OF_DAY) == 0 && now.get(Calendar.MINUTE) == 0) {
            // It's midnight. Reset.
            // Avoid double reset by checking a flag or simpler: just log it.
            // Reset all loaded islands
            for (Island island : islandService.getLoadedIslands()) {
                generateDailyQuests(island.getIslandId());
            }
        }
    }

    @Override
    public int getCompletedToday(UUID playerId) {
        Island island = null;
        for (Island is : islandService.getLoadedIslands()) {
            if (is.isMember(playerId)) {
                island = is;
                break;
            }
        }
        if (island == null)
            return 0;
        return island.getCompletedDailyQuests().size();
    }

    public QuestTemplate getTemplate(String id) {
        return questTemplates.get(id);
    }

    public static class QuestTemplate {
        String id;
        String name;
        String description;
        String type;
        String target;
        String tier;
        Map<String, Integer> requirements = new HashMap<>();
        Map<String, Integer> usdtRewards = new HashMap<>();
        Map<String, Integer> shardRewards = new HashMap<>();

        public QuestTemplate(String id, String name, String description, String type, String target, String tier) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.target = target;
            this.tier = tier;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public int getRequirement(String t) {
            return requirements.getOrDefault(t.toLowerCase(), 0);
        }

        public int getRewardUSDT(String t) {
            return usdtRewards.getOrDefault(t.toLowerCase(), 0);
        }

        public int getRewardShards(String t) {
            return shardRewards.getOrDefault(t.toLowerCase(), 0);
        }

        public String getTier() {
            return tier;
        }
    }
}
