package com.wiredid.skytree.api;

import org.bukkit.entity.Player;
import java.util.List;
import java.util.UUID;

/**
 * Interface for administrative tools and logging
 */
public interface AdminService {

    /**
     * Log an administrative or critical action
     * 
     * @param admin    The player performing the action (null if console/system)
     * @param category The category (e.g., ECONOMY, GACHA, ITEMS)
     * @param action   Description of the action
     */
    void logAction(UUID admin, String category, String action);

    /**
     * Get recent logs
     * 
     * @return List of formatted log strings
     */
    List<String> getRecentLogs(int limit);

    /**
     * Get logs for a specific player
     */
    List<String> getPlayerLogs(UUID target, int limit);

    /**
     * Toggle staff mode for a player
     */
    void toggleStaffMode(Player player);

    /**
     * Check if a player is in staff mode
     */
    boolean isInStaffMode(Player player);

    /**
     * Open another player's inventory
     */
    void openInventory(Player admin, Player target);

    /**
     * Open another player's enderchest
     */
    void openEnderChest(Player admin, Player target);
}
