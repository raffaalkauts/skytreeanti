package com.wiredid.skytree.fishing.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.fishing.EnchantService;
import com.wiredid.skytree.fishing.NbtUtils;
import com.wiredid.skytree.util.NumberUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class EnchantGUI implements Listener {

    private final SkytreePlugin plugin;
    private final EnchantService enchantService;
    private final Inventory inventory;

    public EnchantGUI(SkytreePlugin plugin, EnchantService enchantService) {
        this.plugin = plugin;
        this.enchantService = enchantService;
        this.inventory = Bukkit.createInventory(null, 27, Component.text("Rod Enchant Gacha"));
    }

    public void open(Player player) {
        inventory.clear();

        // Show Held Rod (Must be custom)
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.FISHING_ROD && NbtUtils.isCustomRod(held)) {

            ItemStack display = held.clone();
            inventory.setItem(13, display);

            if (enchantService.canEnchant(held)) {
                ItemStack btn = new ItemStack(Material.ENCHANTING_TABLE);
                ItemMeta meta = btn.getItemMeta();
                meta.displayName(Component.text("§dRoll Enchant"));
                double cost = enchantService.getEnchantCost(held);
                meta.lore(
                        Collections.singletonList(Component.text("§7Cost: §e" + NumberUtil.formatCurrency(cost))));
                btn.setItemMeta(meta);
                inventory.setItem(15, btn);
            } else {
                ItemStack full = new ItemStack(Material.BARRIER);
                ItemMeta meta = full.getItemMeta();
                meta.displayName(Component.text("§cSlots Full"));
                full.setItemMeta(meta);
                inventory.setItem(15, full);
            }

        } else {
            ItemStack warn = new ItemStack(Material.BARRIER);
            ItemMeta meta = warn.getItemMeta();
            meta.displayName(Component.text("§cHold a Custom Rod!"));
            warn.setItemMeta(meta);
            inventory.setItem(13, warn);
        }

        player.openInventory(inventory);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player))
            return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null)
            return;

        if (clicked.getType() == Material.ENCHANTING_TABLE) {
            ItemStack held = player.getInventory().getItemInMainHand();
            // Re-validate
            if (held.getType() == Material.FISHING_ROD && NbtUtils.isCustomRod(held)) {
                enchantService.rollEnchant(player, held);
                open(player); // Refresh
            } else {
                player.closeInventory();
                player.sendMessage("§cYou must hold the rod!");
            }
        }
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            org.bukkit.event.HandlerList.unregisterAll(this);
        }
    }
}
