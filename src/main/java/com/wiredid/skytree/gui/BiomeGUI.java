package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * GUI for changing island biome
 */
public class BiomeGUI {

        public BiomeGUI(SkytreePlugin plugin) {

        }

        public void open(Player player, Island island) {
                Inventory gui = Bukkit.createInventory(null, 36, ComponentUtil.parse("§6§lIsland §8» §7Biomes"));

                // Premium Border
                com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.CYAN_STAINED_GLASS_PANE,
                                Material.BLUE_STAINED_GLASS_PANE);

                // Biome options
                gui.setItem(10,
                                createBiomeItem(Material.GRASS_BLOCK, "§aPlains",
                                                "§7Cost: §e" + com.wiredid.skytree.util.NumberUtil.formatCurrency(1000),
                                                "§7Default grassland biome"));
                gui.setItem(11, createBiomeItem(Material.SAND, "§eDesert",
                                "§7Cost: §e" + com.wiredid.skytree.util.NumberUtil.formatCurrency(2000),
                                "§7Hot and sandy"));
                gui.setItem(12,
                                createBiomeItem(Material.SNOW_BLOCK, "§fSnowy Plains",
                                                "§7Cost: §e" + com.wiredid.skytree.util.NumberUtil.formatCurrency(2000),
                                                "§7Cold and snowy"));
                gui.setItem(13, createBiomeItem(Material.RED_MUSHROOM, "§dMushroom Fields",
                                "§7Cost: §e" + com.wiredid.skytree.util.NumberUtil.formatCurrency(5000),
                                "§7Rare mushroom biome", "§7No hostile mobs!"));
                gui.setItem(14,
                                createBiomeItem(Material.JUNGLE_SAPLING, "§2Jungle",
                                                "§7Cost: §e" + com.wiredid.skytree.util.NumberUtil.formatCurrency(3000),
                                                "§7Dense vegetation"));
                gui.setItem(15,
                                createBiomeItem(Material.DARK_OAK_SAPLING, "§8Dark Forest",
                                                "§7Cost: §e" + com.wiredid.skytree.util.NumberUtil.formatCurrency(3000),
                                                "§7Spooky trees"));
                gui.setItem(16, createBiomeItem(Material.CHERRY_SAPLING, "§dCherry Grove",
                                "§7Cost: §e" + com.wiredid.skytree.util.NumberUtil.formatCurrency(4000),
                                "§7Beautiful pink trees"));

                // Current biome indicator
                gui.setItem(22, createInfoItem(Material.COMPASS, "§eCurrent Biome", "§f" + island.getBiome()));

                player.openInventory(gui);
        }

        private ItemStack createBiomeItem(Material material, String name, String... lore) {
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(ComponentUtil.parse(name));
                meta.lore(ComponentUtil.parseList(lore));
                item.setItemMeta(meta);
                return item;
        }

        private ItemStack createInfoItem(Material material, String name, String... lore) {
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(ComponentUtil.parse(name));
                meta.lore(ComponentUtil.parseList(lore));
                item.setItemMeta(meta);
                return item;
        }
}
