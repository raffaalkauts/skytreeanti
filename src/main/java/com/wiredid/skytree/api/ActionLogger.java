package com.wiredid.skytree.api;

import com.wiredid.skytree.model.ActionLog;
import java.util.List;
import java.util.UUID;

/**
 * Service for logging and retrieving island actions
 */
public interface ActionLogger {

    /**
     * Log a new action
     */
    void log(ActionLog log);

    /**
     * Get logs for a specific island
     */
    List<ActionLog> getLogs(UUID islandId);

    /**
     * Get recent logs for a specific player on an island
     */
    List<ActionLog> getRecentLogs(UUID islandId, int limit);

    /**
     * Rollback the last X actions on an island
     */
    void rollback(UUID islandId, int count);

    /**
     * Rollback actions by a specific player on an island
     */
    void rollbackPlayer(UUID islandId, UUID playerId, long durationMs);

    /**
     * Clear old logs
     */
    void clearOldLogs(long maxAgeMs);
}
