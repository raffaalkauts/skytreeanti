package com.wiredid.skytree.listener;

import com.wiredid.skytree.api.IslandShopService;
import com.wiredid.skytree.model.IslandShop;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public class IslandShopListener implements Listener {

    private final IslandShopService islandShopService;
    private final com.wiredid.skytree.gui.IslandShopGUI islandShopGUI;

    public IslandShopListener(IslandShopService islandShopService,
            com.wiredid.skytree.gui.IslandShopGUI islandShopGUI) {
        this.islandShopService = islandShopService;
        this.islandShopGUI = islandShopGUI;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Block block = event.getClickedBlock();
        if (block == null || (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST
                && block.getType() != Material.BARREL)) {
            return;
        }

        Optional<IslandShop> shopOpt = islandShopService.getShop(block);
        if (shopOpt.isPresent()) {
            event.setCancelled(true);
            IslandShop shop = shopOpt.get();

            islandShopGUI.open(event.getPlayer(), shop);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Optional<IslandShop> shopOpt = islandShopService.getShop(block);
        if (shopOpt.isPresent()) {
            IslandShop shop = shopOpt.get();
            if (!event.getPlayer().getUniqueId().equals(shop.getOwnerUUID())
                    && !event.getPlayer().hasPermission("skytree.admin")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cYou cannot break this shop!");
                return;
            }
            islandShopService.removeShop(block);
            event.getPlayer().sendMessage("§cIsland Shop removed.");
        }
    }
}
