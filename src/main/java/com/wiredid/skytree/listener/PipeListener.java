package com.wiredid.skytree.listener;

import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.api.PipeService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class PipeListener implements Listener {

    private final PipeService pipeService;
    private final ItemRegistry itemRegistry;

    public PipeListener(PipeService pipeService, ItemRegistry itemRegistry) {
        this.pipeService = pipeService;
        this.itemRegistry = itemRegistry;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String itemId = itemRegistry.getItemId(item);

        if (itemId != null && itemId.equals("item_pipe")) {
            pipeService.registerPipe(event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (pipeService.isPipe(event.getBlock().getLocation())) {
            pipeService.unregisterPipe(event.getBlock().getLocation());

            // Drop pipe item
            event.setDropItems(false);
            ItemStack pipeDrop = itemRegistry.getItem("item_pipe");
            if (pipeDrop != null) {
                event.getBlock().getWorld().dropItemNaturally(
                        event.getBlock().getLocation(), pipeDrop);
            }
        }
    }
}
