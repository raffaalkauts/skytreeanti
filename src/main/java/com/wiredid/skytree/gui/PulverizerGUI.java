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
 * Pulverizer GUI - Slimefun style
 * Crushes ores into dusts
 */
public class PulverizerGUI {

    public PulverizerGUI(SkytreePlugin plugin) {

    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6Pulverizer GUI"));

        // Info item
        gui.setItem(13, createInfoItem());

        // Processing button
        gui.setItem(22, createProcessButton());

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE);

        player.openInventory(gui);
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.IRON_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§eInput Item"));
        meta.lore(ComponentUtil.parseList(
                "§7Crush ores into dusts!",
                "",
                "§eHow to use:",
                "§71. Place ores in input",
                "§72. Click Pulverize button",
                "§73. Get dusts!",
                "",
                "§aRecipes:",
                "§7- Iron Ore → 2x Iron Dust",
                "§7- Gold Ore → 2x Gold Dust",
                "§7- Copper Ore → 2x Copper Dust",
                "§7- Cobblestone → Gravel",
                "§7- Gravel → Sand"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createProcessButton() {
        ItemStack item = new ItemStack(Material.FLINT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§c§lPulverize"));
        meta.lore(ComponentUtil.parseList(
                "§7Click to crush items"));
        item.setItemMeta(meta);
        return item;
    }
}
