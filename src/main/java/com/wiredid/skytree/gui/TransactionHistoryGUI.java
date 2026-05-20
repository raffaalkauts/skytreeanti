package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.api.Transaction;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryGUI {

    private final EconomyService economy;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    public TransactionHistoryGUI(SkytreePlugin plugin, EconomyService economy) {
        this.economy = economy;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ComponentUtil.smartParse("§8Transaction History"));
        GuiUtil.applyPremiumBorder(inv, Material.BLACK_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE);

        List<Transaction> transactions = economy.getTransactions(player.getUniqueId());

        // Slot mapping: Center 28 slots
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        if (transactions == null || transactions.isEmpty()) {
            ItemStack emptyItem = new ItemStack(Material.PAPER);
            ItemMeta meta = emptyItem.getItemMeta();
            meta.displayName(ComponentUtil.smartParse("§cNo Transactions Found"));
            emptyItem.setItemMeta(meta);
            inv.setItem(22, emptyItem);
        } else {
            int slotIdx = 0;
            // Iterate normal order (most recent is usually first in our LinkedList impl if
            // we added First,
            // but let's just iterate naturally)
            // Wait, we used addFirst, so index 0 is newest.
            for (Transaction tx : transactions) {
                if (slotIdx >= slots.length)
                    break;

                boolean isDeposit = "DEPOSIT".equals(tx.getType());
                Material mat = isDeposit ? Material.LIME_DYE : Material.RED_DYE;
                String prefix = isDeposit ? "§a[+]" : "§c[-]";

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(ComponentUtil.smartParse(prefix + " " + NumberUtil.formatCurrency(tx.getAmount())));

                List<String> lore = new ArrayList<>();
                lore.add("§7Reason: §f" + tx.getReason());
                lore.add("§7Time: §e" + tx.getTimestamp().format(DATE_FORMAT));
                meta.lore(ComponentUtil.parseList(lore));
                item.setItemMeta(meta);

                inv.setItem(slots[slotIdx++], item);
            }
        }

        // Close Button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cMeta = close.getItemMeta();
        cMeta.displayName(ComponentUtil.smartParse("§cClose"));
        close.setItemMeta(cMeta);
        inv.setItem(49, close);

        player.openInventory(inv);
    }
}
