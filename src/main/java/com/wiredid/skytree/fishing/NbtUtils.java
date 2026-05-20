package com.wiredid.skytree.fishing;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class NbtUtils {

    private static SkytreePlugin plugin;
    private static boolean keysInitialized = false;

    public static void setPlugin(SkytreePlugin p) {
        plugin = p;
        keysInitialized = false;
        initKeys();
    }

    // Keys
    public static NamespacedKey KEY_ROD_ID;
    public static NamespacedKey KEY_ROD_TIER;
    public static NamespacedKey KEY_ROD_SLOTS;
    public static NamespacedKey KEY_ROD_ENCHANTS;
    public static NamespacedKey KEY_PITY_RARE;
    public static NamespacedKey KEY_PITY_LEGEND;
    public static NamespacedKey KEY_PITY_MYTHIC;
    public static NamespacedKey KEY_PITY_LIMITED;
    public static NamespacedKey KEY_FISH_ID;
    public static NamespacedKey KEY_FISH_RARITY;
    public static NamespacedKey KEY_FISH_WEIGHT;
    public static NamespacedKey KEY_FISH_MUTATION;
    public static NamespacedKey KEY_FISH_PRICE;

    private static void initKeys() {
        if (keysInitialized || plugin == null) return;
        keysInitialized = true;
        KEY_ROD_ID = new NamespacedKey(plugin, "rod_id");
        KEY_ROD_TIER = new NamespacedKey(plugin, "rod_tier");
        KEY_ROD_SLOTS = new NamespacedKey(plugin, "rod_slots");
        KEY_ROD_ENCHANTS = new NamespacedKey(plugin, "rod_enchants");
        KEY_PITY_RARE = new NamespacedKey(plugin, "pity_rare");
        KEY_PITY_LEGEND = new NamespacedKey(plugin, "pity_legend");
        KEY_PITY_MYTHIC = new NamespacedKey(plugin, "pity_mythic");
        KEY_PITY_LIMITED = new NamespacedKey(plugin, "pity_limited");
        KEY_FISH_ID = new NamespacedKey(plugin, "fish_id");
        KEY_FISH_RARITY = new NamespacedKey(plugin, "fish_rarity");
        KEY_FISH_WEIGHT = new NamespacedKey(plugin, "fish_weight");
        KEY_FISH_MUTATION = new NamespacedKey(plugin, "fish_mutation");
        KEY_FISH_PRICE = new NamespacedKey(plugin, "fish_price");
    }

    private static SkytreePlugin getPlugin() {
        return plugin;
    }

    // Rod Utils
    public static void setRodId(ItemStack item, UUID id) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_ROD_ID, PersistentDataType.STRING, id.toString());
            item.setItemMeta(meta);
        }
    }

    public static UUID getRodId(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        String idStr = item.getItemMeta().getPersistentDataContainer().get(KEY_ROD_ID, PersistentDataType.STRING);
        return idStr != null ? UUID.fromString(idStr) : null;
    }

    public static void setRodTier(ItemStack item, FishingModels.RodTier tier) {
        setString(item, KEY_ROD_TIER, tier.name());
    }

    public static FishingModels.RodTier getRodTier(ItemStack item) {
        String tier = getString(item, KEY_ROD_TIER);
        return tier != null ? FishingModels.RodTier.valueOf(tier) : null;
    }

    public static void setRodSlots(ItemStack item, int slots) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_ROD_SLOTS, PersistentDataType.INTEGER, slots);
            item.setItemMeta(meta);
        }
    }

    public static int getRodSlots(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return 0;
        Integer slots = item.getItemMeta().getPersistentDataContainer().get(KEY_ROD_SLOTS, PersistentDataType.INTEGER);
        return slots != null ? slots : 0;
    }

    // Generic Utils
    public static void setString(ItemStack item, String key, String value) {
        setString(item, new NamespacedKey(plugin, key), value);
    }

    public static void setString(ItemStack item, NamespacedKey key, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
    }

    public static String getString(ItemStack item, String key) {
        return getString(item, new NamespacedKey(plugin, key));
    }

    public static String getString(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta())
            return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static void setInt(ItemStack item, NamespacedKey key, int value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
            item.setItemMeta(meta);
        }
    }

    public static int getInt(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta())
            return 0;
        Integer val = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return val != null ? val : 0;
    }

    public static void setDouble(ItemStack item, NamespacedKey key, double value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
            item.setItemMeta(meta);
        }
    }

    public static double getDouble(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta())
            return 0.0;
        Double val = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
        return val != null ? val : 0.0;
    }

    // Checks
    public static boolean isCustomRod(ItemStack item) {
        return getRodId(item) != null;
    }

    public static boolean isCustomFish(ItemStack item) {
        return getString(item, KEY_FISH_ID) != null;
    }

    public static void setBoolean(ItemStack item, String key, boolean value) {
        setBoolean(item, new NamespacedKey(plugin, key), value);
    }

    public static boolean getBoolean(ItemStack item, String key) {
        return getBoolean(item, new NamespacedKey(plugin, key));
    }

    public static void setBoolean(ItemStack item, NamespacedKey key, boolean value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) (value ? 1 : 0));
            item.setItemMeta(meta);
        }
    }

    public static boolean getBoolean(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta())
            return false;
        Byte val = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return val != null && val == 1;
    }
}
