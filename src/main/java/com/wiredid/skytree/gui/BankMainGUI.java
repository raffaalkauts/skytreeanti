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
 * Main bank GUI - Premium glassmorphism design
 */
public class BankMainGUI {

    private final SkytreePlugin plugin;
    private final BankService bankService;

    public BankMainGUI(SkytreePlugin plugin, BankService bankService) {
        this.plugin = plugin;
        this.bankService = bankService;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ComponentUtil.parse("§6§lBank §8» §7Account"));

        BankAccount account = bankService.getAccount(player.getUniqueId());
        double cashUSDT = plugin.getEconomyService().getBalance(player.getUniqueId());

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.BLACK_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE);

        // Main action buttons
        gui.setItem(20, createDepositButton());
        gui.setItem(21, createWithdrawButton());
        gui.setItem(22, createTransferButton());
        gui.setItem(23, createHistoryButton());
        gui.setItem(24, createStatsButton());

        // Info display in center
        gui.setItem(13, createInfoDisplay(account, cashUSDT, player));

        // Interest info
        long nextInterest = bankService.getNextInterestAmount(player.getUniqueId());
        long timeUntil = bankService.getTimeUntilNextInterest(player.getUniqueId());
        gui.setItem(31, createInterestInfo(nextInterest, timeUntil));

        player.openInventory(gui);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.5f);
    }

    private ItemStack createDepositButton() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§a§l⬆ DEPOSIT"));

        List<String> lore = new ArrayList<>();
        lore.add("§7Deposit cash into your bank account");
        lore.add("");
        lore.add("§7• §aNo fees §7for deposits");
        lore.add("§7• §aEarn interest §7on your balance");
        lore.add("§7• §aSecure storage §7for your money");
        lore.add("");
        lore.add("§e§lCLICK §7to deposit");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createWithdrawButton() {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§c§l⬇ WITHDRAW"));

        List<String> lore = new ArrayList<>();
        lore.add("§7Withdraw money from your account");
        lore.add("");
        lore.add("§7• §cFee: §e0.01%");
        lore.add("§7• §aInstant transfer §7to cash");
        lore.add("");
        lore.add("§e§lCLICK §7to withdraw");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createTransferButton() {
        ItemStack item = new ItemStack(Material.YELLOW_DYE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§e§l➜ TRANSFER"));

        List<String> lore = new ArrayList<>();
        lore.add("§7Send money to another player");
        lore.add("");
        lore.add("§7• §cFee: §e0.01%");
        lore.add("§7• §aInstant delivery");
        lore.add("§7• §aSecure transaction");
        lore.add("");
        lore.add("§e§lCLICK §7to transfer");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createHistoryButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§b§l📋 HISTORY"));

        List<String> lore = new ArrayList<>();
        lore.add("§7View your transaction history");
        lore.add("");
        lore.add("§7• §aComplete audit trail");
        lore.add("§7• §aDetailed records");
        lore.add("§7• §aPaginated view");
        lore.add("");
        lore.add("§e§lCLICK §7to view history");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createStatsButton() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§d§l★ STATISTICS"));

        List<String> lore = new ArrayList<>();
        lore.add("§7View your banking statistics");
        lore.add("");
        lore.add("§7• §aTotal deposits & withdrawals");
        lore.add("§7• §aInterest earned");
        lore.add("§7• §aNet profit");
        lore.add("");
        lore.add("§e§lCLICK §7to view stats");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createInfoDisplay(BankAccount account, double cashUSDT, Player player) {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§6§l⚡ YOUR ACCOUNT"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Bank Balance: §e" + BankUtil.formatCurrency(account.getBalance()));
        lore.add("§7Cash in Hand: §e" + BankUtil.formatCurrency(BankUtil.toCents(cashUSDT)));
        lore.add("");
        lore.add("§7Total Wealth: §a" + BankUtil.formatCurrency(account.getBalance() + BankUtil.toCents(cashUSDT)));
        lore.add("");
        lore.add("§7Interest Rate: §a6% per hour");
        lore.add("§7Transaction Fee: §c0.01%");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createInterestInfo(long nextInterest, long timeUntil) {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§6§l★ INTEREST INFO"));

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (nextInterest > 0) {
            long seconds = timeUntil / 1000;
            lore.add("§7Next Interest: §e+" + BankUtil.formatCurrency(nextInterest));
            lore.add("§7Time Until: §e" + seconds + " seconds");
        } else {
            lore.add("§7Minimum balance required: §e10 \u20AE");
            lore.add("§7Deposit more to earn interest!");
        }

        lore.add("");
        lore.add("§7Interest compounds every hour");
        lore.add("§7Rate: §a0.1% per minute");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }
}
