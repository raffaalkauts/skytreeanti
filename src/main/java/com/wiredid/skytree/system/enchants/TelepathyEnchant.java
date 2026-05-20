package com.wiredid.skytree.system.enchants;

import com.wiredid.skytree.api.CustomEnchant;
import org.bukkit.event.Event;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.Arrays;

public class TelepathyEnchant extends CustomEnchant {
    public TelepathyEnchant() {
        super("telepathy", "Telepathy", Rarity.UNIQUE, 4, Arrays.asList(
                Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL, Material.DIAMOND_HOE, Material.NETHERITE_HOE));
    }

    @Override
    public void onTrigger(Event event, int level, ItemStack item) {
        // Handled via PDC in other listeners
    }

    @Override
    public String getDescription() {
        return "§7Automatically sends drops to inventory.";
    }
}
