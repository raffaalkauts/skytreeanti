package com.wiredid.skytree.fishing.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.PersistenceService;
import com.wiredid.skytree.fishing.NbtUtils;
import com.wiredid.skytree.fishing.RodStorage;
import com.wiredid.skytree.model.PlayerData;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ActiveRodGUI implements Listener {

    private final SkytreePlugin plugin;
    private final PersistenceService persistence;
    private final RodStorage rodStorage;
    private final Inventory inventory;
    private final Map<Integer, UUID> slotToRodId = new HashMap<>();

    public ActiveRodGUI(SkytreePlugin plugin, RodStorage rodStorage) {
        this.plugin = plugin;
        this.rodStorage = rodStorage;
        this.persistence = plugin.getPersistenceService();
        if (this.persistence == null) {
            throw new IllegalStateException("PersistenceService is not initialized!");
        }
        this.inventory = Bukkit.createInventory(null, 27, Component.text("Rod Storage - Hold rod to swap"));
    }

    public void open(Player player) {
        inventory.clear();
        slotToRodId.clear();

        PlayerData data = persistence.loadPlayerData(player.getUniqueId());
        UUID currentActive = data.getActiveRodId();

        // Get stored rods from RodStorage
        List<ItemStack> storedRods = rodStorage.getRods(player.getUniqueId());
        int slot = 0;

        for (ItemStack rod : storedRods) {
            if (slot >= 27)
                break;

            ItemStack displayItem = rod.clone();
            ItemMeta meta = displayItem.getItemMeta();
            UUID rodId = NbtUtils.getRodId(rod);

            if (rodId != null && rodId.equals(currentActive)) {
                meta.displayName(Component.text("§a[ACTIVE] ")
                        .append(meta.displayName() != null ? meta.displayName() : Component.text("Rod")));
                displayItem.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
                displayItem.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.displayName(Component.text("§e")
                        .append(meta.displayName() != null ? meta.displayName() : Component.text("Rod")));
            }

            displayItem.setItemMeta(meta);

            inventory.setItem(slot, displayItem);
            slotToRodId.put(slot, rodId);
            slot++;
        }

        // Add info item
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("§6§lHow to Use"));
        infoMeta.lore(List.of(
                Component.text("§7Click a rod to equip it"),
                Component.text("§7• No rod in hand: Take from storage"),
                Component.text("§7• Rod in hand: Swap with storage")));
        info.setItemMeta(infoMeta);
        inventory.setItem(26, info);

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

        int slot = event.getRawSlot();
        if (slotToRodId.containsKey(slot)) {
            UUID rodId = slotToRodId.get(slot);
            PlayerData data = persistence.loadPlayerData(player.getUniqueId());
            UUID currentActiveId = data.getActiveRodId();

            if (rodId.equals(currentActiveId)) {
                // UNEQUIP/DEACTIVATE
                data.setActiveRodId(null);
                removeVirtualRodFromInventory(player, rodId);
                player.sendMessage("§eRod unequipped!");
            } else {
                // EQUIP/ACTIVATE
                // 1. Remove any currently active virtual rod from inventory
                if (currentActiveId != null) {
                    removeVirtualRodFromInventory(player, currentActiveId);
                }

                // 2. Find the rod in storage
                ItemStack rodToGive = null;
                for (ItemStack s : rodStorage.getRods(player.getUniqueId())) {
                    if (rodId.equals(NbtUtils.getRodId(s))) {
                        rodToGive = s.clone();
                        break;
                    }
                }

                if (rodToGive != null) {
                    data.setActiveRodId(rodId);
                    player.getInventory().addItem(rodToGive);
                    player.sendMessage("§aRod activated and added to inventory!");
                } else {
                    player.sendMessage("§cRod not found in storage!");
                }
            }

            persistence.savePlayerData(data);
            open(player); // Refresh GUI
        } else if (slot == 26) {
            // Info item, maybe show help?
        } else if (event.getClickedInventory() == player.getInventory()) {
            // If player clicks a rod in their inventory while GUI is open, maybe add it to
            // storage?
            ItemStack heldItem = event.getCurrentItem();
            if (heldItem != null && heldItem.getType() == Material.FISHING_ROD) {
                rodStorage.addRod(player.getUniqueId(), heldItem.clone());
                heldItem.setAmount(0);
                player.sendMessage("§aRod added to virtual storage! You can now toggle it here.");
                open(player); // Refresh
            }
        }
    }

    private void removeVirtualRodFromInventory(Player player, UUID rodId) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.FISHING_ROD) {
                if (rodId.equals(NbtUtils.getRodId(item))) {
                    player.getInventory().setItem(i, null);
                }
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
