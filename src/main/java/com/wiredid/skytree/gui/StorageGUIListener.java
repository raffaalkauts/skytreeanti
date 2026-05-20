package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.api.StorageService;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class StorageGUIListener implements Listener {

    private final SkytreePlugin plugin;
    private final StorageService storageService;
    private final ItemRegistry itemRegistry;
    private final NamespacedKey controllerKey;

    public StorageGUIListener(SkytreePlugin plugin, StorageService storageService, ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.storageService = storageService;
        this.itemRegistry = itemRegistry;
        this.controllerKey = new NamespacedKey(plugin, "storage_controller");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        String title = ComponentUtil.toLegacy(event.getView().title());

        if (!title.equals("§9§lStorage Network"))
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        // Find controller location from the Info item (slot 53)
        ItemStack infoItem = event.getInventory().getItem(53);
        if (infoItem == null || !infoItem.hasItemMeta())
            return;

        String locStr = infoItem.getItemMeta().getPersistentDataContainer().get(controllerKey,
                PersistentDataType.STRING);
        if (locStr == null)
            return;

        String[] parts = locStr.split(",");
        Location controllerLoc = new Location(plugin.getServer().getWorld(parts[0]),
                Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));

        // Check if player clicked a valid item (not info item)
        if (clicked.getType() != Material.CHEST && clicked.getType() != Material.AIR) {
            // Extract item
            int amount = event.isShiftClick() ? 64 : 1;
            ItemStack template = clicked.clone();
            template.setAmount(1); // Template for matching

            ItemStack extracted = storageService.extractItem(controllerLoc, template, amount);
            if (extracted != null) {
                player.getInventory().addItem(extracted);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);

                // Refresh GUI
                new StorageGUI(plugin, storageService, itemRegistry).open(player, controllerLoc);
            } else {
                player.sendMessage("§cItem not available!");
            }
        }
    }
}

