package com.wiredid.skytree.fishing.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.NumberUtil;
import com.wiredid.skytree.fishing.FishingModels;
import com.wiredid.skytree.fishing.FishingService;
import com.wiredid.skytree.fishing.RodStorage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import com.wiredid.skytree.util.ComponentUtil;

public class FishShopGUI implements Listener {

    private final FishingService fishingService;
    private final RodStorage rodStorage;
    private final com.wiredid.skytree.api.EconomyService economy;
    private final Inventory inventory;

    public FishShopGUI(SkytreePlugin plugin, FishingService fishingService, RodStorage rodStorage,
            com.wiredid.skytree.api.EconomyService economy) {
        this.fishingService = fishingService;
        this.rodStorage = rodStorage;
        this.economy = economy;
        this.inventory = Bukkit.createInventory(null, 27, Component.text("Fishing Shop"));
    }

    public void open(Player player) {
        inventory.clear();

        // Add Rods
        addShopItem(10, FishingModels.RodTier.BASIC, 10000);
        addShopItem(12, FishingModels.RodTier.ADVANCED, 100000);
        addShopItem(14, FishingModels.RodTier.MYTHIC, 1000000);
        addShopItem(16, FishingModels.RodTier.RELIC, 10000000);

        player.openInventory(inventory);
    }

    private void addShopItem(int slot, FishingModels.RodTier tier, double price) {
        ItemStack item = fishingService.createRod(tier); // Visual only? No, we use it as icon
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null)
            lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(ComponentUtil.smartParse("&fPrice: &e" + NumberUtil.formatCurrency(price)));
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player))
            return;

        int slot = event.getRawSlot();
        double price = 0;
        FishingModels.RodTier tier = null;

        // mapping
        if (slot == 10) {
            tier = FishingModels.RodTier.BASIC;
            price = 10000;
        } else if (slot == 12) {
            tier = FishingModels.RodTier.ADVANCED;
            price = 100000;
        } else if (slot == 14) {
            tier = FishingModels.RodTier.MYTHIC;
            price = 1000000;
        } else if (slot == 16) {
            tier = FishingModels.RodTier.RELIC;
            price = 10000000;
        }

        if (tier != null) {
            // Check Economy
            if (economy.getBalance(player.getUniqueId()) >= price) {
                economy.removeBalance(player.getUniqueId(), price);
                ItemStack rod = fishingService.createRod(tier);
                // Add to storage instead of inventory
                rodStorage.addRod(player.getUniqueId(), rod);
                player.sendMessage("§aPurchased " + tier.getDisplayName() + "! Use /rod to equip it.");
            } else {
                player.sendMessage("§cNot enough USDT!");
            }
        }
    }
}
