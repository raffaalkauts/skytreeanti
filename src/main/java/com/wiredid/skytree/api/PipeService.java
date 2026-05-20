package com.wiredid.skytree.api;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Service for handling item pipes
 */
public class PipeService {

    private final SkytreePlugin plugin;
    private final Set<Location> pipes = new HashSet<>();
    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    public PipeService(SkytreePlugin plugin) {
        this.plugin = plugin;
        startPipeTask();
    }

    public void registerPipe(Location loc) {
        pipes.add(loc);
    }

    public void unregisterPipe(Location loc) {
        pipes.remove(loc);
    }

    public boolean isPipe(Location loc) {
        return pipes.contains(loc);
    }

    private void startPipeTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (pipes.isEmpty())
                return;

            for (Location pipeLoc : new HashSet<>(pipes)) { // Copy to avoid concurrent modification
                processPipe(pipeLoc);
            }
        }, 20L, 20L); // Run every second
    }

    private void processPipe(Location pipeLoc) {
        Block pipeBlock = pipeLoc.getBlock();
        if (pipeBlock.getType() != Material.END_ROD) {
            unregisterPipe(pipeLoc);
            return;
        }

        // Simple logic: Pull from one neighbor, push to another
        // For simplicity: Pull from UP/NORTH/EAST, Push to DOWN/SOUTH/WEST
        // Or better: Pull from any container, push to any container that isn't the
        // source

        Inventory source = null;
        Inventory sink = null;

        for (BlockFace face : FACES) {
            Block rel = pipeBlock.getRelative(face);
            if (rel.getState() instanceof Container) {
                Container container = (Container) rel.getState();
                if (source == null) {
                    // Check if it has items
                    if (!isEmpty(container.getInventory())) {
                        source = container.getInventory();
                    }
                } else if (sink == null) {
                    // Check if it has space
                    if (hasSpace(container.getInventory())) {
                        sink = container.getInventory();
                    }
                }
            }
        }

        if (source != null && sink != null && source != sink) {
            moveItem(source, sink);
        }
    }

    private boolean isEmpty(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR)
                return false;
        }
        return true;
    }

    private boolean hasSpace(Inventory inv) {
        return inv.firstEmpty() != -1;
    }

    private void moveItem(Inventory source, Inventory sink) {
        for (int i = 0; i < source.getSize(); i++) {
            ItemStack item = source.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                ItemStack one = item.clone();
                one.setAmount(1);

                // Try adding to sink
                if (sink.addItem(one).isEmpty()) {
                    // Success, remove from source
                    item.setAmount(item.getAmount() - 1);
                    source.setItem(i, item.getAmount() > 0 ? item : null);
                    return; // Move one item per tick per pipe
                }
            }
        }
    }
}

