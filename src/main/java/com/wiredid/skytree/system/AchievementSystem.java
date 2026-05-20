package com.wiredid.skytree.system;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.PlayerData;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Achievement system for tracking player milestones
 */
public class AchievementSystem {

    private final SkytreePlugin plugin;

    public AchievementSystem(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public enum Achievement {
        // Achievement definitions...
        FIRST_ISLAND("First Island", "Create your first island", 100),
        ISLAND_LEVEL_10("Island Level 10", "Reach island level 10", 500),
        ISLAND_LEVEL_50("Island Level 50", "Reach island level 50", 2000),
        ISLAND_LEVEL_100("Island Level 100", "Reach island level 100", 5000),

        // Economy Achievements
        FIRST_TRADE("First Trade", "Make your first purchase", 50),
        RICH("Rich", "Have \u20AE 10,000", 1000),
        MILLIONAIRE("Millionaire", "Have \u20AE 1,000,000", 10000),

        // Item Achievements
        CRAFTER("Crafter", "Craft 100 items", 200),
        MASTER_CRAFTER("Master Crafter", "Craft 1000 items", 2000),

        // Team Achievements
        TEAM_PLAYER("Team Player", "Invite your first member", 300),
        BIG_TEAM("Big Team", "Have 5 team members", 1000),

        // Progression Achievements
        SIEVE_MASTER("Sieve Master", "Sieve 1000 blocks", 500),
        HAMMER_TIME("Hammer Time", "Use hammer 500 times", 300),
        MOB_FARMER("Mob Farmer", "Spawn 100 mobs with dolls", 800),

        // Special Achievements
        VISITOR("Visitor", "Visit 10 different islands", 400),
        EXPLORER("Explorer", "Unlock all biomes", 1500);

        private final String name;
        private final String description;
        private final int reward;

        Achievement(String name, String description, int reward) {
            this.name = name;
            this.description = description;
            this.reward = reward;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public int getReward() {
            return reward;
        }
    }

    public void unlockAchievement(Player player, Achievement achievement) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());

        if (data.getAchievements().add(achievement.name())) {
            plugin.getPersistenceService().savePlayerData(data);

            // New achievement unlocked!
            player.sendMessage("§6§l✨ ACHIEVEMENT UNLOCKED! ✨");
            player.sendMessage("§e§l" + achievement.getName());
            player.sendMessage("§7" + achievement.getDescription());
            player.sendMessage("§a+§e\u20AE " + achievement.getReward() + " §areward!");

            // Reward
            plugin.getEconomyService().addBalance(player.getUniqueId(), achievement.getReward());

            // Play sound
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    public boolean hasAchievement(UUID uuid, Achievement achievement) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(uuid);
        return data.getAchievements().contains(achievement.name());
    }

    public Set<String> getAchievements(UUID uuid) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(uuid);
        return new HashSet<>(data.getAchievements());
    }

    public int getAchievementCount(UUID uuid) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(uuid);
        return data.getAchievements().size();
    }

    public double getCompletionPercentage(UUID uuid) {
        return (double) getAchievementCount(uuid) / Achievement.values().length * 100;
    }
}
