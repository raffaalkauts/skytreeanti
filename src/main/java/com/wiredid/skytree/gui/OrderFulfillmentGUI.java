package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.AuctionOrder;
import com.wiredid.skytree.api.AuctionHouseService;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Refactored OrderFulfillmentGUI.
 * Uses a button-based system (like BulkSellGUI) to prevent item loss bugs.
 */
public class OrderFulfillmentGUI implements Listener {

    private final SkytreePlugin plugin;
    private final AuctionHouseService service;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey AMOUNT_KEY;
    private final NamespacedKey ORDER_ID_KEY;

    public OrderFulfillmentGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.service = plugin.getAuctionHouseService();
        this.ACTION_KEY = new NamespacedKey(plugin, "fulfill_action");
        this.AMOUNT_KEY = new NamespacedKey(plugin, "fulfill_amount");
        this.ORDER_ID_KEY = new NamespacedKey(plugin, "fulfill_order_id");
    }

    public void open(Player player, AuctionOrder order) {
        Inventory inv = Bukkit.createInventory(null, 36, ComponentUtil.smartParse("§b§lBounty §8» §fShipment"));

        // Merchant Theme Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                Material.WHITE_STAINED_GLASS_PANE);

        // Center Info Icon (Slot 13)
        ItemStack info = order.getRequestedItem().clone();
        info.setAmount(1);
        ItemMeta meta = info.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.parse("§7Buyer: §f" + Bukkit.getOfflinePlayer(order.getBuyer()).getName()));
        lore.add(ComponentUtil.parse("§7Price/Item: §b" + NumberUtil.formatCurrency(order.getPricePerItem())));
        lore.add(ComponentUtil.parse("§7Remaining Demand: §e" + order.getRemainingQuantity()));
        lore.add(ComponentUtil.parse(" "));
        lore.add(ComponentUtil.parse("§fChoose an amount from your inventory"));
        lore.add(ComponentUtil.parse("§fto fulfill this bounty."));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(ORDER_ID_KEY, PersistentDataType.STRING, order.getId().toString());
        info.setItemMeta(meta);
        inv.setItem(13, info);

        // Count items in inventory
        int totalOwned = countMatchingItems(player, order);

        // Action Buttons (Slots 19, 20, 21, 23, 24, 25)
        int[] amounts = { 1, 16, 32, 64 };
        int[] slots = { 19, 20, 21, 23 };

        for (int i = 0; i < amounts.length; i++) {
            int amt = amounts[i];
            if (totalOwned >= amt) {
                inv.setItem(slots[i], createActionButton(Material.BLUE_DYE, "§bSell " + amt + "x", amt, order));
            } else {
                inv.setItem(slots[i],
                        createLockedButton(Material.GRAY_DYE, "§7Sell " + amt + "x", "§cNot enough items"));
            }
        }

        // Sell All Button
        int sellAllAmt = Math.min(totalOwned, order.getRemainingQuantity());
        if (sellAllAmt > 0) {
            inv.setItem(24,
                    createActionButton(Material.LAPIS_LAZULI, "§3Sell ALL (" + sellAllAmt + "x)", sellAllAmt, order));
        } else {
            inv.setItem(24, createLockedButton(Material.GRAY_DYE, "§7Sell ALL", "§cInventory empty"));
        }

        // Close Button
        inv.setItem(31, createSimpleButton(Material.BARRIER, "§c§lClose", "CLOSE"));

        player.openInventory(inv);
    }

    private ItemStack createActionButton(Material mat, String name, int amount, AuctionOrder order) {
        ItemStack item = new ItemStack(mat, Math.min(amount, 64));
        ItemMeta meta = item.getItemMeta();
        double totalPayout = amount * order.getPricePerItem();

        meta.displayName(ComponentUtil.parse(name));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.parse("§7Total Payout: §a" + NumberUtil.formatCurrency(totalPayout)));
        lore.add(ComponentUtil.parse(" "));
        lore.add(ComponentUtil.parse("§eClick to confirm shipment"));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "FULFILL");
        meta.getPersistentDataContainer().set(AMOUNT_KEY, PersistentDataType.INTEGER, amount);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLockedButton(Material mat, String name, String reason) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.lore(List.of(ComponentUtil.parse(reason)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSimpleButton(Material mat, String name, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private int countMatchingItems(Player player, AuctionOrder order) {
        int count = 0;
        ItemStack template = order.getRequestedItem();
        com.wiredid.skytree.api.WorthService worthSvc = plugin.getWorthService();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir())
                continue;

            boolean matches = order.isStrictMatch()
                    ? worthSvc.isSimilarIgnoringWorth(item, template)
                    : item.getType() == template.getType();

            if (matches) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Bounty") || !title.contains("Shipment"))
            return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        String action = clicked.getItemMeta().getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
        if (action == null)
            return;

        if (action.equals("CLOSE")) {
            player.closeInventory();
            return;
        }

        if (action.equals("FULFILL")) {
            Integer amount = clicked.getItemMeta().getPersistentDataContainer().get(AMOUNT_KEY,
                    PersistentDataType.INTEGER);
            if (amount == null)
                return;

            ItemStack info = event.getInventory().getItem(13);
            if (info == null)
                return;

            String orderIdStr = info.getItemMeta().getPersistentDataContainer().get(ORDER_ID_KEY,
                    PersistentDataType.STRING);
            if (orderIdStr == null)
                return;

            AuctionOrder order = service.getOrder(UUID.fromString(orderIdStr));
            if (order == null || order.isExpired()) {
                player.sendMessage("§cThis bounty has expired.");
                player.closeInventory();
                return;
            }

            // Secondary Check: Do they still have items? (Prevent race condition/GUIs sync)
            int currentOwned = countMatchingItems(player, order);
            if (currentOwned < amount) {
                player.sendMessage("§cYou no longer have enough items!");
                open(player, order);
                return;
            }

            // Confirmation Step
            double payout = amount * order.getPricePerItem();
            ItemStack confirmIcon = order.getRequestedItem().clone();
            confirmIcon.setAmount(1);
            ItemMeta meta = confirmIcon.getItemMeta();
            meta.lore(List.of(
                    ComponentUtil.parse("§7Amount: §f" + amount + "x"),
                    ComponentUtil.parse("§7Total Payout: §a" + NumberUtil.formatCurrency(payout)),
                    ComponentUtil.parse(" "),
                    ComponentUtil.parse("§eClick to complete transaction")));
            confirmIcon.setItemMeta(meta);

            plugin.getConfirmationGUI().open(player, "Confirm Shipment?", confirmIcon, confirmed -> {
                if (confirmed) {
                    ItemStack toTake = order.getRequestedItem().clone();
                    toTake.setAmount(amount);
                    if (service.fulfillOrder(player, order, toTake)) {
                        player.sendMessage("§a§l[Market] §aSuccessfully fulfilled the bounty!");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    } else {
                        player.sendMessage("§c§l[Market] §cFailed to fulfill. Check your inventory.");
                    }
                } else {
                    open(player, order);
                }
            });
        }
    }
}
