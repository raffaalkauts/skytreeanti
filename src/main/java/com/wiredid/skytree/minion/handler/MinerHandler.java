package com.wiredid.skytree.minion.handler;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.MinionData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

public class MinerHandler {

    private final SkytreePlugin plugin;

    public MinerHandler(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void handle(MinionData data) {
        // Cooldown check (speed is actions per second-ish, but let's use 1 action every X seconds)
        long cooldown = (long) (5000 / data.getEffectiveSpeed()); // Base 5s, reduced by level
        if (System.currentTimeMillis() - data.getLastAction() < cooldown) {
            return;
        }

        Location loc = data.getLocation();
        int radius = data.getEffectiveRange();
        
        // Find a block to mine (1 block below)
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block b = loc.clone().add(x, -1, z).getBlock();
                if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK && !b.hasMetadata("skytree_regen")) {
                    mineBlock(data, b);
                    data.updateLastAction();
                    return;
                }
            }
        }
    }

    private void mineBlock(MinionData data, Block block) {
        Material original = block.getType();
        
        // Collect drops
        for (ItemStack drop : block.getDrops()) {
            if (!data.addToStorage(drop)) {
                block.getWorld().dropItemNaturally(data.getLocation(), drop);
            }
        }
        
        block.setType(Material.AIR);
        block.setMetadata("skytree_regen", new FixedMetadataValue(plugin, true));

        // Regen logic
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (block.getType() == Material.AIR) {
                block.setType(original);
                block.removeMetadata("skytree_regen", plugin);
            }
        }, 100L); // 5 seconds regen
    }
}
