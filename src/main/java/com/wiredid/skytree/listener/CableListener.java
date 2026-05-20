package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.system.EnergySystem;
import org.bukkit.Location;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class CableListener implements Listener {

    private final SkytreePlugin plugin;
    private final EnergySystem energySystem;
    private final ItemRegistry itemRegistry;
    private final int defaultCapacity;
    private final int defaultTransfer;

    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    public CableListener(SkytreePlugin plugin, EnergySystem energySystem, ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.energySystem = energySystem;
        this.itemRegistry = itemRegistry;
        this.defaultCapacity = plugin.getConfig().getInt("machines.cable_capacity", 100000);
        this.defaultTransfer = plugin.getConfig().getInt("machines.cable_transfer", 1000);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String itemId = itemRegistry.getItemId(item);

        if (itemId == null || !itemId.startsWith("cable_"))
            return;

        Block block = event.getBlockPlaced();
        Location loc = block.getLocation();

        // Determine cable stats and type
        int capacity = defaultCapacity / 100;
        int transfer = defaultTransfer / 100;
        String cableType = "cable_basic";

        if (itemId.contains("advanced")) {
            capacity = defaultCapacity / 10;
            transfer = defaultTransfer / 10;
            cableType = "cable_advanced";
        } else if (itemId.contains("elite")) {
            capacity = defaultCapacity;
            transfer = defaultTransfer;
            cableType = "cable_elite";
        }

        // Register cable
        energySystem.registerEnergyBlock(loc, capacity, transfer);
        plugin.getLogger().info("Registered cable at " + loc);

        // Store cable type in block PDC for proper drops
        if (block.getState() instanceof org.bukkit.block.TileState tileState) {
            tileState.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "cable_type"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    cableType);
            tileState.update();
        }

        // Connect to neighbors
        for (BlockFace face : FACES) {
            Block relative = block.getRelative(face);
            Location relLoc = relative.getLocation();

            if (energySystem.hasEnergy(relLoc)) {
                energySystem.connectCable(loc, relLoc);
                // plugin.getLogger().info("Connected cable to " + relLoc);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();

        if (energySystem.hasEnergy(loc)) {
            energySystem.unregisterEnergyBlock(loc);

            // Drop cable item with correct type
            event.setDropItems(false);
            String cableType = "cable_basic"; // Default fallback

            if (block.getState() instanceof org.bukkit.block.TileState tileState) {
                cableType = tileState.getPersistentDataContainer().getOrDefault(
                        new org.bukkit.NamespacedKey(plugin, "cable_type"),
                        org.bukkit.persistence.PersistentDataType.STRING,
                        "cable_basic");
            }

            ItemStack cableDrop = itemRegistry.getItem(cableType);
            if (cableDrop != null) {
                block.getWorld().dropItemNaturally(loc, cableDrop);
            }
        }
    }
}
