package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.IslandService;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.model.IslandPermission;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.Optional;

/**
 * Listener for island protection
 */
public class IslandProtectionListener implements Listener {

    private final SkytreePlugin plugin;
    private final IslandService islandService;

    public IslandProtectionListener(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.islandService = plugin.getIslandService();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Optional<Island> island = islandService.getIslandAtLocation(event.getBlock().getLocation());

        if (island.isEmpty())
            return;

        if (!hasPermission(player, IslandPermission.BREAK, island.get())) {
            event.setCancelled(true);
            player.sendMessage("§c§l[Skytree] §cYou can't break blocks here!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Optional<Island> island = islandService.getIslandAtLocation(event.getBlock().getLocation());

        if (island.isEmpty())
            return;

        if (!hasPermission(player, IslandPermission.BUILD, island.get())) {
            event.setCancelled(true);
            player.sendMessage("§c§l[Skytree] §cYou can't place blocks here!");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR
                || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR) {
            // Check protection even for air clicks if they result in item use (buckets,
            // spawn eggs)
            ItemStack item = event.getItem();
            if (item != null
                    && (item.getType().name().contains("BUCKET") || item.getType().name().contains("SPAWN_EGG"))) {
                Optional<Island> island = islandService.getIslandAtLocation(player.getLocation());
                if (island.isPresent() && !hasPermission(player, IslandPermission.USE_ITEMS, island.get())) {
                    event.setCancelled(true);
                    player.sendMessage("§c§l[Skytree] §cYou can't use that here!");
                    return;
                }
            }
        }

        if (event.getClickedBlock() == null && event.getAction() != org.bukkit.event.block.Action.PHYSICAL)
            return;
        org.bukkit.Location loc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation()
                : player.getLocation();
        Optional<Island> island = islandService.getIslandAtLocation(loc);

        if (island.isEmpty())
            return;

        IslandPermission perm = IslandPermission.USE_ITEMS;

        if (event.getAction() == org.bukkit.event.block.Action.PHYSICAL) {
            perm = IslandPermission.BREAK; // Trampling
        } else if (event.getClickedBlock() != null) {
            org.bukkit.Material mat = event.getClickedBlock().getType();
            if (mat.name().contains("CHEST") || mat.name().contains("SHULKER") || mat.name().contains("BARREL")
                    || mat == org.bukkit.Material.FURNACE || mat == org.bukkit.Material.BLAST_FURNACE
                    || mat == org.bukkit.Material.SMOKER || mat == org.bukkit.Material.HOPPER) {
                perm = IslandPermission.CONTAINER;
            } else if (mat.name().contains("DOOR") || mat.name().contains("GATE")) {
                perm = IslandPermission.DOORS;
            } else if (mat == org.bukkit.Material.LEVER || mat.name().contains("BUTTON")
                    || mat.name().contains("PRESSURE_PLATE")) {
                perm = IslandPermission.REDSTONE;
            }
        }

        if (!hasPermission(player, perm, island.get())) {
            event.setCancelled(true);
            if (event.getAction() != org.bukkit.event.block.Action.PHYSICAL) {
                player.sendMessage("§c§l[Skytree] §cYou don't have permission to do that here!");
            }
        }
    }

    @EventHandler
    public void onInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        handleEntityInteraction(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler
    public void onInteractAtEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        handleEntityInteraction(event.getPlayer(), event.getRightClicked(), event);
    }

    private void handleEntityInteraction(Player player, org.bukkit.entity.Entity entity,
            org.bukkit.event.Cancellable event) {
        Optional<Island> island = islandService.getIslandAtLocation(entity.getLocation());
        if (island.isPresent()) {
            IslandPermission perm = IslandPermission.USE_ITEMS;

            // Protect Item Frames and Armor Stands with BREAK permission
            if (entity instanceof org.bukkit.entity.ItemFrame || entity instanceof org.bukkit.entity.ArmorStand) {
                perm = IslandPermission.BREAK;
            }

            if (!hasPermission(player, perm, island.get())) {
                event.setCancelled(true);
                player.sendMessage("§c§l[Skytree] §cYou can't interact with that here!");
            }
        }
    }

    @EventHandler
    public void onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Optional<Island> island = islandService.getIslandAtLocation(event.getBlock().getLocation());
        if (island.isPresent()
                && !hasPermission(player, IslandPermission.USE_ITEMS, island.get())) {
            event.setCancelled(true);
            player.sendMessage("§c§l[Skytree] §cYou can't use buckets here!");
        }
    }

    @EventHandler
    public void onBucketFill(org.bukkit.event.player.PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Optional<Island> island = islandService.getIslandAtLocation(event.getBlock().getLocation());
        if (island.isPresent()
                && !hasPermission(player, IslandPermission.USE_ITEMS, island.get())) {
            event.setCancelled(true);
            player.sendMessage("§c§l[Skytree] §cYou can't use buckets here!");
        }
    }

    @EventHandler
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;
        Player player = (Player) event.getDamager();

        Optional<Island> island = islandService.getIslandAtLocation(event.getEntity().getLocation());
        if (island.isPresent()
                && !hasPermission(player, IslandPermission.ANIMALS, island.get())) {

            // Allow PVP if it's a player (handled by onPvp, but we must not block here if
            // it's player)
            if (event.getEntity() instanceof Player) {
                return; // Let onPvp handle it
            }

            event.setCancelled(true);
            player.sendMessage("§c§l[Skytree] §cYou can't damage things here!");
        }
    }

    @EventHandler
    public void onHangingBreak(org.bukkit.event.hanging.HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player))
            return;
        Player player = (Player) event.getRemover();

        Optional<Island> island = islandService.getIslandAtLocation(event.getEntity().getLocation());
        if (island.isPresent()
                && !hasPermission(player, IslandPermission.BREAK, island.get())) {
            event.setCancelled(true);
            player.sendMessage("§c§l[Skytree] §cYou can't break that here!");
        }
    }

    // ==========================================
    // NEW HANDLERS FOR SETTINGS & EXPLOITS
    // ==========================================

    @EventHandler
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        Optional<Island> island = islandService.getIslandAtLocation(event.getLocation());
        if (island.isPresent()) {
            boolean allow = plugin.getIslandSettingsService().getSetting(island.get(), "tnt");
            if (!allow) {
                event.blockList().clear();
            }
        }
    }

    @EventHandler
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        Optional<Island> island = islandService.getIslandAtLocation(event.getBlock().getLocation());
        if (island.isPresent()) {
            if (!plugin.getIslandSettingsService().getSetting(island.get(), "tnt")) {
                event.blockList().clear();
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent event) {
        Optional<Island> island = islandService.getIslandAtLocation(event.getBlock().getLocation());
        if (island.isPresent()) {
            if (!plugin.getIslandSettingsService().getSetting(island.get(), "pistons")) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent event) {
        Optional<Island> island = islandService.getIslandAtLocation(event.getBlock().getLocation());
        if (island.isPresent()) {
            if (!plugin.getIslandSettingsService().getSetting(island.get(), "pistons")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFireSpread(org.bukkit.event.block.BlockSpreadEvent event) {
        if (event.getSource().getType() == org.bukkit.Material.FIRE) {
            Optional<Island> island = islandService.getIslandAtLocation(event.getBlock().getLocation());
            if (island.isPresent()) {
                if (!plugin.getIslandSettingsService().getSetting(island.get(), "fire_spread")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onLeafDecay(org.bukkit.event.block.LeavesDecayEvent event) {
        Optional<Island> island = islandService.getIslandAtLocation(event.getBlock().getLocation());
        if (island.isPresent()) {
            if (!plugin.getIslandSettingsService().getSetting(island.get(), "leaf_decay")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMobSpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {
        if (event.getSpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM)
            return;

        Optional<Island> island = islandService.getIslandAtLocation(event.getLocation());
        if (island.isPresent()) {
            if (event.getEntity() instanceof org.bukkit.entity.Monster) {
                if (!plugin.getIslandSettingsService().getSetting(island.get(), "mob_spawning")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // PVP Check injection into EntityDamageByEntity
    @EventHandler(priority = org.bukkit.event.EventPriority.LOW)
    public void onPvp(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Optional<Island> island = islandService.getIslandAtLocation(event.getEntity().getLocation());
            if (island.isPresent()) {
                if (!plugin.getIslandSettingsService().getSetting(island.get(), "pvp")) {
                    event.setCancelled(true);
                    ((Player) event.getDamager()).sendMessage("§c§l[Skytree] §cPvP is disabled on this island!");
                }
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        Optional<Island> island = islandService.getIslandAtLocation(event.getEntity().getLocation());
        if (island.isPresent()) {
            if (plugin.getIslandSettingsService().getSetting(island.get(), "drop_protection")) {
                if (!hasPermission(player, IslandPermission.CONTAINER, island.get())) { // Use CONTAINER perm for drops
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean hasPermission(Player player, IslandPermission permission, Island island) {
        if (island.getOwnerUUID().equals(player.getUniqueId()))
            return true;
        if (player.hasPermission("skytree.admin") || player.hasPermission("skytree.bypass"))
            return true;

        com.wiredid.skytree.model.TrustLevel trust = island.getTrustLevel(player.getUniqueId());
        IslandPermission[] perms = IslandPermission.getDefaultPerms(trust);
        for (IslandPermission p : perms) {
            if (p == permission)
                return true;
        }

        return false;
    }
}
