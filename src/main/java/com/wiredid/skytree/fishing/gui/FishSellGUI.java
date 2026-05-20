package com.wiredid.skytree.fishing.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.fishing.FishingModels;
import com.wiredid.skytree.fishing.FishingService;
import com.wiredid.skytree.fishing.NbtUtils;
import com.wiredid.skytree.util.NumberUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

import java.util.List;

public class FishSellGUI implements Listener {

    private final EconomyService economy;
    private final Inventory inventory;

    public FishSellGUI(SkytreePlugin plugin, FishingService fishingService, EconomyService economy) {
        this.economy = economy;
        this.inventory = Bukkit.createInventory(null, 54, Component.text("Fish Market - Drop items to sell"));
    }

    public void open(Player player) {
        // Setup GUI: 45 slots for items, bottom row for controls
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" "));
        filler.setItemMeta(meta);

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        ItemStack sellBtn = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta sellMeta = sellBtn.getItemMeta();
        sellMeta.displayName(Component.text("§aSELL ALL"));
        sellBtn.setItemMeta(sellMeta);

        inventory.setItem(49, sellBtn);

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory))
            return;

        // Allow adding items to top part (slots 0-44)
        if (event.getRawSlot() < 45) {
            // Allow normal inventory interactions for fish placement
            return;
        }

        // Cancel clicks on control area (slots 45-53)
        event.setCancelled(true);

        if (event.getRawSlot() == 49) {
            // Sell Action
            sellAll((Player) event.getWhoClicked());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory))
            return;

        // Return unsold items
        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                event.getPlayer().getInventory().addItem(item);
            }
        }
    }

    private void sellAll(Player player) {
        double total = 0;
        List<ItemStack> toRemove = new ArrayList<>();

        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR)
                continue;

            if (NbtUtils.isCustomFish(item)) {
                double price = NbtUtils.getDouble(item, NbtUtils.KEY_FISH_PRICE);
                if (price <= 0) {
                    // Fallback if price not saved
                    double weight = NbtUtils.getDouble(item, NbtUtils.KEY_FISH_WEIGHT);
                    String mutStr = NbtUtils.getString(item, NbtUtils.KEY_FISH_MUTATION);
                    FishingModels.Mutation mutation = FishingModels.Mutation.valueOf(mutStr);
                    price = weight * mutation.getPriceMultiplier() * 1.5;
                }

                total += price;
                toRemove.add(item);
            } else {
                // Return non-fish
                player.getInventory().addItem(item);
            }
        }

        // Execute Transaction
        if (total > 0) {
            economy.addBalance(player.getUniqueId(), total);
            player.sendMessage("§aSold " + toRemove.size() + " fish for " + NumberUtil.formatCurrency(total));

            // Clear Grid
            for (int i = 0; i < 45; i++) {
                inventory.setItem(i, null);
            }
        } else {
            player.sendMessage("§cNo valid fish to sell.");
        }

        player.closeInventory();
    }
}
