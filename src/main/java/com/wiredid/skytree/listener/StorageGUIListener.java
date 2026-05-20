package com.wiredid.skytree.listener;

import com.wiredid.skytree.api.StorageService;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class StorageGUIListener implements Listener {

    private final StorageService storageService;

    public StorageGUIListener(StorageService storageService) {
        this.storageService = storageService;
    }

    // Store which controller a player is looking at
    private final java.util.Map<java.util.UUID, org.bukkit.Location> openStorageLocations = new java.util.HashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        if (block.getType() == Material.OBSERVER) {
            // Open Storage GUI
            event.setCancelled(true);
            openStorageGUI(event.getPlayer(), block.getLocation());
        }
    }

    private void openStorageGUI(Player player, org.bukkit.Location loc) {
        Inventory gui = Bukkit.createInventory(null, 54, ComponentUtil.parse("§c§lStorage Network"));

        List<ItemStack> items = storageService.getAllItems(loc);
        for (ItemStack item : items) {
            if (gui.firstEmpty() != -1) {
                gui.addItem(item);
            }
        }

        openStorageLocations.put(player.getUniqueId(), loc);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.stripColor(event.getView().title());
        if (!title.contains("Storage Network"))
            return;

        event.setCancelled(true); // Prevent normal interaction

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        org.bukkit.Location controllerLoc = openStorageLocations.get(player.getUniqueId());

        if (controllerLoc == null) {
            player.closeInventory();
            return;
        }

        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            // Clicked in GUI -> Extract
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                // Try to extract the item
                int amountToTake = event.isShiftClick() ? clicked.getAmount() : 1;
                ItemStack extracted = storageService.extractItem(controllerLoc, clicked, amountToTake);

                if (extracted != null) {
                    player.getInventory().addItem(extracted);
                    // Refresh GUI
                    refreshGUI(player, event.getView().getTopInventory(), controllerLoc);
                } else {
                    player.sendMessage("§cItem not available!");
                }
            }
        } else if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            // Clicked in Player Inv -> Insert
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                ItemStack remaining = storageService.insertItem(controllerLoc, clicked);
                event.getClickedInventory().setItem(event.getSlot(), remaining);

                // Refresh GUI
                refreshGUI(player, event.getView().getTopInventory(), controllerLoc);
            }
        }
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        openStorageLocations.remove(event.getPlayer().getUniqueId());
    }

    private void refreshGUI(Player player, Inventory gui, org.bukkit.Location loc) {
        gui.clear();
        List<ItemStack> items = storageService.getAllItems(loc);
        for (ItemStack item : items) {
            if (gui.firstEmpty() != -1) {
                gui.addItem(item);
            }
        }
    }
}
