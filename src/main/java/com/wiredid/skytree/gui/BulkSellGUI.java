package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.api.ItemRegistry;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

/**
 * Bulk Sell GUI - Allows selling 1, 16, 32, 64, ALL items at once
 */
public class BulkSellGUI implements Listener {

    private final EconomyService economy;
    private final ItemRegistry itemRegistry;
    private final NamespacedKey priceKey;
    private final NamespacedKey itemKey;

    public BulkSellGUI(SkytreePlugin plugin, EconomyService economy, ItemRegistry itemRegistry) {

        this.economy = economy;
        this.itemRegistry = itemRegistry;
        this.priceKey = new NamespacedKey(plugin, "sell_price");
        this.itemKey = new NamespacedKey(plugin, "sell_item");
    }

    public void open(Player player, ItemStack shopItem, double unitPrice) {
        Inventory gui = Bukkit.createInventory(null, 27,
                ComponentUtil.parse("§c§lBulk Sell: " + getItemName(shopItem)));

        // Background
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, bg);
        }

        // Display Item
        gui.setItem(13, shopItem.clone());

        // Count how many player has
        int totalOwned = countPlayerItems(player, shopItem);

        // Sell Options
        if (totalOwned >= 1) {
            gui.setItem(10, createOption(Material.RED_DYE, 1, unitPrice, shopItem, totalOwned));
        }
        if (totalOwned >= 16) {
            gui.setItem(11, createOption(Material.RED_DYE, 16, unitPrice, shopItem, totalOwned));
        }
        if (totalOwned >= 32) {
            gui.setItem(12, createOption(Material.RED_DYE, 32, unitPrice, shopItem, totalOwned));
        }
        if (totalOwned >= 64) {
            gui.setItem(14, createOption(Material.RED_DYE, 64, unitPrice, shopItem, totalOwned));
        }
        if (totalOwned > 64) {
            gui.setItem(15, createOption(Material.REDSTONE_BLOCK, totalOwned, unitPrice, shopItem, totalOwned));
        }

        // Info
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(ComponentUtil.parse("§e§lYou Own: §f" + totalOwned));
        infoMeta.lore(ComponentUtil.parseList(
                "§7Select amount to sell below",
                "§7Unit Price: §a" + NumberUtil.formatCurrency(unitPrice)));
        info.setItemMeta(infoMeta);
        gui.setItem(4, info);

        gui.setItem(26, createItem(Material.ARROW, "§cCancel"));

        player.openInventory(gui);
    }

    private int countPlayerItems(Player player, ItemStack shopItem) {
        int count = 0;
        String shopItemId = itemRegistry.getItemId(shopItem);

        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem == null)
                continue;

            boolean matches;
            if (shopItemId != null) {
                // Custom item
                matches = shopItemId.equals(itemRegistry.getItemId(invItem));
            } else {
                // Vanilla item
                matches = invItem.getType() == shopItem.getType() && !itemRegistry.isCustomItem(invItem);
            }

            if (matches) {
                count += invItem.getAmount();
            }
        }
        return count;
    }

    private ItemStack createOption(Material mat, int amount, double unitPrice, ItemStack shopItem, int maxOwned) {
        ItemStack item = new ItemStack(mat, Math.min(amount, 64));
        ItemMeta meta = item.getItemMeta();
        double totalPrice = unitPrice * amount;

        String displayAmount = (amount == maxOwned) ? "ALL (" + amount + "x)" : amount + "x";

        meta.displayName(ComponentUtil.parse("§cSell " + displayAmount));
        meta.lore(ComponentUtil.parseList(
                "§7Unit Price: §a" + NumberUtil.formatCurrency(unitPrice),
                "",
                "§7Total Earnings: §a" + NumberUtil.formatCurrency(totalPrice),
                "",
                "§eClick to Sell"));

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(priceKey, PersistentDataType.DOUBLE, totalPrice);
        data.set(itemKey, PersistentDataType.STRING, String.valueOf(amount));

        item.setItemMeta(meta);
        return item;
    }

    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return ComponentUtil.toLegacy(item.getItemMeta().displayName());
        }
        return item.getType().name().replace("_", " ");
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ComponentUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.startsWith("§c§lBulk Sell:")) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        if (clicked.getType() == Material.ARROW) {
            player.closeInventory();
            return;
        }

        PersistentDataContainer data = clicked.getItemMeta().getPersistentDataContainer();
        if (!data.has(priceKey, PersistentDataType.DOUBLE))
            return;

        double totalPrice = data.get(priceKey, PersistentDataType.DOUBLE);
        int amountToSell = Integer.parseInt(data.get(itemKey, PersistentDataType.STRING));

        // Get the shop item from display (slot 13)
        ItemStack shopItem = event.getInventory().getItem(13);
        if (shopItem == null)
            return;

        // Remove items from player inventory
        int removed = removePlayerItems(player, shopItem, amountToSell);

        if (removed > 0) {
            // Calculate actual earnings
            double actualEarnings = (totalPrice / amountToSell) * removed;
            economy.addBalance(player.getUniqueId(), actualEarnings);

            player.sendMessage("§aSold " + removed + "x items for §e" + NumberUtil.formatCurrency(actualEarnings));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            player.closeInventory();
        } else {
            player.sendMessage("§cYou don't have any of this item!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
        }
    }

    private int removePlayerItems(Player player, ItemStack shopItem, int targetAmount) {
        int removed = 0;
        String shopItemId = itemRegistry.getItemId(shopItem);

        for (int i = 0; i < player.getInventory().getSize() && removed < targetAmount; i++) {
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem == null)
                continue;

            boolean matches;
            if (shopItemId != null) {
                matches = shopItemId.equals(itemRegistry.getItemId(invItem));
            } else {
                matches = invItem.getType() == shopItem.getType() && !itemRegistry.isCustomItem(invItem);
            }

            if (matches) {
                int toRemove = Math.min(invItem.getAmount(), targetAmount - removed);
                invItem.setAmount(invItem.getAmount() - toRemove);
                if (invItem.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
                removed += toRemove;
            }
        }

        return removed;
    }
}
