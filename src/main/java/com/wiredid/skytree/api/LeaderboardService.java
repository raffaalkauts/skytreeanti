package com.wiredid.skytree.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LeaderboardService {

    /**
     * Gets the top players by money balance.
     */
    List<Map.Entry<UUID, Double>> getTopMoney(int limit);

    /**
     * Gets the top players by shard balance.
     */
    List<Map.Entry<UUID, Integer>> getTopShards(int limit);

    /**
     * Gets the top players by playtime.
     */
    List<Map.Entry<UUID, Long>> getTopPlaytime(int limit);

    /**
     * Gets the top players by bounty amount.
     */
    List<Map.Entry<UUID, Double>> getTopBounties(int limit);

    /**
     * Gets the top islands by level.
     * Returns Map.Entry<UUID, Integer> where UUID is Owner UUID and Integer is
     * Level.
     */
    List<Map.Entry<UUID, Integer>> getTopIslands(int limit);

    /**
     * Refreshes the internal caches for all leaderboards.
     */
    void refreshCaches();
}
