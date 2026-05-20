package com.wiredid.skytree.system.enchants;

import com.wiredid.skytree.api.CustomEnchant;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.Material;
import java.util.Arrays;

public class AbidingEnchant extends CustomEnchant {
    public AbidingEnchant() {
        super("abiding", "Abiding", Rarity.LEGENDARY, 1, Arrays.asList(
                Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE, Material.DIAMOND_SHOVEL,
                Material.NETHERITE_SHOVEL,
                Material.DIAMOND_HOE, Material.NETHERITE_HOE, Material.BOW, Material.CROSSBOW, Material.TRIDENT));
    }

    @Override
    public void onTrigger(Event event, int level, ItemStack item) {
        if (item.getItemMeta() instanceof Damageable) {
            Damageable meta = (Damageable) item.getItemMeta();
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
    }

    @Override
    public String getDescription() {
        return "§7Makes the item unbreakable.";
    }
}
