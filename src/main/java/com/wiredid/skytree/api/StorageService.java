package com.wiredid.skytree.api;

import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Service for Advanced Storage
 */
public class StorageService {

    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    /**
     * Get all inventories connected to a controller
     */
    public List<Inventory> getConnectedInventories(Location controllerLoc) {
        List<Inventory> inventories = new ArrayList<>();
        Set<Location> visited = new HashSet<>();
        Queue<Location> queue = new LinkedList<>();

        queue.add(controllerLoc);
        visited.add(controllerLoc);

        while (!queue.isEmpty()) {
            Location current = queue.poll();
            Block block = current.getBlock();

            // Check neighbors
            for (BlockFace face : FACES) {
                Block rel = block.getRelative(face);
                Location relLoc = rel.getLocation();

                if (visited.contains(relLoc))
                    continue;

                if (rel.getState() instanceof Container) {
                    inventories.add(((Container) rel.getState()).getInventory());
                    visited.add(relLoc);
                    queue.add(relLoc); // Continue search through containers
                } else if (rel.getType().name().contains("CABLE") || rel.getType() == Material.END_ROD) {
                    // Optional: Allow cables/pipes to extend network
                    // For now, only touching containers
                }
            }
        }

        return inventories;
    }

    /**
     * Get all items in the network
     */
    public List<ItemStack> getAllItems(Location controllerLoc) {
        List<Inventory> inventories = getConnectedInventories(controllerLoc);
        Map<String, ItemStack> combined = new HashMap<>();

        for (Inventory inv : inventories) {
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    String key = item.getType().name() + ":"
                            + (item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                                    ? ComponentUtil.toLegacy(item.getItemMeta().displayName()) : "");
                    if (combined.containsKey(key)) {
                        combined.get(key).setAmount(combined.get(key).getAmount() + item.getAmount());
                    } else {
                        combined.put(key, item.clone());
                    }
                }
            }
        }

        return new ArrayList<>(combined.values());
    }

    /**
     * Insert item into network
     */
    public ItemStack insertItem(Location controllerLoc, ItemStack item) {
        List<Inventory> inventories = getConnectedInventories(controllerLoc);
        ItemStack remaining = item.clone();

        for (Inventory inv : inventories) {
            HashMap<Integer, ItemStack> left = inv.addItem(remaining);
            if (left.isEmpty()) {
                return null; // All added
            }
            remaining = left.get(0);
        }

        return remaining;
    }

    /**
     * Extract item from network
     */
    public ItemStack extractItem(Location controllerLoc, ItemStack template, int amount) {
        List<Inventory> inventories = getConnectedInventories(controllerLoc);
        int needed = amount;
        ItemStack result = template.clone();
        result.setAmount(0);

        for (Inventory inv : inventories) {
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.isSimilar(template)) {
                    int take = Math.min(needed, item.getAmount());
                    item.setAmount(item.getAmount() - take);
                    inv.setItem(i, item.getAmount() > 0 ? item : null);

                    result.setAmount(result.getAmount() + take);
                    needed -= take;

                    if (needed <= 0)
                        return result;
                }
            }
        }

        return result.getAmount() > 0 ? result : null;
    }
}

