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
 * Crucible GUI - Slimefun style
 * Melting materials into lava
 */
public class CrucibleGUI {

    public CrucibleGUI(SkytreePlugin plugin) {

    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lCrucible - Melting"));

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
        ItemStack item = new ItemStack(Material.CAULDRON);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§6§lCrucible Melting"));
        meta.lore(ComponentUtil.parseList(
                "§7Melt materials into lava!",
                "",
                "§eHow to use:",
                "§71. Place meltable items",
                "§72. Click Process button",
                "§73. Get lava bucket!",
                "",
                "§aMeltable materials:",
                "§7- Cobblestone → Lava",
                "§7- Stone → Lava",
                "§7- Netherrack → Lava",
                "§7- Obsidian → Lava (slow)"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createProcessButton() {
        ItemStack item = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§c§lMelt"));
        meta.lore(ComponentUtil.parseList(
                "§7Click to melt materials",
                "§7into lava!"));
        item.setItemMeta(meta);
        return item;
    }
}
