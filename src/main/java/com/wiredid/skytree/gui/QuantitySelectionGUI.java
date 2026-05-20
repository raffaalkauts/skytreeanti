package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Quantity Selection GUI for buying and selling in shop
 * Replaces the old bulk buy/sell system
 */
public class QuantitySelectionGUI {

    private final EconomyService economy;
    private final ItemRegistry itemRegistry;

    private final org.bukkit.NamespacedKey itemIdKey;
    private final org.bukkit.NamespacedKey priceKey;
    private final org.bukkit.NamespacedKey quantityKey;

    // Mode enum
    public enum Mode {
        BUY, SELL
    }

    public QuantitySelectionGUI(SkytreePlugin plugin, EconomyService economy, ItemRegistry itemRegistry) {
        this.economy = economy;
        this.itemRegistry = itemRegistry;
        this.itemIdKey = new org.bukkit.NamespacedKey(plugin, "shop_item_id");
        this.priceKey = new org.bukkit.NamespacedKey(plugin, "shop_price");
        this.quantityKey = new org.bukkit.NamespacedKey(plugin, "shop_quantity");
    }

    /**
     * Open quantity selection GUI
     * 
     * @param player     Player opening GUI
     * @param shopItem   The item from shop
     * @param price      Price per item (buy price for BUY mode, sell price for SELL
     *                   mode)
     * @param mode       BUY or SELL mode
     * @param shopItemId Item ID for custom items (can be null for vanilla)
     */
    public void open(Player player, ItemStack shopItem, double price, Mode mode, String shopItemId) {
        int quantity = 1; // Default quantity

        Inventory gui = Bukkit.createInventory(null, 27,
                ComponentUtil.parse(mode == Mode.BUY ? "§6§lCart §8» §7Quantity" : "§c§lCheckout §8» §7Quantity"));

        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.GRAY_STAINED_GLASS_PANE,
                Material.LIGHT_GRAY_STAINED_GLASS_PANE);

        updateGUI(gui, player, shopItem, price, quantity, mode, shopItemId);
        player.openInventory(gui);
    }

    public void updateGUI(Inventory gui, Player player, ItemStack shopItem, double price, int quantity, Mode mode,
            String shopItemId) {
        gui.clear();

        // Item preview with current quantity
        ItemStack preview = shopItem.clone();
        preview.setAmount(Math.min(quantity, 64));
        ItemMeta previewMeta = preview.getItemMeta();

        // Visual Fix: Show quantity in name if > 64
        if (quantity > 64) {
            String currentName = previewMeta.hasDisplayName() ? ComponentUtil.toLegacy(previewMeta.displayName())
                    : null;
            if (currentName == null) {
                currentName = convertToTitleCase(preview.getType().name().replace("_", " "));
            }
            previewMeta.displayName(ComponentUtil.parse("§e" + quantity + "x " + currentName));
        }
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Quantity: §e" + quantity);
        lore.add("§7Price per item: §e" + NumberUtil.formatCurrency(price));
        lore.add("§7Total " + (mode == Mode.BUY ? "cost" : "value") + ": §e"
                + NumberUtil.formatCurrency(price * quantity));
        lore.add("");
        if (mode == Mode.BUY) {
            lore.add("§7Your balance: §e" + NumberUtil.formatCurrency(economy.getBalance(player.getUniqueId())));
        } else {
            int available = countAvailableItems(player, shopItem, shopItemId);
            lore.add("§7Available: §e" + available);
        }
        previewMeta.lore(ComponentUtil.parseList(lore));

        // Store transaction data in PDC
        org.bukkit.persistence.PersistentDataContainer data = previewMeta.getPersistentDataContainer();
        if (shopItemId != null) {
            data.set(itemIdKey, org.bukkit.persistence.PersistentDataType.STRING, shopItemId);
        }
        data.set(priceKey, org.bukkit.persistence.PersistentDataType.DOUBLE, price);
        data.set(quantityKey, org.bukkit.persistence.PersistentDataType.INTEGER, quantity);

        preview.setItemMeta(previewMeta);
        gui.setItem(4, preview);

        // Quantity adjustment buttons
        gui.setItem(10, createButton(Material.RED_STAINED_GLASS_PANE, "§c§l-64", "§7Decrease by 64"));
        gui.setItem(11, createButton(Material.ORANGE_STAINED_GLASS_PANE, "§6§l-16", "§7Decrease by 16"));
        gui.setItem(12, createButton(Material.YELLOW_STAINED_GLASS_PANE, "§e§l-1", "§7Decrease by 1"));

        gui.setItem(13, createButton(Material.PAPER, "§b§lCurrent: §f" + quantity, "§7Click Confirm to finish"));

        gui.setItem(14, createButton(Material.YELLOW_STAINED_GLASS_PANE, "§e§l+1", "§7Increase by 1"));
        gui.setItem(15, createButton(Material.ORANGE_STAINED_GLASS_PANE, "§6§l+16", "§7Increase by 16"));
        gui.setItem(16, createButton(Material.RED_STAINED_GLASS_PANE, "§c§l+64", "§7Increase by 64"));

        // Max button
        if (mode == Mode.BUY) {
            gui.setItem(19, createButton(Material.CYAN_CONCRETE, "§b§lMax (12 stacks)",
                    "§7Set to 12 stacks (768 items)"));
        } else {
            int available = countAvailableItems(player, shopItem, shopItemId);
            gui.setItem(19, createButton(Material.CYAN_CONCRETE, "§b§lMax Available",
                    "§7Set to " + available + " items"));
        }

        // Reset button
        gui.setItem(20, createButton(Material.BARRIER, "§f§lReset", "§7Set quantity to 1"));

        // Confirm button with full data
        ItemStack confirm = createButton(Material.LIME_CONCRETE, "§a§l[ CONFIRM ]",
                mode == Mode.BUY ? "§7Buy " + quantity + " items" : "§7Sell " + quantity + " items",
                "§7Total: §e" + NumberUtil.formatCurrency(price * quantity),
                "§7Click to complete transaction");
        ItemMeta confirmMeta = confirm.getItemMeta();
        org.bukkit.persistence.PersistentDataContainer confirmData = confirmMeta.getPersistentDataContainer();
        if (shopItemId != null) {
            confirmData.set(itemIdKey, org.bukkit.persistence.PersistentDataType.STRING, shopItemId);
        }
        confirmData.set(priceKey, org.bukkit.persistence.PersistentDataType.DOUBLE, price);
        confirmData.set(quantityKey, org.bukkit.persistence.PersistentDataType.INTEGER, quantity);
        confirm.setItemMeta(confirmMeta);

        gui.setItem(22, confirm);
        gui.setItem(23, createButton(Material.RED_WOOL, "§c§lCancel", "§7Return to shop"));

    }

    private ItemStack createButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }
        meta.lore(ComponentUtil.parseList(loreList));
        item.setItemMeta(meta);
        return item;
    }

    private int countAvailableItems(Player player, ItemStack shopItem, String shopItemId) {
        int count = 0;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem == null)
                continue;

            if (shopItemId != null) {
                // Custom item check
                if (shopItemId.equals(itemRegistry.getItemId(invItem))) {
                    count += invItem.getAmount();
                }
            } else {
                // Vanilla item check
                if (invItem.getType() == shopItem.getType() && !itemRegistry.isCustomItem(invItem)) {
                    count += invItem.getAmount();
                }
            }
        }
        return count;
    }

    private String convertToTitleCase(String text) {
        if (text == null || text.isEmpty())
            return text;
        StringBuilder converted = new StringBuilder();
        boolean convertNext = true;
        for (char ch : text.toCharArray()) {
            if (Character.isSpaceChar(ch)) {
                convertNext = true;
            } else if (convertNext) {
                ch = Character.toTitleCase(ch);
                convertNext = false;
            } else {
                ch = Character.toLowerCase(ch);
            }
            converted.append(ch);
        }
        return converted.toString();
    }
}
