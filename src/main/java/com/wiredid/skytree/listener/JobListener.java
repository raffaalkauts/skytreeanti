package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.economy.JobService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.inventory.CraftItemEvent;

import java.util.Set;

public class JobListener implements Listener {

    private static final Set<Material> ORES = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    private static final Set<Material> LOGS = Set.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD,
            Material.JUNGLE_WOOD, Material.ACACIA_WOOD, Material.DARK_OAK_WOOD,
            Material.MANGROVE_WOOD, Material.CHERRY_WOOD,
            Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG,
            Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_JUNGLE_LOG,
            Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
            Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG
    );

    private static final Set<Material> CROPS = Set.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.NETHER_WART,
            Material.PUMPKIN, Material.MELON,
            Material.SUGAR_CANE, Material.CACTUS,
            Material.BAMBOO, Material.KELP,
            Material.COCOA, Material.SWEET_BERRY_BUSH,
            Material.PITCHER_CROP, Material.TORCHFLOWER_CROP
    );

    private static final Set<Material> STONE_VARIANTS = Set.of(
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE,
            Material.ANDESITE, Material.DIORITE, Material.GRANITE,
            Material.TUFF, Material.CALCITE, Material.DRIPSTONE_BLOCK,
            Material.NETHERRACK, Material.BASALT, Material.BLACKSTONE,
            Material.END_STONE, Material.SANDSTONE, Material.RED_SANDSTONE
    );

    private final SkytreePlugin plugin;
    private final JobService jobService;

    public JobListener(SkytreePlugin plugin, JobService jobService) {
        this.plugin = plugin;
        this.jobService = jobService;
    }

    private boolean hasJobsEnabled() {
        return plugin.getConfig().contains("jobs");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!hasJobsEnabled()) return;
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) return;
        Block block = event.getBlock();
        Material type = block.getType();

        if (ORES.contains(type) || STONE_VARIANTS.contains(type)) {
            double worth = jobService.getBlockValue(type);
            jobService.handleJobAction(player, "miner", worth);
        }

        if (LOGS.contains(type)) {
            double worth = jobService.getBlockValue(type);
            jobService.handleJobAction(player, "lumberjack", worth);
        }

        if (CROPS.contains(type)) {
            double worth = jobService.getBlockValue(type);
            jobService.handleJobAction(player, "farmer", worth);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!hasJobsEnabled()) return;
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) return;

        double worth = jobService.getBlockValue(event.getBlock().getType());
        jobService.handleJobAction(player, "builder", worth);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!hasJobsEnabled()) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (killer.hasMetadata("NPC")) return;

        EntityType type = event.getEntityType();
        double worth = jobService.getMobValue(type);
        jobService.handleJobAction(killer, "hunter", worth);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (!hasJobsEnabled()) return;
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) return;

        double worth = 5.0;
        if (event.getCaught() instanceof org.bukkit.entity.Item item) {
            worth = jobService.getBlockValue(item.getItemStack().getType());
        }
        jobService.handleJobAction(player, "fisher", worth);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!hasJobsEnabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.hasMetadata("NPC")) return;

        double worth = jobService.getBlockValue(event.getRecipe().getResult().getType());
        jobService.handleJobAction(player, "crafter", worth);
    }
}
