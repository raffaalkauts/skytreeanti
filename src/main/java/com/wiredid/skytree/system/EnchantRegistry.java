package com.wiredid.skytree.system;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.CustomEnchant;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EnchantRegistry {

    private final SkytreePlugin plugin;
    private final Map<String, CustomEnchant> enchants = new HashMap<>();

    public EnchantRegistry(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void register(CustomEnchant enchant) {
        enchants.put(enchant.getId().toLowerCase(), enchant);
    }

    public CustomEnchant getEnchant(String id) {
        return enchants.get(id.toLowerCase());
    }

    public Collection<CustomEnchant> getAllEnchants() {
        return enchants.values();
    }

    /**
     * Applies an enchantment to an item stack.
     * Updates PDC and Lore.
     */
    public void applyEnchant(ItemStack item, CustomEnchant enchant, int level) {
        if (item == null || item.getItemMeta() == null)
            return;

        int finalLevel = Math.min(level, enchant.getMaxLevel());

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        NamespacedKey key = new NamespacedKey(plugin, "ce_" + enchant.getId().toLowerCase());
        pdc.set(key, PersistentDataType.INTEGER, finalLevel);

        item.setItemMeta(meta);
        refreshLore(item);
    }

    /**
     * Refreshes the lore of an item to reflect its custom enchantments.
     * Enchants are sorted by rarity (highest first) and descriptions are included.
     */
    public void refreshLore(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return;

        ItemMeta meta = item.getItemMeta();
        Map<CustomEnchant, Integer> currentEnchants = getEnchantsOnItem(item);

        if (currentEnchants.isEmpty())
            return;

        // Sort: FABLED -> LEGENDARY -> ULTIMATE -> ELITE -> UNIQUE -> SIMPLE
        List<CustomEnchant> sorted = new ArrayList<>(currentEnchants.keySet());
        sorted.sort((a, b) -> {
            int rarityCompare = Integer.compare(b.getRarity().ordinal(), a.getRarity().ordinal());
            if (rarityCompare != 0)
                return rarityCompare;
            return a.getDisplayName().compareTo(b.getDisplayName());
        });

        // Current lore
        List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        if (lore == null)
            lore = new ArrayList<>();

        // Remove old CE lore (lines starting with § rarity prefixes)
        // This is a bit tricky if we have other lore.
        // We assume CE lore is at the top.
        lore.removeIf(component -> {
            String legacy = com.wiredid.skytree.util.ComponentUtil.toLegacy(component);
            for (CustomEnchant.Rarity rarity : CustomEnchant.Rarity.values()) {
                if (legacy.startsWith(rarity.getPrefix()))
                    return true;
            }
            // Also remove descriptions (starting with §7)
            return legacy.startsWith("§7");
        });

        // Add new CE lore at the beginning
        List<net.kyori.adventure.text.Component> ceLore = new ArrayList<>();
        for (CustomEnchant enchant : sorted) {
            int level = currentEnchants.get(enchant);
            String title = enchant.getRarity().getPrefix() + " " + enchant.getDisplayName() + " " + romanNumeral(level);
            ceLore.add(com.wiredid.skytree.util.ComponentUtil.parse(title));
            ceLore.add(com.wiredid.skytree.util.ComponentUtil.parse(enchant.getDescription()));
        }

        // Combine
        ceLore.addAll(lore);
        meta.lore(ceLore);
        item.setItemMeta(meta);
    }

    public Map<CustomEnchant, Integer> getEnchantsOnItem(ItemStack item) {
        Map<CustomEnchant, Integer> found = new HashMap<>();
        if (item == null || !item.hasItemMeta())
            return found;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (CustomEnchant enchant : enchants.values()) {
            NamespacedKey key = new NamespacedKey(plugin, "ce_" + enchant.getId().toLowerCase());
            if (pdc.has(key, PersistentDataType.INTEGER)) {
                found.put(enchant, pdc.get(key, PersistentDataType.INTEGER));
            }
        }
        return found;
    }

    private String romanNumeral(int level) {
        switch (level) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            case 6:
                return "VI";
            case 7:
                return "VII";
            case 8:
                return "VIII";
            case 9:
                return "IX";
            case 10:
                return "X";
            default:
                return String.valueOf(level);
        }
    }
}
