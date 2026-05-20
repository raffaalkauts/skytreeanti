package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.WorthService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;

public class SmartStackListener implements Listener {

    private final SkytreePlugin plugin;
    private final WorthService worthService;

    public SmartStackListener(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.worthService = plugin.getWorthService();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled())
            return;
        if (!(event.getWhoClicked() instanceof Player))
            return;

        // Skip negative slots (outside window clicks)
        if (event.getRawSlot() < 0)
            return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // 1. DUPE PREVENTION: If clicking a RESULT slot (Crafting Output, Anvil result,
        // etc.)
        // We MUST let vanilla handle it so ingredients are consumed.
        if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.RESULT) {
            return;
        }

        // Handle Shift+Click (Move/Sort)
        if (event.isShiftClick()) {
            if (current == null || current.getType() == Material.AIR)
                return;

            Player player = (Player) event.getWhoClicked();
            org.bukkit.inventory.Inventory clickedInv = event.getClickedInventory();
            org.bukkit.inventory.Inventory top = event.getView().getTopInventory();
            org.bukkit.inventory.Inventory bottom = event.getView().getBottomInventory();

            // 1. Identify Target Inventory
            org.bukkit.inventory.Inventory target;
            if (clickedInv.equals(bottom)) {
                // Clicking in player inventory
                if (top.getType() == InventoryType.CRAFTING ||
                        top.getType() == InventoryType.PLAYER) {
                    target = bottom;
                } else {
                    target = top;
                }
            } else {
                target = bottom;
            }

            // 2. Special Handling for Survival Inventory (Hotbar <-> Main <-> Armor)
            if (target.equals(bottom)) {
                int slot = event.getSlot();
                int targetStart, targetEnd;

                if (slot < 9) { // Hotbar
                    if (isArmor(current)) {
                        if (moveItemToRange(current, bottom, 36, 40)) {
                            event.setCancelled(true);
                            player.updateInventory();
                            return;
                        }
                    }
                    targetStart = 9;
                    targetEnd = 36;
                } else if (slot < 36) { // Main Inventory
                    if (isArmor(current)) {
                        if (moveItemToRange(current, bottom, 36, 40)) {
                            event.setCancelled(true);
                            player.updateInventory();
                            return;
                        }
                    }
                    targetStart = 0;
                    targetEnd = 9;
                } else { // Armor/Offhand
                    targetStart = 0;
                    targetEnd = 36;
                }

                if (moveItemToRange(current, bottom, targetStart, targetEnd)) {
                    event.setCancelled(true);
                    player.updateInventory();
                }
                return;
            }

            // 3. General Move Logic (e.g. into Chest)
            event.setCancelled(true);
            int remaining = current.getAmount();
            ItemStack[] contents = target.getContents();

            for (int i = 0; i < contents.length; i++) {
                ItemStack targetItem = contents[i];
                if (targetItem == null || targetItem.getType() == Material.AIR
                        || targetItem.getAmount() >= targetItem.getMaxStackSize())
                    continue;

                if (worthService.isSimilarIgnoringWorth(current, targetItem)) {
                    int space = targetItem.getMaxStackSize() - targetItem.getAmount();
                    int toAdd = Math.min(space, remaining);
                    if (toAdd > 0) {
                        targetItem.setAmount(targetItem.getAmount() + toAdd);
                        // Scheduler will update lore in 0-2 seconds
                        target.setItem(i, targetItem);
                        remaining -= toAdd;
                    }
                    if (remaining <= 0)
                        break;
                }
            }

            if (remaining > 0) {
                ItemStack toAdd = current.clone();
                toAdd.setAmount(remaining);
                java.util.Map<Integer, ItemStack> leftovers = target.addItem(toAdd);
                if (leftovers.isEmpty()) {
                    remaining = 0;
                } else {
                    remaining = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
                }
            }

            current.setAmount(remaining);
            if (remaining <= 0)
                event.setCurrentItem(new ItemStack(Material.AIR));
            else {
                // Scheduler will update lore in 0-2 seconds
                event.setCurrentItem(current);
            }
            player.updateInventory();

            // Schedule delayed worth lore update
            plugin.getWorthVisualSystem().scheduleWorthUpdate(player);
            return;
        }

        // Ignore Middle Click
        if (event.getClick() == ClickType.MIDDLE) {
            return;
        }

        // Handle Double Click
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            if (cursor != null && cursor.getType() != Material.AIR) {
                event.setCancelled(true);
                int maxStack = cursor.getMaxStackSize();
                int currentAmount = cursor.getAmount();
                if (currentAmount >= maxStack)
                    return;

                Player player = (Player) event.getWhoClicked();
                org.bukkit.inventory.InventoryView view = event.getView();
                int totalSlots = view.countSlots();

                boolean changed = false;
                for (int i = 0; i < totalSlots; i++) {
                    if (currentAmount >= maxStack)
                        break;

                    // Skip result slots during mass gathering to be safe
                    if (view.getSlotType(i) == org.bukkit.event.inventory.InventoryType.SlotType.RESULT)
                        continue;

                    ItemStack item = view.getItem(i);
                    if (item == null || item.getType() == Material.AIR)
                        continue;

                    if (worthService.isSimilarIgnoringWorth(cursor, item)) {
                        int space = maxStack - currentAmount;
                        int toTake = Math.min(space, item.getAmount());
                        if (toTake > 0) {
                            if (item.getAmount() - toTake <= 0)
                                view.setItem(i, new ItemStack(Material.AIR));
                            else {
                                item.setAmount(item.getAmount() - toTake);
                                // Scheduler will update lore in 0-2 seconds
                                view.setItem(i, item);
                            }
                            currentAmount += toTake;
                            changed = true;
                        }
                    }
                }

                if (changed) {
                    cursor.setAmount(currentAmount);
                    // Scheduler will update lore in 0-2 seconds
                    player.setItemOnCursor(cursor);

                    // Critical: sync back to client
                    Bukkit.getScheduler().runTask(plugin, player::updateInventory);
                }
                return;
            }
        }

        // Handle manual click (placing/swapping similar items)
        if (cursor == null || cursor.getType() == Material.AIR || current == null || current.getType() == Material.AIR)
            return;
        if (cursor.isSimilar(current))
            return;

        if (worthService.isSimilarIgnoringWorth(cursor, current)) {
            int maxStack = current.getMaxStackSize();
            int space = maxStack - current.getAmount();
            if (space > 0) {
                int toMove = Math.min(space, cursor.getAmount());
                if (event.isRightClick())
                    toMove = 1;

                current.setAmount(current.getAmount() + toMove);
                // Scheduler will update lore in 0-2 seconds
                event.setCurrentItem(current);

                if (cursor.getAmount() - toMove <= 0) {
                    event.getWhoClicked().setItemOnCursor(new ItemStack(Material.AIR));
                } else {
                    cursor.setAmount(cursor.getAmount() - toMove);
                    // Scheduler will update lore in 0-2 seconds
                    event.getWhoClicked().setItemOnCursor(cursor);
                }
                event.setCancelled(true);
            }
        }

        // Schedule delayed worth lore update for manual stacking
        if (event.getWhoClicked() instanceof Player) {
            plugin.getWorthVisualSystem().scheduleWorthUpdate((Player) event.getWhoClicked());
        }
    }

    private boolean moveItemToRange(ItemStack item, org.bukkit.inventory.Inventory inv, int start, int end) {
        int remaining = item.getAmount();
        for (int i = start; i < end; i++) {
            ItemStack target = inv.getItem(i);
            if (target == null || target.getType() == Material.AIR)
                continue;
            if (target.getAmount() >= target.getMaxStackSize())
                continue;
            if (worthService.isSimilarIgnoringWorth(item, target)) {
                int space = target.getMaxStackSize() - target.getAmount();
                int toAdd = Math.min(space, remaining);
                target.setAmount(target.getAmount() + toAdd);
                // Scheduler will update lore in 0-2 seconds
                inv.setItem(i, target);
                remaining -= toAdd;
                if (remaining <= 0)
                    break;
            }
        }
        if (remaining > 0) {
            for (int i = start; i < end; i++) {
                ItemStack target = inv.getItem(i);
                if (target == null || target.getType() == Material.AIR) {
                    ItemStack newItem = item.clone();
                    newItem.setAmount(remaining);
                    // Scheduler will update lore in 0-2 seconds
                    inv.setItem(i, newItem);
                    remaining = 0;
                    break;
                }
            }
        }
        if (remaining != item.getAmount()) {
            item.setAmount(remaining);
            return true;
        }
        return false;
    }

    private boolean isArmor(ItemStack item) {
        String type = item.getType().name();
        return type.endsWith("_HELMET") || type.endsWith("_CHESTPLATE") ||
                type.endsWith("_LEGGINGS") || type.endsWith("_BOOTS") ||
                type.equals("TURTLE_HELMET") || type.equals("ELYTRA");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;
        ItemStack pickedItem = event.getItem().getItemStack();
        if (pickedItem.getType() == Material.AIR)
            return;

        for (ItemStack invItem : player.getInventory().getStorageContents()) {
            if (invItem == null || invItem.getType() == Material.AIR
                    || invItem.getAmount() >= invItem.getMaxStackSize())
                continue;

            if (worthService.isSimilarIgnoringWorth(pickedItem, invItem)) {
                int space = invItem.getMaxStackSize() - invItem.getAmount();
                int toAdd = Math.min(space, pickedItem.getAmount());
                if (toAdd > 0) {
                    invItem.setAmount(invItem.getAmount() + toAdd);
                    // Scheduler will update lore in 0-2 seconds
                    pickedItem.setAmount(pickedItem.getAmount() - toAdd);
                    if (pickedItem.getAmount() <= 0) {
                        event.getItem().remove();
                        event.setCancelled(true);
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
                        return;
                    } else {
                        event.getItem().setItemStack(pickedItem);
                    }
                }
            }
        }

        // Schedule delayed worth lore update
        plugin.getWorthVisualSystem().scheduleWorthUpdate(player);
    }
}
