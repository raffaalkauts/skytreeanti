package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.banking.BankService;
import com.wiredid.skytree.banking.model.Transaction;
import com.wiredid.skytree.banking.util.BankUtil;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Transaction history GUI - Paginated view
 */
public class BankHistoryGUI {

    private final BankService bankService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm");

    public BankHistoryGUI(SkytreePlugin plugin, BankService bankService) {
        this.bankService = bankService;
    }

    public void open(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, 54,
                ComponentUtil.parse("§6§lBank §8» §7History §8(" + (page + 1) + ")"));

        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE);

        List<Transaction> transactions = bankService.getTransactionHistory(player.getUniqueId(), page, 45);

        if (transactions.isEmpty()) {
            gui.setItem(22, createNoTransactionsItem());
        } else {
            for (int i = 0; i < Math.min(transactions.size(), 45); i++) {
                gui.setItem(i, createTransactionItem(transactions.get(i), player.getUniqueId()));
            }
        }

        // Navigation
        if (page > 0) {
            gui.setItem(48, createPreviousPageButton(page));
        }

        if (transactions.size() == 45) {
            gui.setItem(50, createNextPageButton(page));
        }

        // Back button
        gui.setItem(49, createBackButton());

        player.openInventory(gui);
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    private ItemStack createTransactionItem(Transaction tx, java.util.UUID playerId) {
        Material material = getTransactionMaterial(tx.getType());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String typeColor = getTypeColor(tx.getType());
        String date = dateFormat.format(new Date(tx.getTimestamp()));

        meta.displayName(ComponentUtil.parse(typeColor + "§l" + tx.getType().name()));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Date: §f" + date);
        lore.add("§7Amount: §e" + BankUtil.formatCurrency(tx.getAmount()));

        if (tx.getFee() > 0) {
            lore.add("§7Fee: §c" + BankUtil.formatCurrency(tx.getFee()));
        }

        lore.add("§7Status: " + getStatusColor(tx.getStatus()) + tx.getStatus().name());

        // Add from/to info for transfers
        if (tx.getType() == Transaction.TransactionType.TRANSFER) {
            if (tx.getFromAccount().equals(playerId.toString())) {
                lore.add("§7To: §b" + getPlayerName(tx.getToAccount()));
            } else {
                lore.add("§7From: §b" + getPlayerName(tx.getFromAccount()));
            }
        }

        lore.add("");
        lore.add("§8ID: " + tx.getTransactionId().toString().substring(0, 8));

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createNoTransactionsItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§c§lNo Transactions"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7You haven't made any transactions yet");
        lore.add("§7Start using the bank to see your history!");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createPreviousPageButton(int currentPage) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§e§l← PREVIOUS PAGE"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Go to page " + currentPage);

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createNextPageButton(int currentPage) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§e§lNEXT PAGE →"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Go to page " + (currentPage + 2));

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§c§l← BACK"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Return to main menu");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private Material getTransactionMaterial(Transaction.TransactionType type) {
        return switch (type) {
            case DEPOSIT -> Material.LIME_DYE;
            case WITHDRAW -> Material.RED_DYE;
            case TRANSFER -> Material.YELLOW_DYE;
            case INTEREST -> Material.SUNFLOWER;
            case FEE -> Material.REDSTONE;
        };
    }

    private String getTypeColor(Transaction.TransactionType type) {
        return switch (type) {
            case DEPOSIT -> "§a";
            case WITHDRAW -> "§c";
            case TRANSFER -> "§e";
            case INTEREST -> "§6";
            case FEE -> "§c";
        };
    }

    private String getStatusColor(Transaction.TransactionStatus status) {
        return switch (status) {
            case COMPLETED -> "§a";
            case PENDING -> "§e";
            case FAILED -> "§c";
            case REVERSED -> "§c";
        };
    }

    private String getPlayerName(String uuidOrSpecial) {
        if ("CASH".equals(uuidOrSpecial) || "SYSTEM".equals(uuidOrSpecial)) {
            return uuidOrSpecial;
        }

        try {
            java.util.UUID uuid = java.util.UUID.fromString(uuidOrSpecial);
            org.bukkit.OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            return player.getName() != null ? player.getName() : "Unknown";
        } catch (IllegalArgumentException e) {
            return "Unknown";
        }
    }
}
