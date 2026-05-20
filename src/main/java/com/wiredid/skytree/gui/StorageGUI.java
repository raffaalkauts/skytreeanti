package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.api.StorageService;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.List;

public class StorageGUI {

    private final StorageService storageService;

    private final NamespacedKey controllerKey;

    public StorageGUI(SkytreePlugin plugin, StorageService storageService, ItemRegistry itemRegistry) {

        this.storageService = storageService;

        this.controllerKey = new NamespacedKey(plugin, "storage_controller");
    }

    public void open(Player player, Location controllerLoc) {
        Inventory gui = Bukkit.createInventory(null, 54, ComponentUtil.parse("§9§lStorage Network"));

        List<ItemStack> items = storageService.getAllItems(controllerLoc);

        // Simple pagination: just first 45 items for now
        for (int i = 0; i < Math.min(items.size(), 45); i++) {
            ItemStack item = items.get(i);
            gui.setItem(i, createDisplayItem(item));
        }

        // Store controller location in invisible item or holder?
        // Since we can't easily store data on the inventory itself without a custom
        // holder,
        // we'll put a marker item or rely on the listener to know context (hard without
        // holder).
        // Let's allow the listener to parse a hidden item or just title match +
        // metadata on items.

        // Actually, we can put a "Info" icon with the location data
        gui.setItem(53, createInfoItem(controllerLoc));

        player.openInventory(gui);
    }

    private ItemStack createDisplayItem(ItemStack item) {
        ItemStack display = item.clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = new java.util.ArrayList<>();
        lore.add("§7Count: §e" + item.getAmount());
        lore.add("§7Left-Click to take 1");
        lore.add("§7Shift-Click to take stack");
        meta.lore(ComponentUtil.parseList(lore));
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack createInfoItem(Location loc) {
        ItemStack item = new ItemStack(org.bukkit.Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§eNetwork Info"));
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(controllerKey, PersistentDataType.STRING,
                loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        item.setItemMeta(meta);
        return item;
    }
}

