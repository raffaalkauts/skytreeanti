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
 * Advanced Furnace GUI - Conventional Slimefun style
 * 10x faster smelting than vanilla furnace
 * Uses fuel like vanilla furnace, no electricity needed
 */
public class AdvancedFurnaceGUI {

    public AdvancedFurnaceGUI(SkytreePlugin plugin) {
        // Constructor
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lAdvanced Furnace"));

        // Create furnace-like layout
        // Slot 10: Input (items to smelt)
        gui.setItem(10, createSlotMarker(Material.GRAY_STAINED_GLASS_PANE, "§fInput Slot",
                "§7Place items to smelt here"));

        // Slot 12: Fuel
        gui.setItem(12, createSlotMarker(Material.ORANGE_STAINED_GLASS_PANE, "§eFuel Slot",
                "§7Place fuel here",
                "§7(Coal, Lava Bucket, etc.)"));

        // Slot 16: Output
        gui.setItem(16, createSlotMarker(Material.LIME_STAINED_GLASS_PANE, "§aOutput Slot",
                "§7Smelted items appear here"));

        // Slot 13: Progress indicator
        gui.setItem(13, createProgressIndicator(0));

        // Info item
        gui.setItem(4, createInfoItem());

        // Decoration/borders
        fillBorders(gui);

        player.openInventory(gui);
    }

    private ItemStack createSlotMarker(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.lore(ComponentUtil.parseList(Arrays.asList(lore)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createProgressIndicator(int progress) {
        ItemStack item = new ItemStack(Material.FLINT_AND_STEEL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§6Progress: " + progress + "%"));
        meta.lore(ComponentUtil.parseList(Arrays.asList(
                "§7Smelting speed: §e10x faster",
                "§7than vanilla furnace")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BLAST_FURNACE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§6§lAdvanced Furnace"));
        meta.lore(ComponentUtil.parseList(Arrays.asList(
                "§7Smelts items ultra-fast!",
                "",
                "§eHow to use:",
                "§71. Place items in Input slot",
                "§72. Add fuel to Fuel slot",
                "§73. Wait 1 second per item",
                "§74. Collect from Output",
                "",
                "§aFeatures:",
                "§7- 10x faster than vanilla",
                "§7- Uses conventional fuel",
                "§7- No electricity needed")));
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorders(Inventory gui) {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(ComponentUtil.parse(" "));
        border.setItemMeta(meta);

        // Fill all except slots 10, 12, 13, 16, and 4
        for (int i = 0; i < 27; i++) {
            if (i != 4 && i != 10 && i != 12 && i != 13 && i != 16) {
                if (gui.getItem(i) == null) {
                    gui.setItem(i, border);
                }
            }
        }
    }

}

