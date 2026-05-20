package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ActionLogger;
import com.wiredid.skytree.model.ActionLog;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of island action logging
 */
public class SkytreeActionLogger implements ActionLogger {

    private final SkytreePlugin plugin;
    private final Map<UUID, List<ActionLog>> islandLogs = new ConcurrentHashMap<>();
    private static final int MAX_LOGS_PER_ISLAND = 5000;

    public SkytreeActionLogger(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void log(ActionLog log) {
        islandLogs.computeIfAbsent(log.getIslandId(), k -> Collections.synchronizedList(new ArrayList<>())).add(log);

        // Enforce limit
        List<ActionLog> logs = islandLogs.get(log.getIslandId());
        if (logs.size() > MAX_LOGS_PER_ISLAND) {
            logs.remove(0);
        }
    }

    @Override
    public List<ActionLog> getLogs(UUID islandId) {
        return new ArrayList<>(islandLogs.getOrDefault(islandId, Collections.emptyList()));
    }

    @Override
    public List<ActionLog> getRecentLogs(UUID islandId, int limit) {
        List<ActionLog> logs = getLogs(islandId);
        if (logs.size() <= limit)
            return logs;
        return logs.subList(logs.size() - limit, logs.size());
    }

    @Override
    public void rollback(UUID islandId, int count) {
        List<ActionLog> logs = islandLogs.get(islandId);
        if (logs == null || logs.isEmpty())
            return;

        int actualRollback = Math.min(count, logs.size());
        for (int i = 0; i < actualRollback; i++) {
            ActionLog log = logs.remove(logs.size() - 1);
            applyRollback(log);
        }
    }

    @Override
    public void rollbackPlayer(UUID islandId, UUID playerId, long durationMs) {
        List<ActionLog> logs = islandLogs.get(islandId);
        if (logs == null || logs.isEmpty())
            return;

        long threshold = System.currentTimeMillis() - durationMs;
        ListIterator<ActionLog> it = logs.listIterator(logs.size());

        while (it.hasPrevious()) {
            ActionLog log = it.previous();
            if (log.getTimestamp() < threshold)
                break;
            if (log.getPlayerId().equals(playerId)) {
                applyRollback(log);
                it.remove();
            }
        }
    }

    private void applyRollback(ActionLog log) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Block block = log.getLocation().getBlock();
            if (log.getAction() == ActionLog.Action.PLACE) {
                // If they placed it, we break it (set to AIR)
                block.setType(Material.AIR);
            } else if (log.getAction() == ActionLog.Action.BREAK) {
                // If they broke it, we restore it
                block.setType(log.getBlockType());
                if (log.getBlockData() != null) {
                    try {
                        BlockData data = Bukkit.createBlockData(log.getBlockData());
                        block.setBlockData(data);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    @Override
    public void clearOldLogs(long maxAgeMs) {
        long threshold = System.currentTimeMillis() - maxAgeMs;
        islandLogs.values().forEach(logs -> {
            logs.removeIf(log -> log.getTimestamp() < threshold);
        });
    }
}
