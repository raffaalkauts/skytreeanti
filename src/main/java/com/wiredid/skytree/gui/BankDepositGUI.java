package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.banking.BankService;
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
 * Deposit GUI - Quick amount selection
 */
public class BankDepositGUI {

    private final SkytreePlugin plugin;

    public BankDepositGUI(SkytreePlugin plugin, BankService bankService) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lBank §8» §7Deposit"));

        double cashUSDT = plugin.getEconomyService().getBalance(player.getUniqueId());
        long cashCents = BankUtil.toCents(cashUSDT);

        // Quick amount buttons
        gui.setItem(10, createAmountButton(BankUtil.toCents(100), "100", cashCents));
        gui.setItem(11, createAmountButton(BankUtil.toCents(1000), "1,000", cashCents));
        gui.setItem(12, createAmountButton(BankUtil.toCents(10000), "10,000", cashCents));
        gui.setItem(13, createAmountButton(BankUtil.toCents(100000), "100,000", cashCents));
        gui.setItem(14, createAmountButton(BankUtil.toCents(1000000), "1,000,000", cashCents));
        gui.setItem(15, createAllButton(cashCents));
        gui.setItem(16, createCustomButton());

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.BLACK_STAINED_GLASS_PANE,
                Material.LIME_STAINED_GLASS_PANE);
        gui.setItem(4, createCashInfo(cashUSDT));

        // Back button
        gui.setItem(22, createBackButton());

        player.openInventory(gui);
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);
    }

    private ItemStack createAmountButton(long amount, String displayAmount, long cashCents) {
        boolean canAfford = cashCents >= amount;

        ItemStack item = new ItemStack(canAfford ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse((canAfford ? "§a" : "§7") + "§l" + "\u20AE " + displayAmount));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Amount: §e" + BankUtil.formatCurrency(amount));
        lore.add("§7Fee: §a\u20AE 0 §7(No deposit fee)");
        lore.add("");

        if (canAfford) {
            lore.add("§e§lCLICK §7to deposit");
        } else {
            lore.add("§c§lINSUFFICIENT CASH");
        }

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createAllButton(long cashCents) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§a§l✦ DEPOSIT ALL"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Deposit all your cash");
        lore.add("§7Amount: §e" + BankUtil.formatCurrency(cashCents));
        lore.add("");
        lore.add("§e§lCLICK §7to deposit all");

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
        lore.add("§e/bank deposit <amount>");
        lore.add("");
        lore.add("§7Example:");
        lore.add("§e/bank deposit 5000");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createCashInfo(double cashUSDT) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtil.parse("§6§l💰 AVAILABLE CASH"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Cash in Hand: §e" + BankUtil.formatCurrency(BankUtil.toCents(cashUSDT)));
        lore.add("");
        lore.add("§7Select an amount below");
        lore.add("§7to deposit into your bank");

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
