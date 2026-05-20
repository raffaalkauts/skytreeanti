package com.wiredid.skytree.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiUtil {

    public static void applyPremiumBorder(Inventory inv, Material primary, Material secondary) {
        int size = inv.getSize();
        ItemStack primaryPane = createGlassPane(primary);
        ItemStack secondaryPane = createGlassPane(secondary);

        for (int i = 0; i < size; i++) {
            // Fill edges
            if (isEdge(i, size)) {
                if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                    // Pattern: Alternating
                    if (i % 2 == 0) {
                        inv.setItem(i, primaryPane);
                    } else {
                        inv.setItem(i, secondaryPane);
                    }
                }
            }
        }
    }

    private static boolean isEdge(int slot, int size) {
        int rows = size / 9;
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }

    public static java.util.List<Integer> getInteriorSlots(int size) {
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (!isEdge(i, size)) {
                slots.add(i);
            }
        }
        return slots;
    }

    private static ItemStack createGlassPane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ComponentUtil.parse(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ComponentUtil.parse(name));
            java.util.List<String> loreList = new java.util.ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.lore(ComponentUtil.parseList(loreList));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createGlass(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ComponentUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void addGlow(ItemStack item) {
        if (item == null)
            return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
    }
}
