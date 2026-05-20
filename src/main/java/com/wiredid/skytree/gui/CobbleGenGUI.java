package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * GUI for Cobblestone Generator
 */
public class CobbleGenGUI {

    public CobbleGenGUI(SkytreePlugin plugin) {

    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6Cobble Generator"));

        // Status
        gui.setItem(4, createStatus(Material.COBBLESTONE, "§7Generator Status",
                "§aActive",
                "§7Speed: §f1 block/10s",
                "§7Ore Chance: §f5%"));

        // Output buffer
        for (int i = 10; i < 17; i++) {
            gui.setItem(i, new ItemStack(Material.AIR));
        }

        // Upgrade slots
        gui.setItem(19, createSlotIndicator(Material.REDSTONE, "§cSpeed Upgrade",
                "§7Place speed upgrade here"));
        gui.setItem(20, createSlotIndicator(Material.DIAMOND_PICKAXE, "§bFortune Upgrade",
                "§7Increase ore chance"));

        // Controls
        gui.setItem(22, createButton(Material.LEVER, "§eToggle Generator",
                "§7Click to start/stop"));
        gui.setItem(23, createButton(Material.CHEST, "§aCollect All",
                "§7Collect all items"));

        player.openInventory(gui);
    }

    private ItemStack createStatus(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSlotIndicator(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }
}

