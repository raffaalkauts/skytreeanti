package com.wiredid.skytree.minion.handler;

import com.wiredid.skytree.model.MinionData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;

public class FarmerHandler {

    public FarmerHandler() {
    }

    public void handle(MinionData data) {
        long cooldown = (long) (5000 / data.getEffectiveSpeed());
        if (System.currentTimeMillis() - data.getLastAction() < cooldown) {
            return;
        }

        Location loc = data.getLocation();
        int radius = data.getEffectiveRange();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block b = loc.clone().add(x, 0, z).getBlock();
                
                // Harvest
                if (b.getBlockData() instanceof Ageable ageable) {
                    if (ageable.getAge() == ageable.getMaximumAge()) {
                        harvest(data, b);
                        data.updateLastAction();
                        return;
                    }
                }
                
                // Replant
                if (b.getType() == Material.AIR && b.getRelative(0, -1, 0).getType() == Material.FARMLAND) {
                    if (replant(data, b)) {
                        data.updateLastAction();
                        return;
                    }
                }
            }
        }
    }

    private void harvest(MinionData data, Block block) {
        for (ItemStack drop : block.getDrops()) {
            if (!data.addToStorage(drop)) {
                block.getWorld().dropItemNaturally(data.getLocation(), drop);
            }
        }
        block.setType(Material.AIR);
    }

    private boolean replant(MinionData data, Block block) {
        for (ItemStack item : data.getStorage()) {
            if (item == null) continue;
            Material plant = getPlantType(item.getType());
            if (plant != null) {
                block.setType(plant);
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    private Material getPlantType(Material seed) {
        return switch (seed) {
            case WHEAT_SEEDS -> Material.WHEAT;
            case CARROT -> Material.CARROTS;
            case POTATO -> Material.POTATOES;
            case BEETROOT_SEEDS -> Material.BEETROOTS;
            case PUMPKIN_SEEDS -> Material.PUMPKIN_STEM;
            case MELON_SEEDS -> Material.MELON_STEM;
            default -> null;
        };
    }
}
