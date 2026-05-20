package com.wiredid.skytree.gui;

import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * GUI for Sieve machine
 */
public class SieveGUI {

    public SieveGUI(com.wiredid.skytree.SkytreePlugin plugin) {

    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6Sieve GUI"));

        // Input slot
        gui.setItem(10, createSlotIndicator(Material.DIRT, "§eInput Slot",
                "§7Place dirt, gravel, or sand here"));

        // Mesh slot
        gui.setItem(12, createSlotIndicator(Material.STRING, "§eMesh Slot",
                "§7Place a mesh here",
                "§7Higher tier = Better drops"));

        // Output slots (9 slots in grid)
        for (int i = 0; i < 9; i++) {
            gui.setItem(14 + i, new ItemStack(Material.AIR));
        }

        // Process button
        gui.setItem(13, createButton(Material.DIAMOND, "§a§lProcess",
                "§7Click to sieve!",
                "§7Costs: §e10 USDT per use"));

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE);

        player.openInventory(gui);
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
