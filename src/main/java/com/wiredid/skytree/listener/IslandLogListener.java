package com.wiredid.skytree.listener;

import com.wiredid.skytree.api.ActionLogger;
import com.wiredid.skytree.api.IslandService;
import com.wiredid.skytree.model.ActionLog;
import com.wiredid.skytree.model.Island;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Optional;

/**
 * Listener for recording island actions
 */
public class IslandLogListener implements Listener {

    private final IslandService islandService;
    private final ActionLogger logger;

    public IslandLogListener(IslandService islandService, ActionLogger logger) {
        this.islandService = islandService;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Optional<Island> island = islandService.getIslandAtLocation(event.getBlock().getLocation());
        if (island.isEmpty())
            return;

        ActionLog log = new ActionLog(
                event.getPlayer().getUniqueId(),
                island.get().getId(),
                ActionLog.Action.PLACE,
                event.getBlock().getType(),
                event.getBlock().getLocation(),
                event.getBlock().getBlockData().getAsString());
        logger.log(log);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Optional<Island> island = islandService.getIslandAtLocation(event.getBlock().getLocation());
        if (island.isEmpty())
            return;

        ActionLog log = new ActionLog(
                event.getPlayer().getUniqueId(),
                island.get().getId(),
                ActionLog.Action.BREAK,
                event.getBlock().getType(),
                event.getBlock().getLocation(),
                event.getBlock().getBlockData().getAsString());
        logger.log(log);
    }
}
