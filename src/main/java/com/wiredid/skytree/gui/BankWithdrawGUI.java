package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.banking.BankService;
import com.wiredid.skytree.banking.model.BankAccount;
import com.wiredid.skytree.banking.util.BankUtil;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Withdraw GUI - Quick amount selection with fees
 */
public class BankWithdrawGUI {

    private final BankService bankService;

    public BankWithdrawGUI(SkytreePlugin plugin, BankService bankService) {
        this.bankService = bankService;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lBank §8» §7Withdraw"));

        BankAccount account = bankService.getAccount(player.getUniqueId());
        long balance = account.getBalance();

        // Quick amount buttons
        gui.setItem(10, createAmountButton(BankUtil.toCents(100), "100", balance));
        gui.setItem(11, createAmountButton(BankUtil.toCents(1000), "1,000", balance));
        gui.setItem(12, createAmountButton(BankUtil.toCents(10000), "10,000", balance));
        gui.setItem(13, createAmountButton(BankUtil.toCents(100000), "100,000", balance));
        gui.setItem(14, createAmountButton(BankUtil.toCents(1000000), "1,000,000", balance));
        gui.setItem(15, createAllButton(balance));
        gui.setItem(16, createCustomButton());

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.BLACK_STAINED_GLASS_PANE,
                Material.RED_STAINED_GLASS_PANE);

        // Info display
        gui.setItem(4, createBalanceInfo(balance));

        // Back button
        gui.setItem(22, createBackButton());

        player.openInventory(gui);
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);
    }

    private ItemStack createAmountButton(long amount, String displayAmount, long balance) {
        long fee = bankService.calculateFee(amount);
        long totalNeeded = amount + fee;
        boolean canAfford = balance >= totalNeeded;

        ItemStack item = new ItemStack(canAfford ? Material.RED_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse((canAfford ? "§c" : "§7") + "§l" + "\u20AE " + displayAmount));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Amount: §e" + BankUtil.formatCurrency(amount));
        lore.add("§7Fee: §c" + BankUtil.formatCurrency(fee) + " §7(0.01%)");
        lore.add("§7Total: §e" + BankUtil.formatCurrency(totalNeeded));
        lore.add("");

        if (canAfford) {
            lore.add("§e§lCLICK §7to withdraw");
        } else {
            lore.add("§c§lINSUFFICIENT BALANCE");
        }

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createAllButton(long balance) {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§c§l✦ WITHDRAW ALL"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Withdraw your entire balance");
        lore.add("§7Amount: §e" + BankUtil.formatCurrency(balance));
        lore.add("");
        lore.add("§c§lWARNING: §7This will close your account");
        lore.add("§e§lCLICK §7to withdraw all");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createCustomButton() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§e§l✎ CUSTOM AMOUNT"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Use command:");
        lore.add("§e/bank withdraw <amount>");
        lore.add("");
        lore.add("§7Example:");
        lore.add("§e/bank withdraw 5000");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createBalanceInfo(long balance) {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§6§l💰 BANK BALANCE"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Available: §e" + BankUtil.formatCurrency(balance));
        lore.add("");
        lore.add("§7Select an amount below");
        lore.add("§7to withdraw from your bank");
        lore.add("");
        lore.add("§c§lNOTE: §70.01% fee applies");

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
}
