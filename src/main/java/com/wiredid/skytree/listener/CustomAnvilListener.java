package com.wiredid.skytree.listener;

import com.wiredid.skytree.api.CustomEnchant;
import com.wiredid.skytree.system.EnchantRegistry;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import java.util.Map;

public class CustomAnvilListener implements Listener {

    private final EnchantRegistry registry;

    public CustomAnvilListener(EnchantRegistry registry) {
        this.registry = registry;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);

        if (first == null || second == null || first.getType() == Material.AIR || second.getType() == Material.AIR) {
            return;
        }

        ItemStack result = first.clone();
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta == null)
            return;

        boolean changed = false;

        // --- Handle Vanilla Enchantments ---
        Map<Enchantment, Integer> firstEnchants = getEnchants(first);
        Map<Enchantment, Integer> secondEnchants = getEnchants(second);

        for (Map.Entry<Enchantment, Integer> entry : secondEnchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            int secondLevel = entry.getValue();
            int firstLevel = firstEnchants.getOrDefault(enchant, 0);

            if (firstLevel == secondLevel && firstLevel > 0) {
                int newLevel = firstLevel + 1;
                setEnchant(resultMeta, enchant, newLevel);
                changed = true;
            } else if (secondLevel > firstLevel) {
                setEnchant(resultMeta, enchant, secondLevel);
                changed = true;
            }
        }

        // Apply vanilla changes to result before custom enchants (so PDC/Lore logic
        // sees them)
        result.setItemMeta(resultMeta);

        // --- Handle Custom Enchantments ---
        Map<CustomEnchant, Integer> firstCE = registry.getEnchantsOnItem(first);
        Map<CustomEnchant, Integer> secondCE = registry.getEnchantsOnItem(second);

        if (!secondCE.isEmpty()) {
            for (Map.Entry<CustomEnchant, Integer> entry : secondCE.entrySet()) {
                CustomEnchant enchant = entry.getKey();
                int secondLevel = entry.getValue();
                int firstLevel = firstCE.getOrDefault(enchant, 0);

                // Check if enchantment can be applied to the first item (base)
                if (result.getType() != Material.ENCHANTED_BOOK && !enchant.getAppliesTo().contains(result.getType())) {
                    continue;
                }

                int newLevel = firstLevel;
                if (firstLevel == secondLevel) {
                    newLevel = Math.min(enchant.getMaxLevel(), firstLevel + 1);
                } else {
                    newLevel = Math.max(firstLevel, secondLevel);
                }

                if (newLevel > firstLevel) {
                    registry.applyEnchant(result, enchant, newLevel);
                    changed = true;
                } else if (firstLevel == 0 && newLevel > 0) {
                    registry.applyEnchant(result, enchant, newLevel);
                    changed = true;
                }
            }
        }

        // --- Handle Final Result ---
        if (changed) {
            // Re-apply lore refresh to ensure everything is sorted and pretty
            registry.refreshLore(result);

            int repairCost = 0;
            if (first.getItemMeta() instanceof Repairable r1)
                repairCost += r1.getRepairCost();
            if (second.getItemMeta() instanceof Repairable r2)
                repairCost += r2.getRepairCost();

            repairCost += 5;

            ItemMeta finalMeta = result.getItemMeta();
            if (finalMeta instanceof Repairable repairableMeta) {
                repairableMeta.setRepairCost(Math.max(1, repairCost));
            }

            result.setItemMeta(finalMeta);
            event.setResult(result);
        }
    }

    private Map<Enchantment, Integer> getEnchants(ItemStack item) {
        if (item.getType() == Material.ENCHANTED_BOOK) {
            if (item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
                return meta.getStoredEnchants();
            }
        }
        return item.getEnchantments();
    }

    private void setEnchant(ItemMeta meta, Enchantment enchant, int level) {
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            bookMeta.addStoredEnchant(enchant, level, true);
        } else {
            meta.addEnchant(enchant, level, true);
        }
    }
}
