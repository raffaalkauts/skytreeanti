package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Barrel GUI - Slimefun style
 * Composting organic materials into dirt
 */
public class BarrelGUI {

    public BarrelGUI(SkytreePlugin plugin) {

    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6Barrel GUI"));

        // Info item
        gui.setItem(13, createInfoItem());

        // Processing button
        gui.setItem(22, createProcessButton());

        player.openInventory(gui);
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.COMPOSTER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§6§lBarrel Composting"));
        meta.lore(ComponentUtil.parseList(Arrays.asList(
                "§7Compost organic materials into dirt!",
                "",
                "§eHow to use:",
                "§71. Place organic items in slots",
                "§72. Click Process button",
                "§73. Get dirt output!",
                "",
                "§aCompostable items:",
                "§7- Leaves, Saplings",
                "§7- Seeds, Wheat",
                "§7- Rotten Flesh",
                "§7- Any plant material")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createProcessButton() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§a§lProcess"));
        meta.lore(ComponentUtil.parseList(Arrays.asList(
                "§7Click to compost materials",
                "§7into dirt!")));
        item.setItemMeta(meta);
        return item;
    }
}

