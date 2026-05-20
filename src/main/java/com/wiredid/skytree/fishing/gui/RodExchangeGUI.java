package com.wiredid.skytree.fishing.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.fishing.FishingService;
import com.wiredid.skytree.fishing.NbtUtils;
import com.wiredid.skytree.fishing.FishingModels.Rarity;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RodExchangeGUI implements Listener {

    private final SkytreePlugin plugin;
    private final FishingService fishingService;

    public RodExchangeGUI(SkytreePlugin plugin, FishingService fishingService) {
        this.plugin = plugin;
        this.fishingService = fishingService;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
                ComponentUtil.smartParse("§8Rod Exchange (Drop 10 Limited Fish)"));
        player.openInventory(inv);
        // Register this instance as listener specifically?
        // Better to register a global listener for this GUI type, or single-use
        // listener.
        // For simplicity, let's assume global registration in plugin, but this needs to
        // distinguish the inventory.
        // I will register THIS instance and unregister on close.
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!ComponentUtil.toLegacy(event.getView().title()).contains("Rod Exchange"))
            return;
        if (!(event.getPlayer() instanceof Player))
            return;

        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        // Count Limited Fish
        List<ItemStack> limitedFish = new ArrayList<>();
        List<ItemStack> others = new ArrayList<>();

        for (ItemStack item : inv.getContents()) {
            if (item == null)
                continue;

            String rarity = NbtUtils.getString(item, NbtUtils.KEY_FISH_RARITY);
            if (rarity != null && rarity.equals(Rarity.LIMITED.name())) {
                limitedFish.add(item);
            } else {
                others.add(item);
            }
        }

        // Count total amount (stacks)
        int limitedCount = limitedFish.stream().mapToInt(ItemStack::getAmount).sum();

        if (limitedCount >= 10) {
            // Success!
            // Consume 10

            // Give Rod
            ItemStack rod = fishingService.createPoseidonRod();
            player.getInventory().addItem(rod);
            player.sendMessage(ComponentUtil.smartParse("§6§lCONGRATULATIONS! §aYou received Poseidon's Rod!"));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

            // Return excess
            int excess = limitedCount - 10;
            if (excess > 0) {
                // Return excess limited fish
                int toReturn = excess;
                for (ItemStack is : limitedFish) {
                    if (toReturn <= 0)
                        break;
                    int amount = is.getAmount();
                    int take = Math.min(amount, toReturn);

                    ItemStack ret = is.clone();
                    ret.setAmount(take);
                    player.getInventory().addItem(ret); // Or drop if full
                    toReturn -= take;
                }
            }

            // Return others
            for (ItemStack is : others) {
                player.getInventory().addItem(is);
            }

        } else {
            // Fail
            player.sendMessage(
                    ComponentUtil.smartParse("§cExchange failed: You need 10 Limited Rarity Fish. Returning items..."));
            for (ItemStack item : inv.getContents()) {
                if (item != null)
                    player.getInventory().addItem(item);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }

        // Unregister self
        org.bukkit.event.HandlerList.unregisterAll(this);
    }
}
