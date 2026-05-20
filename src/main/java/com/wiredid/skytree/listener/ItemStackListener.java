package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;

/**
 * Handles merging of dropped items into "Hyper-Stacks" to reduce entity lag.
 */
public class ItemStackListener implements Listener {

    private final SkytreePlugin plugin;
    private final NamespacedKey itemAmountKey;

    public ItemStackListener(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.itemAmountKey = new NamespacedKey(plugin, "item_amount");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        ItemStack stack = item.getItemStack();
        if (stack.getType() == Material.AIR)
            return;

        // Search for nearby same items to merge
        double range = 4.0;
        for (org.bukkit.entity.Entity entity : item.getNearbyEntities(range, range, range)) {
            if (entity instanceof Item nearby && !nearby.isDead() && nearby.getEntityId() != item.getEntityId()) {
                ItemStack nearbyStack = nearby.getItemStack();
                if (nearbyStack.isSimilar(stack)) {
                    // Merge!
                    long currentAmount = getAmount(nearby);
                    long newAmount = currentAmount + stack.getAmount();

                    setAmount(nearby, newAmount);
                    updateDisplayName(nearby);

                    item.remove();
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Initialize new item if not merged
        long trueAmount = stack.getAmount();
        if (stack.hasItemMeta()) {
            Long metaAmount = stack.getItemMeta().getPersistentDataContainer().get(itemAmountKey,
                    PersistentDataType.LONG);
            if (metaAmount != null) {
                trueAmount = metaAmount;
            }
        }

        setAmount(item, trueAmount);
        if (trueAmount > 1) {
            updateDisplayName(item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player player))
            return;

        Item itemEntity = event.getItem();
        long totalAmount = getAmount(itemEntity);
        if (totalAmount <= itemEntity.getItemStack().getAmount())
            return;

        event.setCancelled(true);

        ItemStack template = itemEntity.getItemStack().clone();
        long remaining = totalAmount;

        // Try to add to inventory
        while (remaining > 0) {
            int toAdd = (int) Math.min(remaining, template.getMaxStackSize());
            template.setAmount(toAdd);

            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(template);
            if (!leftovers.isEmpty()) {
                // Inventory full, stop picking up
                int failed = leftovers.values().iterator().next().getAmount();
                remaining = remaining - toAdd + failed;
                break;
            }
            remaining -= toAdd;
        }

        if (remaining <= 0) {
            itemEntity.remove();
        } else {
            setAmount(itemEntity, remaining);
            updateDisplayName(itemEntity);
        }

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent event) {
        Item itemEntity = event.getItem();
        long totalAmount = getAmount(itemEntity);

        // Hopper usually takes 1 or stack size? Vanilla takes 1 stack at a time if
        // possible?
        // Actually InventoryPickupItemEvent is for a single stack.
        // We let the event happen for the "base" item stack.
        // Then we subtract it from our hyper-stack and respawn the next stack if
        // needed.

        if (totalAmount <= itemEntity.getItemStack().getAmount())
            return;

        // The entity is moving into the inventory
        // We need to keep the entity alive if there is leftovers
        long leftover = totalAmount - itemEntity.getItemStack().getAmount();

        if (leftover > 0) {
            // We can't cancel the event easily without stopping the pickup.
            // Instead, we let it happen and spawn a replacement entity for leftovers next
            // tick.
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (leftover > 0) {
                    ItemStack nextStack = itemEntity.getItemStack().clone();
                    nextStack.setAmount((int) Math.min(leftover, nextStack.getMaxStackSize()));

                    Item newItem = itemEntity.getWorld().dropItem(itemEntity.getLocation(), nextStack);
                    setAmount(newItem, leftover);
                    updateDisplayName(newItem);
                }
            });
        }
    }

    private long getAmount(Item item) {
        Long val = item.getPersistentDataContainer().get(itemAmountKey, PersistentDataType.LONG);
        return val != null ? val : item.getItemStack().getAmount();
    }

    private void setAmount(Item item, long amount) {
        item.getPersistentDataContainer().set(itemAmountKey, PersistentDataType.LONG, amount);
        // Also update the underlying ItemStack to reflect at least 1 stack for vanilla
        // mechanics
        ItemStack stack = item.getItemStack();
        stack.setAmount((int) Math.min(amount, stack.getMaxStackSize()));
        item.setItemStack(stack);
    }

    private void updateDisplayName(Item item) {
        long amount = getAmount(item);
        if (amount > 1) {
            String name = "§e" + String.format("%,d", amount) + "x §f" + getFriendlyName(item.getItemStack().getType());
            item.customName(ComponentUtil.parse(name));
            item.setCustomNameVisible(true);
        } else {
            item.setCustomNameVisible(false);
        }
    }

    private String getFriendlyName(Material mat) {
        String name = mat.name().replace("_", " ").toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
