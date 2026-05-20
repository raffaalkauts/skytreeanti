package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for essence growing mechanics
 */
public class EssenceGrowingListener implements Listener {

    private final ItemRegistry itemRegistry;

    public EssenceGrowingListener(SkytreePlugin plugin, ItemRegistry itemRegistry) {

        this.itemRegistry = itemRegistry;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        if (item == null || !itemRegistry.isCustomItem(item))
            return;

        String itemId = itemRegistry.getItemId(item);
        if (itemId == null || !itemId.startsWith("essence_"))
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        boolean consumed = false;

        switch (itemId) {
            case "essence_earth" -> {
                if (block.getType() == Material.GRASS_BLOCK || block.getType() == Material.DIRT) {
                    // Grow dirt above
                    Block above = block.getRelative(0, 1, 0);
                    if (above.getType() == Material.AIR) {
                        above.setType(Material.DIRT);
                        consumed = true;
                        event.getPlayer().sendMessage("§6§l[Skytree] §eEarth essence grows dirt!");
                    }
                }
            }
            case "essence_stone" -> {
                if (block.getType() == Material.STONE || block.getType() == Material.COBBLESTONE) {
                    Block above = block.getRelative(0, 1, 0);
                    if (above.getType() == Material.AIR) {
                        above.setType(Material.STONE);
                        consumed = true;
                        event.getPlayer().sendMessage("§7§l[Skytree] §7Stone essence grows stone!");
                    }
                }
            }
            case "essence_iron" -> {
                if (block.getType() == Material.IRON_BLOCK) {
                    Block above = block.getRelative(0, 1, 0);
                    if (above.getType() == Material.AIR) {
                        above.setType(Material.IRON_ORE);
                        consumed = true;
                        event.getPlayer().sendMessage("§f§l[Skytree] §fIron essence grows iron ore!");
                    }
                }
            }
            case "essence_gold" -> {
                if (block.getType() == Material.GOLD_BLOCK) {
                    Block above = block.getRelative(0, 1, 0);
                    if (above.getType() == Material.AIR) {
                        above.setType(Material.GOLD_ORE);
                        consumed = true;
                        event.getPlayer().sendMessage("§e§l[Skytree] §eGold essence grows gold ore!");
                    }
                }
            }
            case "essence_diamond" -> {
                if (block.getType() == Material.DIAMOND_BLOCK) {
                    Block above = block.getRelative(0, 1, 0);
                    if (above.getType() == Material.AIR) {
                        above.setType(Material.DIAMOND_ORE);
                        consumed = true;
                        event.getPlayer().sendMessage("§b§l[Skytree] §bDiamond essence grows diamond ore!");
                    }
                }
            }
        }

        if (consumed) {
            item.setAmount(item.getAmount() - 1);
            event.setCancelled(true);
        }
    }
}

