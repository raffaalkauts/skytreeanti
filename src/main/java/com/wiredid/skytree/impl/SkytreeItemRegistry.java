package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Implementation of ItemRegistry with dynamic loading from items.yml
 */
public class SkytreeItemRegistry implements ItemRegistry {

        private final Map<String, ItemStack> items;
        private final NamespacedKey itemIdKey;
        private final SkytreePlugin plugin;

        public SkytreeItemRegistry(SkytreePlugin plugin) {
                this.items = new HashMap<>();
                this.itemIdKey = new NamespacedKey(plugin, "item_id");
                this.plugin = plugin;
                registerAllItems();
        }

        @Override
        public void reload() {
                items.clear();
                registerAllItems();
                plugin.getLogger()
                                .info("Reloaded and registered " + items.size() + " custom items from configuration!");
        }

        @Override
        public void registerAllItems() {
                File itemFile = new File(plugin.getDataFolder(), "items.yml");
                if (!itemFile.exists()) {
                        plugin.saveResource("items.yml", false);
                }

                YamlConfiguration config = YamlConfiguration.loadConfiguration(itemFile);
                ConfigurationSection root = config.getConfigurationSection("items");

                if (root == null) {
                        plugin.getLogger().warning("items.yml is missing 'items' section!");
                        return;
                }

                for (String key : root.getKeys(false)) {
                        try {
                                ConfigurationSection itemSec = root.getConfigurationSection(key);
                                if (itemSec == null)
                                        continue;

                                String materialName = itemSec.getString("material", "STONE");
                                Material mat = Material.matchMaterial(materialName);
                                if (mat == null) {
                                        plugin.getLogger().warning(
                                                        "Invalid material for item " + key + ": " + materialName);
                                        continue;
                                }

                                ItemStack item = new ItemStack(mat);
                                ItemMeta meta = item.getItemMeta();

                                String name = itemSec.getString("name", key);
                                meta.displayName(ComponentUtil.smartParse(name));

                                java.util.List<String> lore = itemSec.getStringList("lore");
                                if (lore != null && !lore.isEmpty()) {
                                        meta.lore(ComponentUtil.parseList(lore.toArray(new String[0])));
                                }

                                // Enchants
                                ConfigurationSection enchants = itemSec.getConfigurationSection("enchants");
                                if (enchants != null) {
                                        for (String enchKey : enchants.getKeys(false)) {
                                                org.bukkit.enchantments.Enchantment enchantment;
                                                // Support 1.21 NamespacedKeys or Legacy names
                                                try {
                                                        @SuppressWarnings("deprecation")
                                                        org.bukkit.Registry<org.bukkit.enchantments.Enchantment> registry = org.bukkit.Registry.ENCHANTMENT;
                                                        enchantment = registry.get(org.bukkit.NamespacedKey
                                                                        .minecraft(enchKey.toLowerCase()));
                                                } catch (Exception e) {
                                                        continue;
                                                }

                                                if (enchantment != null) {
                                                        int level = enchants.getInt(enchKey);
                                                        meta.addEnchant(enchantment, level, true);
                                                }
                                        }
                                }

                                // Flags
                                java.util.List<String> flags = itemSec.getStringList("flags");
                                if (flags != null) {
                                        for (String flag : flags) {
                                                try {
                                                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf(flag));
                                                } catch (Exception ignored) {
                                                }
                                        }
                                }

                                // Attributes (Unbreakable)
                                if (itemSec.getBoolean("unbreakable")) {
                                        meta.setUnbreakable(true);
                                }

                                // Potion Support
                                if (mat == Material.POTION || mat == Material.SPLASH_POTION
                                                || mat == Material.LINGERING_POTION) {
                                        if (meta instanceof org.bukkit.inventory.meta.PotionMeta pm) {
                                                String pType = itemSec.getString("potion-type");
                                                if (pType != null) {
                                                        try {
                                                                pm.setBasePotionType(org.bukkit.potion.PotionType
                                                                                .valueOf(pType.toUpperCase()));
                                                        } catch (Exception ignored) {
                                                        }
                                                }
                                        }
                                }

                                meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING,
                                                key.toLowerCase());
                                item.setItemMeta(meta);

                                items.put(key.toLowerCase(), item);

                        } catch (Exception e) {
                                plugin.getLogger().log(Level.SEVERE, "Failed to load custom item: " + key, e);
                        }
                }
        }

        /**
         * Registers a pre-built custom item manually (code override).
         */
        public void registerCustomItem(String id, ItemStack item) {
                if (id == null)
                        return;
                id = id.toLowerCase();
                ItemMeta meta = item.getItemMeta();

                if (meta == null)
                        return;

                if (!meta.getPersistentDataContainer().has(itemIdKey, PersistentDataType.STRING)) {
                        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, id);
                        item.setItemMeta(meta);
                }
                items.put(id, item);
        }

        @Override
        public ItemStack getItem(String id) {
                if (id == null)
                        return null;
                ItemStack orig = items.get(id.toLowerCase());
                return orig != null ? orig.clone() : null;
        }

        @Override
        public boolean isCustomItem(ItemStack item) {
                if (item == null || !item.hasItemMeta())
                        return false;
                return item.getItemMeta().getPersistentDataContainer().has(itemIdKey, PersistentDataType.STRING);
        }

        @Override
        public String getItemId(ItemStack item) {
                if (!isCustomItem(item))
                        return null;
                return item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        }

        @Override
        public Set<String> getAllItemIds() {
                return items.keySet();
        }
}
