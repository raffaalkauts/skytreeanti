package com.wiredid.skytree.api;

import java.util.Map;
import java.util.UUID;

/**
 * Service for managing island-specific daily quests
 */
public interface IslandQuestService {

    /**
     * Generate daily quests for an island
     * 
     * @param islandId Island UUID
     */
    void generateDailyQuests(UUID islandId);

    /**
     * Track quest progress
     * 
     * @param playerId  Player UUID
     * @param questType Type of quest (BLOCK_BREAK, MOB_KILL, etc.)
     * @param amount    Amount to increment
     */
    void trackProgress(UUID playerId, String questType, int amount);

    /**
     * Track quest progress with specific target
     * 
     * @param playerId  Player UUID
     * @param questType Quest type
     * @param target    Specific target (material, mob type, etc.)
     * @param amount    Amount
     */
    void trackProgress(UUID playerId, String questType, String target, int amount);

    /**
     * Complete a quest
     * 
     * @param playerId Player UUID
     * @param questId  Quest ID
     * @return true if completed successfully
     */
    boolean completeQuest(UUID playerId, String questId);

    /**
     * Claim quest rewards
     * 
     * @param playerId Player UUID
     * @param questId  Quest ID
     * @return true if rewards claimed
     */
    boolean claimReward(UUID playerId, String questId);

    /**
     * Get active quests for a player's island
     * 
     * @param playerId Player UUID
     * @return Map of quest ID to progress (0.0-1.0)
     */
    Map<String, Double> getActiveQuests(UUID playerId);

    /**
     * Get quest progress
     * 
     * @param playerId Player UUID
     * @param questId  Quest ID
     * @return Progress value (0.0-1.0)
     */
    double getQuestProgress(UUID playerId, String questId);

    /**
     * Check if quests need to be reset (midnight check)
     */
    void checkReset();

    /**
     * Get completed quests count for today
     * 
     * @param playerId Player UUID
     * @return Number of quests completed today
     */
    int getCompletedToday(UUID playerId);
}
