package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A generic GUI for confirmation.
 */
public class ConfirmationGUI implements Listener {

    private final SkytreePlugin plugin;
    private final NamespacedKey ACTION_KEY;

    // Callback storage (Active pending confirmations per player)
    private static final java.util.Map<java.util.UUID, Consumer<Boolean>> pendingCallbacks = new java.util.HashMap<>();

    public ConfirmationGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.ACTION_KEY = new NamespacedKey(plugin, "confirm_action");
    }

    public void open(Player player, String title, ItemStack itemInfo, Consumer<Boolean> callback, String totalDisplay) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.smartParse(title));

        // Premium Border with "Confirmation" name
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        gMeta.displayName(ComponentUtil.smartParse("§7Confirmation"));
        glass.setItemMeta(gMeta);

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        fMeta.displayName(ComponentUtil.smartParse("§7Confirmation"));
        filler.setItemMeta(fMeta);

        for (int i = 0; i < 27; i++) {
            if (i % 2 == 0)
                gui.setItem(i, glass);
            else
                gui.setItem(i, filler);
        }

        // Store Callback
        pendingCallbacks.put(player.getUniqueId(), callback);

        // Info Item (Center)
        if (itemInfo != null) {
            ItemStack displayItem = itemInfo.clone();
            ItemMeta meta = displayItem.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            if (lore == null)
                lore = new ArrayList<>();

            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(ComponentUtil.smartParse(totalDisplay));
            lore.add(net.kyori.adventure.text.Component.empty());

            meta.lore(lore);
            displayItem.setItemMeta(meta);
            gui.setItem(13, displayItem);
        }

        // Cancel Button (Left)
        gui.setItem(11, createButton(Material.RED_STAINED_GLASS_PANE, "§c§lCANCEL", "CANCEL",
                "§7Click to cancel this transaction."));

        // Confirm Button (Right)
        gui.setItem(15, createButton(Material.LIME_STAINED_GLASS_PANE, "§a§lCONFIRM", "CONFIRM",
                "§7Click to confirm this transaction."));

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
    }

    public void open(Player player, String title, ItemStack itemInfo, Consumer<Boolean> callback) {
        open(player, title, itemInfo, callback, "§eTotal: ???");
    }

    private ItemStack createButton(Material mat, String name, String action, String desc) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        List<String> lore = new ArrayList<>();
        lore.add(desc);
        meta.lore(ComponentUtil.parseList(lore));
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        // Check title as a first-pass (mini-message compatible)
        String title = ComponentUtil.stripColor(event.getView().title());
        boolean hasConfirmTitle = title.contains("Confirm") || title.contains("Confirmation") || title.contains("Claim")
                || title.contains("Cancel");
        boolean hasCallback = pendingCallbacks.containsKey(player.getUniqueId());

        // We process if it's obviously a confirmation title OR if we are expecting a
        // callback in a 27-slot GUI
        if (!(hasConfirmTitle || hasCallback) || event.getView().getTopInventory().getSize() != 27)
            return;

        // Don't cancel when clicking in player's own inventory
        if (event.getClickedInventory() == event.getView().getBottomInventory())
            return;

        event.setCancelled(true);

        // Only process clicks in top inventory
        if (event.getClickedInventory() != event.getView().getTopInventory())
            return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        String action = clicked.getItemMeta().getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);

        if (action == null)
            return;

        if ("CONFIRM".equals(action) || "CANCEL".equals(action)) {
            // Remove callback immediately to prevent onClose from consuming it
            Consumer<Boolean> callback = pendingCallbacks.remove(player.getUniqueId());

            boolean confirmed = "CONFIRM".equals(action);

            // Play sound immediately
            if (confirmed) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            } else {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
            }

            // Close inventory immediately to provide feedback and prevent double clicks
            player.closeInventory();

            // Execute callback on NEXT TICK
            if (callback != null) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    callback.accept(confirmed);
                });
            }

            return;
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;
        Player player = (Player) event.getPlayer();

        // Only auto-cancel if callback still exists (wasn't processed by click)
        if (pendingCallbacks.containsKey(player.getUniqueId())) {
            // Verify this is a Confirmation GUI closing
            String title = ComponentUtil.stripColor(event.getView().title());
            if (title.contains("Confirm") || title.contains("Confirmation") || title.contains("Claim")
                    || title.contains("Cancel")) {
                Consumer<Boolean> callback = pendingCallbacks.remove(player.getUniqueId());
                if (callback != null) {
                    plugin.getLogger().info("[DEBUG] Confirmation GUI closed - auto-cancelling");
                    callback.accept(false);
                }
            }
        }
    }
}
