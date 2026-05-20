package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.api.ItemTransportService;
import org.bukkit.Location;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemTransportListener implements Listener {

    private final SkytreePlugin plugin;
    private final ItemRegistry itemRegistry;
    private final ItemTransportService transportService;

    // Temporary storage for player selection
    private final Map<UUID, Location> sourceSelection = new HashMap<>();

    public ItemTransportListener(SkytreePlugin plugin, ItemRegistry itemRegistry,
            ItemTransportService transportService) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.transportService = transportService;
    }

    @EventHandler
    public void onLink(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !itemRegistry.isCustomItem(item))
            return;
        if (!"linker_tool".equals(itemRegistry.getItemId(item)))
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        // Ensure it's a container or machine
        if (!(block.getState() instanceof org.bukkit.inventory.InventoryHolder)) {
            // Also allow machines that might not have inventory holder yet (e.g. custom
            // machines)
            if (!plugin.getMachineProcessor().getActiveMachinesLocations().contains(block.getLocation())) {
                return;
            }
        }

        event.setCancelled(true);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (player.isSneaking()) {
                // Clear links from this block
                transportService.removeAllLinks(block.getLocation(), player.getUniqueId());
                player.sendMessage("§c[Linker] §fAll links removed from this block.");
                sourceSelection.remove(player.getUniqueId());
            } else {
                // First click: Source
                if (!sourceSelection.containsKey(player.getUniqueId())) {
                    sourceSelection.put(player.getUniqueId(), block.getLocation());
                    player.sendMessage("§b[Linker] §fSource set! §7Right-click another container to link.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                } else {
                    // Second click: Destination
                    Location source = sourceSelection.remove(player.getUniqueId());
                    if (transportService.createLink(source, block.getLocation(), player.getUniqueId())) {
                        player.sendMessage("§a[Linker] §fLink created! §7Items will move periodically.");
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 0.5f, 1.5f);
                        transportService.spawnLinkParticle(source, block.getLocation());
                    } else {
                        player.sendMessage("§c[Linker] §fFailed to create link. (Max links reached or already linked)");
                    }
                }
            }
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Left click to clear current selection
            if (sourceSelection.remove(player.getUniqueId()) != null) {
                player.sendMessage("§e[Linker] §fSelection cleared.");
            }
        }
    }
}
