package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.util.ComponentUtil;
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

/**
 * Handles the Bulk Sell GUI interactions.
 */
public class SellGUIListener implements Listener {

    private final SkytreePlugin plugin;
    private final EconomyService economy;
    private final com.wiredid.skytree.api.WorthService worthService;

    public static final String GUI_TITLE = "§2§lBulk Sell (Place Items)";
    private static final int CONFIRM_SLOT = 53;
    private static final int GUIDE_SLOT = 45;

    public SellGUIListener(SkytreePlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.worthService = plugin.getWorthService();
    }

    public void openSellGUI(Player player) {
        // 54 Slots (Double Chest)
        Inventory gui = Bukkit.createInventory(null, 54, ComponentUtil.parse(GUI_TITLE));

        // Add "Confirm Sell" button in last slot
        gui.setItem(CONFIRM_SLOT, createConfirmButton(0));
        gui.setItem(GUIDE_SLOT, createGuideButton());

        // Fill bottom row with glass except confirm button?
        // No, let users fill everything except connection slots.
        // Actually, typical implementation is full chest space, then click button.
        // Let's reserve the last row for controls.
        for (int i = 45; i < 54; i++) {
            if (i != CONFIRM_SLOT) {
                gui.setItem(i, createPane());
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.equals(GUI_TITLE))
            return;

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        // Handle clicks in the top inventory (GUI)
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            int slot = event.getSlot();

            // Bottom row handling (control area)
            if (slot >= 45) {
                if (slot == CONFIRM_SLOT) {
                    event.setCancelled(true);
                    processSell(player, event.getView().getTopInventory());
                } else if (slot == GUIDE_SLOT) {
                    event.setCancelled(true);
                    plugin.getFishPriceGuideGUI().open(player);
                } else {
                    event.setCancelled(true); // Glass panes
                }
                return;
            }
            // Allow normal interactions for slots 0-44 (sellable area)
            return;
        }

        // Handle shift-click from player inventory - allow it
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.equals(GUI_TITLE))
            return;

        // Return unsullied items to player (if they closed without selling)
        // Or if we process on close?
        // User workflow: "I put items, I click sell". If I close, I expect items back.

        Inventory inv = event.getView().getTopInventory();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                event.getPlayer().getInventory().addItem(item).values().forEach(
                        leftover -> event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), leftover));
            }
        }
    }

    private void processSell(Player player, Inventory inv) {
        double totalValue = 0;
        int itemsSold = 0;
        List<ItemStack> unsold = new ArrayList<>();

        // Loop through sellable slots (0-44)
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR)
                continue;

            double price = getSellPrice(item);
            if (price > 0) {
                totalValue += price * item.getAmount();
                itemsSold += item.getAmount();
                inv.setItem(i, null); // Remove item
            } else {
                unsold.add(item); // Keep track to inform user? Or just leave them there?
                // Leaving them there is good visual feedback.
            }
        }

        if (totalValue > 0) {
            economy.addBalance(player.getUniqueId(), totalValue);
            player.sendMessage("§a§l[Shop] §aSold §f" + itemsSold + " §aitems for §e"
                    + com.wiredid.skytree.util.NumberUtil.formatCurrency(totalValue));
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

            // Re-open GUI? Or Close?
            // Usually simpler to close, or clear sold items and update button.
            // Let's close for now to be safe.
            player.closeInventory();
            // Note: onInventoryClose will handle returning unsold items!
        } else {
            player.sendMessage("§cNo sellable items found!");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
        }
    }

    public double getSellPrice(ItemStack item) {
        double base = worthService.getItemSellPrice(item);
        if (plugin.getEconomyManager() != null) {
            return base * plugin.getEconomyManager().getPriceMultiplier();
        }
        return base;
    }

    private ItemStack createConfirmButton(double value) {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§a§lCONFIRM SELL"));
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to sell all valid items");
        lore.add("§7in the area above.");
        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(" "));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuideButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§b§lFISH PRICE GUIDE"));
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to view prices of");
        lore.add("§7all custom fish.");
        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
