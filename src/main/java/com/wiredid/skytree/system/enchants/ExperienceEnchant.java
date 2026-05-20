package com.wiredid.skytree.system.enchants;

import com.wiredid.skytree.api.CustomEnchant;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.Arrays;

public class ExperienceEnchant extends CustomEnchant {
    public ExperienceEnchant() {
        super("experience", "Experience", Rarity.SIMPLE, 5, Arrays.asList(
                Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL));
    }

    @Override
    public void onTrigger(Event event, int level, ItemStack item) {
        if (event instanceof BlockBreakEvent) {
            BlockBreakEvent e = (BlockBreakEvent) event;
            e.setExpToDrop((int) (e.getExpToDrop() * (1 + (level * 0.2)))); // 20% more exp per level
        }
    }

    @Override
    public String getDescription() {
        return "§7Increases experience dropped from blocks.";
    }
}
