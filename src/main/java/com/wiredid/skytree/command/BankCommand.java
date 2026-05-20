package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.banking.BankService;
import com.wiredid.skytree.banking.model.BankAccount;
import com.wiredid.skytree.banking.model.BankStats;
import com.wiredid.skytree.banking.model.Transaction;
import com.wiredid.skytree.banking.model.TransactionResult;
import com.wiredid.skytree.banking.util.BankUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bank command for players
 * /bank - Main banking interface
 */
public class BankCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final BankService bankService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm");

    public BankCommand(SkytreePlugin plugin, BankService bankService) {
        this.plugin = plugin;
        this.bankService = bankService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command is for players only.");
            return true;
        }

        if (args.length == 0) {
            // Open GUI
            new com.wiredid.skytree.gui.BankMainGUI(plugin, bankService).open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "balance", "bal" -> handleBalance(player);
            case "deposit", "dep" -> handleDeposit(player, args);
            case "withdraw", "wd" -> handleWithdraw(player, args);
            case "transfer", "send" -> handleTransfer(player, args);
            case "history", "hist" -> handleHistory(player, args);
            case "stats" -> handleStats(player);
            case "help" -> handleHelp(player);
            default -> player.sendMessage("§c§l[Bank] §7Unknown command. Use §e/bank help§7.");
        }

        return true;
    }

    private void handleBalance(Player player) {
        BankAccount account = bankService.getAccount(player.getUniqueId());
        double cashUSDT = plugin.getEconomyService().getBalance(player.getUniqueId());

        player.sendMessage("§8§m                                                  ");
        player.sendMessage("§6§l⚡ SKYTREE BANK ⚡");
        player.sendMessage("");
        player.sendMessage("§7Bank Balance: §e" + BankUtil.formatCurrency(account.getBalance()));
        player.sendMessage("§7Cash in Hand: §e" + plugin.getEconomyService().format(cashUSDT));
        player.sendMessage(
                "§7Total Wealth: §a"
                        + plugin.getEconomyService().format(BankUtil.toUSDT(account.getBalance()) + cashUSDT));
        player.sendMessage("");
        player.sendMessage("§7Interest Rate: §a6% per hour");

        long nextInterest = bankService.getNextInterestAmount(player.getUniqueId());
        if (nextInterest > 0) {
            long timeUntil = bankService.getTimeUntilNextInterest(player.getUniqueId());
            long seconds = timeUntil / 1000;
            player.sendMessage(
                    "§7Next Interest: §e+" + BankUtil.formatCurrency(nextInterest) + " §7in §e" + seconds + "s");
        }

        player.sendMessage("§8§m                                                  ");
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c§l[Bank] §7Usage: §e/bank deposit <amount|all>");
            return;
        }

        long amount;
        if (args[1].equalsIgnoreCase("all")) {
            double cashUSDT = plugin.getEconomyService().getBalance(player.getUniqueId());
            amount = BankUtil.toCents(cashUSDT);
        } else {
            try {
                double USDT = NumberUtil.parseSmartNumber(args[1]);
                amount = BankUtil.toCents(USDT);
            } catch (NumberFormatException e) {
                player.sendMessage("§c§l[Bank] §7Invalid amount!");
                return;
            }
        }

        if (amount <= 0) {
            player.sendMessage("§c§l[Bank] §7Amount must be positive!");
            return;
        }

        TransactionResult result = bankService.deposit(player.getUniqueId(), amount);
        player.sendMessage(result.getMessage());

        if (result.isSuccess()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c§l[Bank] §7Usage: §e/bank withdraw <amount|all>");
            return;
        }

        long amount;
        if (args[1].equalsIgnoreCase("all")) {
            BankAccount account = bankService.getAccount(player.getUniqueId());
            amount = account.getBalance();
        } else {
            try {
                double USDT = NumberUtil.parseSmartNumber(args[1]);
                amount = BankUtil.toCents(USDT);
            } catch (NumberFormatException e) {
                player.sendMessage("§c§l[Bank] §7Invalid amount!");
                return;
            }
        }

        if (amount <= 0) {
            player.sendMessage("§c§l[Bank] §7Amount must be positive!");
            return;
        }

        TransactionResult result = bankService.withdraw(player.getUniqueId(), amount);
        player.sendMessage(result.getMessage());

        if (result.isSuccess()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void handleTransfer(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c§l[Bank] §7Usage: §e/bank transfer <player> <amount>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c§l[Bank] §7Player not found!");
            return;
        }

        long amount;
        try {
            double USDT = NumberUtil.parseSmartNumber(args[2]);
            amount = BankUtil.toCents(USDT);
        } catch (NumberFormatException e) {
            player.sendMessage("§c§l[Bank] §7Invalid amount format! Use 100, 1k, 1m etc.");
            return;
        }

        if (amount <= 0) {
            player.sendMessage("§c§l[Bank] §7Amount must be positive!");
            return;
        }

        TransactionResult result = bankService.transfer(player.getUniqueId(), target.getUniqueId(), amount);
        player.sendMessage(result.getMessage());

        if (result.isSuccess()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            target.sendMessage("§a§l[Bank] §7You received §e" + BankUtil.formatCurrency(amount) + " §7from §b"
                    + player.getName() + "§7.");
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void handleHistory(Player player, String[] args) {
        int page = 0;
        if (args.length >= 2) {
            try {
                page = (int) NumberUtil.parseSmartNumber(args[1]) - 1; // Convert to 0-indexed
                if (page < 0)
                    page = 0;
            } catch (NumberFormatException e) {
                player.sendMessage("§c§l[Bank] §7Invalid page number!");
                return;
            }
        }

        List<Transaction> transactions = bankService.getTransactionHistory(player.getUniqueId(), page, 10);

        if (transactions.isEmpty()) {
            player.sendMessage("§c§l[Bank] §7No transactions found.");
            return;
        }

        player.sendMessage("§8§m                                                  ");
        player.sendMessage("§6§l⚡ TRANSACTION HISTORY §7(Page " + (page + 1) + ")");
        player.sendMessage("");

        for (Transaction tx : transactions) {
            String typeColor = getTypeColor(tx.getType());
            String typeSymbol = getTypeSymbol(tx.getType());
            String date = dateFormat.format(new Date(tx.getTimestamp()));

            player.sendMessage(typeColor + typeSymbol + " " + tx.getType().name() + " §8- §7" + date);
            player.sendMessage("  §7Amount: §e" + BankUtil.formatCurrency(tx.getAmount()) +
                    (tx.getFee() > 0 ? " §7(Fee: §c" + BankUtil.formatCurrency(tx.getFee()) + "§7)" : ""));
        }

        player.sendMessage("");
        player.sendMessage("§7Use §e/bank history " + (page + 2) + " §7for next page");
        player.sendMessage("§8§m                                                  ");
    }

    private void handleStats(Player player) {
        BankStats stats = bankService.getStats(player.getUniqueId());

        long accountAgeDays = stats.getAccountAge() / (24 * 60 * 60 * 1000);

        player.sendMessage("§8§m                                                  ");
        player.sendMessage("§6§l⚡ BANKING STATISTICS ⚡");
        player.sendMessage("");
        player.sendMessage("§7Current Balance: §e" + BankUtil.formatCurrency(stats.getCurrentBalance()));
        player.sendMessage("§7Total Deposited: §a" + BankUtil.formatCurrency(stats.getTotalDeposited()));
        player.sendMessage("§7Total Withdrawn: §c" + BankUtil.formatCurrency(stats.getTotalWithdrawn()));
        player.sendMessage(
                "§7Total Interest: §6" + BankUtil.formatCurrency(stats.getTotalInterestEarned()));
        player.sendMessage("§7Total Transfers: §b" + stats.getTotalTransfers());
        player.sendMessage("§7Total Fees Paid: §c" + BankUtil.formatCurrency(stats.getTotalFeesPaid()));
        player.sendMessage("");
        player.sendMessage("§7Net Profit: " + (stats.getNetProfit() >= 0 ? "§a+" : "§c")
                + BankUtil.formatCurrency(stats.getNetProfit()));
        player.sendMessage("§7Account Age: §e" + accountAgeDays + " days");
        player.sendMessage("§8§m                                                  ");
    }

    private void handleHelp(Player player) {
        player.sendMessage("§8§m                                                  ");
        player.sendMessage("§6§l⚡ BANK COMMANDS ⚡");
        player.sendMessage("");
        player.sendMessage("§e/bank §8- §7Open bank GUI");
        player.sendMessage("§e/bank balance §8- §7Check your balance");
        player.sendMessage("§e/bank deposit <amount|all> §8- §7Deposit to bank");
        player.sendMessage("§e/bank withdraw <amount|all> §8- §7Withdraw from bank");
        player.sendMessage("§e/bank transfer <player> <amount> §8- §7Transfer money");
        player.sendMessage("§e/bank history [page] §8- §7View transactions");
        player.sendMessage("§e/bank stats §8- §7View statistics");
        player.sendMessage("");
        player.sendMessage("§7Interest Rate: §a6% per hour");
        player.sendMessage("§7Transaction Fee: §c0.01%");
        player.sendMessage("§8§m                                                  ");
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

    private String getTypeSymbol(Transaction.TransactionType type) {
        return switch (type) {
            case DEPOSIT -> "⬆";
            case WITHDRAW -> "⬇";
            case TRANSFER -> "➜";
            case INTEREST -> "★";
            case FEE -> "✖";
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("balance", "deposit", "withdraw", "transfer", "history", "stats", "help").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("transfer")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("deposit") || args[0].equalsIgnoreCase("withdraw")) {
                return Arrays.asList("10", "100", "1000", "all").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}
