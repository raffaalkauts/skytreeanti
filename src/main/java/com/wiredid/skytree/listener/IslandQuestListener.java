package com.wiredid.skytree.listener;

import com.wiredid.skytree.api.IslandQuestService;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class IslandQuestListener implements Listener {

    private final IslandQuestService questService;

    public IslandQuestListener(IslandQuestService questService) {
        this.questService = questService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // General Break tracking
        questService.trackProgress(player.getUniqueId(), "BLOCK_BREAK", blockType.name(), 1);
        questService.trackProgress(player.getUniqueId(), "BLOCK_BREAK", "ANY", 1);

        // Mining specific
        if (blockType == Material.COBBLESTONE || blockType == Material.STONE) {
            questService.trackProgress(player.getUniqueId(), "BLOCK_BREAK", "COBBLESTONE", 1);
        }
        if (blockType.name().endsWith("_ORE") || blockType == Material.ANCIENT_DEBRIS) {
            questService.trackProgress(player.getUniqueId(), "BLOCK_BREAK", "ORE", 1);
        }

        // Farming crops
        if (event.getBlock().getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) event.getBlock().getBlockData();
            if (ageable.getAge() == ageable.getMaximumAge()) {
                questService.trackProgress(player.getUniqueId(), "BLOCK_BREAK", "CROP", 1);
                questService.trackProgress(player.getUniqueId(), "BLOCK_BREAK", blockType.name(), 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        questService.trackProgress(player.getUniqueId(), "BLOCK_PLACE", blockType.name(), 1);
        questService.trackProgress(player.getUniqueId(), "BLOCK_PLACE", "ANY", 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null)
            return;

        String entityType = event.getEntityType().name();
        questService.trackProgress(killer.getUniqueId(), "MOB_KILL", entityType, 1);
        questService.trackProgress(killer.getUniqueId(), "MOB_KILL", "ANY", 1);

        if (event.getEntity() instanceof org.bukkit.entity.Monster) {
            questService.trackProgress(killer.getUniqueId(), "MOB_KILL", "MONSTER", 1);
        }
        if (event.getEntity() instanceof org.bukkit.entity.Animals) {
            questService.trackProgress(killer.getUniqueId(), "MOB_KILL", "ANIMAL", 1);
        }
    }
}
