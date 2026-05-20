package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.LeaderboardService;
import com.wiredid.skytree.model.Bounty;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.stream.Collectors;

public class SkytreeLeaderboardService implements LeaderboardService {

    private final SkytreePlugin plugin;

    // Caches
    private List<Map.Entry<UUID, Double>> topMoney = new ArrayList<>();
    private List<Map.Entry<UUID, Integer>> topShards = new ArrayList<>();
    private List<Map.Entry<UUID, Long>> topPlaytime = new ArrayList<>();

    private List<Map.Entry<UUID, Double>> topBounties = new ArrayList<>();
    private List<Map.Entry<UUID, Integer>> topIslands = new ArrayList<>();

    public SkytreeLeaderboardService(SkytreePlugin plugin) {
        this.plugin = plugin;

        // Initial refresh
        refreshCaches();

        // Schedule periodic refresh (every 10 minutes)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refreshCaches, 20 * 60 * 10, 20 * 60 * 10);
    }

    @Override
    public List<Map.Entry<UUID, Double>> getTopMoney(int limit) {
        return topMoney.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<Map.Entry<UUID, Integer>> getTopShards(int limit) {
        return topShards.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<Map.Entry<UUID, Long>> getTopPlaytime(int limit) {
        return topPlaytime.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<Map.Entry<UUID, Double>> getTopBounties(int limit) {
        return topBounties.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<Map.Entry<UUID, Integer>> getTopIslands(int limit) {
        return topIslands.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public void refreshCaches() {
        // Refresh Money
        Map<UUID, Double> moneyMap = plugin.getPersistenceService().getAllBalances();
        topMoney = moneyMap.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toList());

        // Refresh Shards
        Map<UUID, Integer> shardMap = plugin.getShardService().getAllShards();
        topShards = shardMap.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toList());

        // Refresh Playtime
        Map<UUID, Long> playtimeMap = plugin.getPlaytimeService().getAllPlaytimeMillis();
        topPlaytime = playtimeMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toList());

        // Refresh Bounties
        // For bounties, we can aggregate or use the service's own top list
        // Let's aggregate if we want a different limit than what the GUI shows,
        // but getTopBounties is already available.
        // Actually, let's just use the service's getAllBounties and sort here to be
        // consistent.
        List<Bounty> allBounties = plugin.getBountyService().getAllBounties();
        topBounties = allBounties.stream()
                .collect(Collectors.groupingBy(Bounty::getTarget, Collectors.summingDouble(Bounty::getAmount)))
                .entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))

                .collect(Collectors.toList());

        // Refresh Islands
        Map<UUID, Integer> islandMap = plugin.getIslandService().getAllIslandLevels();
        topIslands = islandMap.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toList());

        plugin.getLogger().info("Leaderboard caches refreshed.");
    }
}
