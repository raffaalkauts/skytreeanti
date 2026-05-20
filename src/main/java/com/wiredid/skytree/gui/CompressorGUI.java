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
 * Compressor GUI - Slimefun style
 * Compresses items into blocks or special materials
 */
public class CompressorGUI {

    public CompressorGUI(SkytreePlugin plugin) {

    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6Compressor GUI"));

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
        ItemStack item = new ItemStack(Material.PISTON);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§eInput Items (9x)"));
        meta.lore(ComponentUtil.parseList(
                "§7Compress items into blocks!",
                "",
                "§eHow to use:",
                "§71. Place 9 items in input",
                "§72. Click Compress button",
                "§73. Get compressed block!",
                "",
                "§aRecipes:",
                "§7- 9 Cobblestone → Compressed Cobble",
                "§7- 9 Dirt → Compressed Dirt",
                "§7- 9 Gravel → Compressed Gravel",
                "§7- 9 Sand → Compressed Sand",
                "§7- 9 Dust → Compressed Dust"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createProcessButton() {
        ItemStack item = new ItemStack(Material.IRON_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§a§lCompress"));
        meta.lore(ComponentUtil.parseList(
                "§7Click to compress items"));
        item.setItemMeta(meta);
        return item;
    }
}
