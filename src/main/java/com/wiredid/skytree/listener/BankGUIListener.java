package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.banking.BankService;
import com.wiredid.skytree.banking.model.TransactionResult;
import com.wiredid.skytree.banking.util.BankUtil;
import com.wiredid.skytree.gui.*;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for bank GUI interactions
 */
public class BankGUIListener implements Listener {

    private final SkytreePlugin plugin;
    private final BankService bankService;
    private final BankMainGUI mainGUI;
    private final BankDepositGUI depositGUI;
    private final BankWithdrawGUI withdrawGUI;
    private final BankHistoryGUI historyGUI;

    public BankGUIListener(SkytreePlugin plugin, BankService bankService) {
        this.plugin = plugin;
        this.bankService = bankService;
        this.mainGUI = new BankMainGUI(plugin, bankService);
        this.depositGUI = new BankDepositGUI(plugin, bankService);
        this.withdrawGUI = new BankWithdrawGUI(plugin, bankService);
        this.historyGUI = new BankHistoryGUI(plugin, bankService);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = ComponentUtil.toLegacy(event.getView().title());

        // Check if it's a bank GUI
        if (!title.contains("Bank")) {
            return;
        }

        // Don't cancel when clicking in player's own inventory
        if (event.getClickedInventory() == player.getInventory())
            return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        // Handle different GUIs
        if (title.contains("Account")) {
            handleMainGUI(player, clicked);
        } else if (title.contains("Deposit")) {
            handleDepositGUI(player, clicked);
        } else if (title.contains("Withdraw")) {
            handleWithdrawGUI(player, clicked);
        } else if (title.contains("History")) {
            handleHistoryGUI(player, clicked, title);
        }
    }

    private void handleMainGUI(Player player, ItemStack clicked) {
        String displayName = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());

        if (displayName.contains("DEPOSIT")) {
            depositGUI.open(player);
        } else if (displayName.contains("WITHDRAW")) {
            withdrawGUI.open(player);
        } else if (displayName.contains("TRANSFER")) {
            player.closeInventory();
            player.sendMessage("§e§l[Bank] §7Use command: §e/bank transfer <player> <amount>");
        } else if (displayName.contains("HISTORY")) {
            historyGUI.open(player, 0);
        } else if (displayName.contains("STATISTICS")) {
            player.closeInventory();
            player.performCommand("bank stats");
        }
    }

    private void handleDepositGUI(Player player, ItemStack clicked) {
        String displayName = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());

        if (displayName.contains("BACK")) {
            mainGUI.open(player);
            return;
        }

        if (displayName.contains("CUSTOM")) {
            player.closeInventory();
            player.sendMessage("§e§l[Bank] §7Use command: §e/bank deposit <amount>");
            return;
        }

        // Parse amount from display name
        long amount = parseAmountFromDisplay(displayName);

        if (amount == -1) {
            // Deposit All
            amount = BankUtil.toCents(plugin.getEconomyService().getBalance(player.getUniqueId()));
        }

        if (amount > 0) {
            TransactionResult result = bankService.deposit(player.getUniqueId(), amount);
            player.closeInventory();
            player.sendMessage(result.getMessage());

            if (result.isSuccess()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            } else {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    private void handleWithdrawGUI(Player player, ItemStack clicked) {
        String displayName = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());

        if (displayName.contains("BACK")) {
            mainGUI.open(player);
            return;
        }

        if (displayName.contains("CUSTOM")) {
            player.closeInventory();
            player.sendMessage("§e§l[Bank] §7Use command: §e/bank withdraw <amount>");
            return;
        }

        // Parse amount from display name
        long amount = parseAmountFromDisplay(displayName);

        if (amount == -1) {
            // Withdraw All
            amount = bankService.getAccount(player.getUniqueId()).getBalance();
        }

        if (amount > 0) {
            TransactionResult result = bankService.withdraw(player.getUniqueId(), amount);
            player.closeInventory();
            player.sendMessage(result.getMessage());

            if (result.isSuccess()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            } else {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    private void handleHistoryGUI(Player player, ItemStack clicked, String title) {
        String displayName = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());

        if (displayName.contains("BACK")) {
            mainGUI.open(player);
            return;
        }

        // Parse current page from title
        int currentPage = parsePageFromTitle(title);

        if (displayName.contains("PREVIOUS PAGE")) {
            historyGUI.open(player, currentPage - 1);
        } else if (displayName.contains("NEXT PAGE")) {
            historyGUI.open(player, currentPage + 1);
        }
    }

    private long parseAmountFromDisplay(String displayName) {
        // Remove color codes and formatting
        String clean = displayName.replaceAll("§.", "").trim();

        if (clean.contains("DEPOSIT ALL") || clean.contains("WITHDRAW ALL")) {
            // Special case handled by GUI
            return -1;
        }

        // Parse amounts like "100 ₮", "1,000 ₮", "1M ₮"
        try {
            String val = clean.replace("\u20AE", "").replace(",", "").trim();
            if (val.toUpperCase().endsWith("M")) {
                String num = val.substring(0, val.length() - 1).trim();
                return BankUtil.toCents(Double.parseDouble(num) * 1_000_000);
            } else if (val.toUpperCase().endsWith("K")) {
                String num = val.substring(0, val.length() - 1).trim();
                return BankUtil.toCents(Double.parseDouble(num) * 1_000);
            } else {
                return BankUtil.toCents(Double.parseDouble(val));
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parsePageFromTitle(String title) {
        try {
            // Extract page number from "(X)"
            String pageStr = title.substring(title.lastIndexOf("(") + 1, title.lastIndexOf(")"));
            return Integer.parseInt(pageStr) - 1; // Convert to 0-indexed
        } catch (Exception e) {
            return 0;
        }
    }
}
