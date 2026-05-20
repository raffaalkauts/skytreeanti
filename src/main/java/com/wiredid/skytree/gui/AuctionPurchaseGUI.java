package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.AuctionHouse;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI for selecting quantity when purchasing a listing
 */
public class AuctionPurchaseGUI implements Listener {

    private final SkytreePlugin plugin;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey LISTING_ID_KEY;
    private final NamespacedKey AMOUNT_KEY;

    public AuctionPurchaseGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.ACTION_KEY = new NamespacedKey(plugin, "buy_action");
        this.LISTING_ID_KEY = new NamespacedKey(plugin, "buy_listing_id");
        this.AMOUNT_KEY = new NamespacedKey(plugin, "buy_amount");
    }

    public void open(Player player, AuctionHouse listing, int currentAmount) {
        Inventory inv = Bukkit.createInventory(null, 27, ComponentUtil.smartParse("§6§lMarket §8» §7Confirm Purchase"));

        // Fill background
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(inv, Material.GRAY_STAINED_GLASS_PANE,
                Material.LIGHT_GRAY_STAINED_GLASS_PANE);

        // Preview Item (13)
        ItemStack preview = listing.getItem().clone();
        preview.setAmount(currentAmount);
        ItemMeta meta = preview.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        lore.add(ComponentUtil.parse(" "));
        lore.add(ComponentUtil.parse("§7Selected Amount: §e" + currentAmount));
        lore.add(ComponentUtil
                .parse("§7Total Price: §a$" + NumberUtil.formatCurrency(listing.getPricePerItem() * currentAmount)));
        lore.add(ComponentUtil.parse(" "));
        lore.add(ComponentUtil.parse("§eClick CONFIRM to buy!"));
        meta.lore(lore);
        preview.setItemMeta(meta);
        inv.setItem(13, preview);

        // Adjustment Buttons
        inv.setItem(10,
                createAdjustButton(Material.RED_STAINED_GLASS_PANE, "§c-10", -10, listing.getId(), currentAmount));
        inv.setItem(11,
                createAdjustButton(Material.ORANGE_STAINED_GLASS_PANE, "§c-1", -1, listing.getId(), currentAmount));

        inv.setItem(15,
                createAdjustButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§a+1", 1, listing.getId(), currentAmount));
        inv.setItem(16,
                createAdjustButton(Material.CYAN_STAINED_GLASS_PANE, "§a+10", 10, listing.getId(), currentAmount));

        // Confirm/Cancel
        inv.setItem(21, createActionButton(Material.LIME_STAINED_GLASS_PANE, "§a§lCONFIRM", "CONFIRM", listing.getId(),
                currentAmount));
        inv.setItem(23, createActionButton(Material.BARRIER, "§c§lCANCEL", "CANCEL", listing.getId(), currentAmount));

        player.openInventory(inv);
    }

    private ItemStack createAdjustButton(Material mat, String name, int delta, UUID listingId, int current) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "ADJUST");
        meta.getPersistentDataContainer().set(LISTING_ID_KEY, PersistentDataType.STRING, listingId.toString());
        meta.getPersistentDataContainer().set(AMOUNT_KEY, PersistentDataType.INTEGER, current + delta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionButton(Material mat, String name, String action, UUID listingId, int current) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(LISTING_ID_KEY, PersistentDataType.STRING, listingId.toString());
        meta.getPersistentDataContainer().set(AMOUNT_KEY, PersistentDataType.INTEGER, current);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.stripColor(event.getView().title());
        if (!title.contains("Confirm Purchase"))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
        if (action == null)
            return;

        String idStr = meta.getPersistentDataContainer().get(LISTING_ID_KEY, PersistentDataType.STRING);
        if (idStr == null)
            return;
        UUID id = UUID.fromString(idStr);
        int amount = meta.getPersistentDataContainer().getOrDefault(AMOUNT_KEY, PersistentDataType.INTEGER, 1);

        AuctionHouse listing = plugin.getAuctionHouseService().getActiveListings().stream()
                .filter(l -> l.getId().equals(id)).findFirst().orElse(null);

        if (listing == null) {
            player.sendMessage("§cThis listing is no longer available.");
            player.closeInventory();
            return;
        }

        switch (action) {
            case "ADJUST":
                int newAmount = Math.max(1, Math.min(amount, listing.getQuantityRemaining()));
                open(player, listing, newAmount);
                break;
            case "CONFIRM":
                if (plugin.getAuctionHouseService().purchaseListing(player, listing, amount)) {
                    player.sendMessage("§aPurchase successful!");
                    plugin.getAuctionHouseGUI().open(player, AuctionHouseGUI.ViewType.LISTINGS, 0,
                            AuctionHouseGUI.SortType.NEWEST, null);
                }
                break;
            case "CANCEL":
                plugin.getAuctionHouseGUI().open(player, AuctionHouseGUI.ViewType.LISTINGS, 0,
                        AuctionHouseGUI.SortType.NEWEST, null);
                break;
        }
    }
}
