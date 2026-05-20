package com.wiredid.skytree.system.enchants;

import com.wiredid.skytree.api.CustomEnchant;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Material;
import java.util.Arrays;

public class HasteEnchant extends CustomEnchant {
    public HasteEnchant() {
        super("haste", "Haste", Rarity.FABLED, 3, Arrays.asList(
                Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL, Material.DIAMOND_HOE, Material.NETHERITE_HOE));
    }

    @Override
    public void onTrigger(Event event, int level, ItemStack item) {
        if (event instanceof BlockBreakEvent) {
            BlockBreakEvent e = (BlockBreakEvent) event;
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, level - 1));
        }
    }

    @Override
    public String getDescription() {
        return "§7Gives Haste while mining.";
    }
}
