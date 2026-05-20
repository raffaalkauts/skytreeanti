package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SellWandListener implements Listener {

    private final SkytreePlugin plugin;
    private final SellGUIListener sellGUIListener;

    public SellWandListener(SkytreePlugin plugin, SellGUIListener sellGUIListener) {
        this.plugin = plugin;
        this.sellGUIListener = sellGUIListener;
    }

    @EventHandler
    public void onWandUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getItemRegistry().isCustomItem(item))
            return;
        String id = plugin.getItemRegistry().getItemId(item);
        if (id == null || !id.equals("sell_wand"))
            return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Container))
            return;

        event.setCancelled(true);
        Container container = (Container) block.getState();
        Inventory inv = container.getInventory();

        double totalValue = 0;
        int soldItems = 0;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack slot = inv.getItem(i);
            if (slot == null || slot.getType() == Material.AIR)
                continue;

            double price = sellGUIListener.getSellPrice(slot);
            if (price > 0) {
                totalValue += price * slot.getAmount();
                soldItems += slot.getAmount();
                inv.setItem(i, null);
            }
        }

        if (totalValue > 0) {
            double multiplier = plugin.getConfig().getDouble("shop.sell_wand_multiplier", 3.0);
            double multipliedValue = totalValue * multiplier;
            plugin.getEconomyService().addBalance(player.getUniqueId(), multipliedValue);

            player.sendMessage("§6§l[SellWand] §aYou sold §e" + soldItems + " §aitems for §e"
                    + com.wiredid.skytree.util.NumberUtil.formatCurrency(multipliedValue) + " §7(" + (int) multiplier + "x Multiplier!)");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
            player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 1, 0.5),
                    20, 0.3, 0.3, 0.3);
        } else {
            player.sendMessage("§c§l[SellWand] §7This container has no sellable items!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        }
    }
}
