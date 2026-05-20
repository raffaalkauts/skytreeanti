package com.wiredid.skytree.system.enchants;

import com.wiredid.skytree.api.CustomEnchant;
import com.wiredid.skytree.fishing.FishingModels.RodEnchant;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import java.util.Collections;

public class FishingRodEnchants {

    public static class GenericFishingEnchant extends CustomEnchant {
        public GenericFishingEnchant(RodEnchant rodEnchant) {
            super(rodEnchant.name().toLowerCase(), rodEnchant.getDisplayName(),
                    translateRarity(rodEnchant), 5, Collections.singletonList(Material.FISHING_ROD));
            this.rodEnchant = rodEnchant;
        }

        private final RodEnchant rodEnchant;

        private static Rarity translateRarity(RodEnchant re) {
            // Simplified translation for registry purposes
            return Rarity.UNIQUE;
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            // Logic is handled in FishingService for these specialized enchants
        }

        @Override
        public String getDescription() {
            return "§7" + rodEnchant.getDescription();
        }
    }

    // Since we need unique classes for registration if we use the current registry
    // Or we can just loop and register instances.
}
