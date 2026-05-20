package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.banking.BankService;
import com.wiredid.skytree.banking.model.Transaction;
import com.wiredid.skytree.banking.util.BankUtil;
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
 * Bank admin command
 * /bankadmin - Administrative banking operations
 */
public class BankAdminCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final BankService bankService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm:ss");

    public BankAdminCommand(SkytreePlugin plugin, BankService bankService) {
        this.plugin = plugin;
        this.bankService = bankService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("skytree.bank.admin")) {
            sender.sendMessage("§c§l[Bank] §7You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reconcile" -> handleReconcile(sender);
            case "audit" -> handleAudit(sender, args);
            case "setrate" -> handleSetRate(sender, args);
            case "stats" -> handleGlobalStats(sender);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleReconcile(CommandSender sender) {
        sender.sendMessage("§6§l[Bank Admin] §7Running reconciliation...");

        boolean balanced = bankService.reconcile();

        if (balanced) {
            sender.sendMessage("§a§l[Bank Admin] §7Reconciliation §aPASSED§7. All balances match!");
        } else {
            sender.sendMessage("§c§l[Bank Admin] §7Reconciliation §cFAILED§7! Check server logs for details.");
        }

        // Admin Logging
        java.util.UUID adminUUID = (sender instanceof Player p) ? p.getUniqueId() : null;
        plugin.getAdminService().logAction(adminUUID, "BANK", "Ran reconciliation (Passed: " + balanced + ")");
    }

    private void handleAudit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c§l[Bank Admin] §7Usage: §e/bankadmin audit <player> [days]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c§l[Bank Admin] §7Player not found!");
            return;
        }

        int days = 7; // Default 7 days
        if (args.length >= 3) {
            try {
                days = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§c§l[Bank Admin] §7Invalid number of days!");
                return;
            }
        }

        List<Transaction> auditTrail = bankService.getAuditTrail(target.getUniqueId(), days);

        if (auditTrail.isEmpty()) {
            sender.sendMessage("§c§l[Bank Admin] §7No transactions found in the last " + days + " days.");
            return;
        }

        sender.sendMessage("§8§m                                                  ");
        sender.sendMessage("§6§l[Bank Admin] §7Audit Trail for §b" + target.getName());
        sender.sendMessage("§7Period: Last " + days + " days §8(§7" + auditTrail.size() + " transactions§8)");
        sender.sendMessage("");

        for (Transaction tx : auditTrail) {
            String date = dateFormat.format(new Date(tx.getTimestamp()));
            sender.sendMessage("§8[§7" + date + "§8] §e" + tx.getType().name());
            sender.sendMessage("  §7From: §f" + tx.getFromAccount());
            sender.sendMessage("  §7To: §f" + tx.getToAccount());
            sender.sendMessage("  §7Amount: §e" + BankUtil.formatCurrency(tx.getAmount()));
            sender.sendMessage("  §7Fee: §c" + BankUtil.formatCurrency(tx.getFee()));
            sender.sendMessage("  §7Status: " + getStatusColor(tx.getStatus()) + tx.getStatus().name());
            sender.sendMessage("");
        }

        sender.sendMessage("§8§m                                                  ");
    }

    private void handleSetRate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c§l[Bank Admin] §7Usage: §e/bankadmin setrate <rate>");
            sender.sendMessage("§7Example: §e/bankadmin setrate 0.001 §7(0.1% per minute = 6% per hour)");
            return;
        }

        try {
            String cleanArg = args[1].replace("%", "").trim();
            double percentageInput = Double.parseDouble(cleanArg);

            if (percentageInput < 0) {
                sender.sendMessage("§c§l[Bank Admin] §7Rate cannot be negative!");
                return;
            }

            // Convert percentage to multiplier (e.g. 0.11 -> 0.0011)
            double multiplier = percentageInput / 100.0;

            bankService.setGlobalInterestRate(multiplier);

            double hourlyRate = multiplier * 60 * 100; // Hourly percentage
            sender.sendMessage(
                    "§a§l[Bank Admin] §7Interest rate set to §e" + String.format("%.4f", percentageInput) + "%" +
                            " §7per minute §8(§e" + String.format("%.2f", hourlyRate) + "% §7per hour§8)");

            // Admin Logging
            java.util.UUID adminUUID = (sender instanceof Player p) ? p.getUniqueId() : null;
            plugin.getAdminService().logAction(adminUUID, "BANK",
                    String.format("Set interest rate to %s%% per minute", String.format("%.4f", percentageInput)));

        } catch (NumberFormatException e) {
            sender.sendMessage("§c§l[Bank Admin] §7Invalid rate format! Use numbers like '0.11'.");
        }
    }

    private void handleGlobalStats(CommandSender sender) {
        // This is a heavy operation, so we check implementation directly
        // In a real database we would run a SUM query
        // In a real database we would run a SUM query
        // Since we don't have a direct method exposed for global stats yet, we'll add a
        // placeholder or simple logic
        // Ideally BankService should expose getGlobalStats()

        // For now, let's just show basic info
        sender.sendMessage("§8§m                                                  ");
        sender.sendMessage("§6§l[Bank Admin] §7Global Statistics");
        sender.sendMessage("");
        sender.sendMessage("§7Global Interest Rate: §e"
                + String.format("%.4f", bankService.getGlobalInterestRate() * 100) + "% per min");
        sender.sendMessage("§7Feature Status: " + (bankService != null ? "§aActive" : "§cInactive"));
        sender.sendMessage("");
        sender.sendMessage("§7Use §e/bankadmin audit <player> §7for individual stats.");
        sender.sendMessage("§8§m                                                  ");
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage("§6§l[Bank Admin] §7Reloading banking system...");

        // Reload persistence and config
        // Casting to implementation might be needed if the interface doesn't expose
        // reload
        // Or we assume the persistence service handles it

        // Since we don't have a direct reload method in the interface, we'll inform the
        // user
        sender.sendMessage("§e§l[Bank Admin] §7Configuration reload is handled via main plugin reload.");
        sender.sendMessage("§7Run §e/skytree reload §7to reload all configurations.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m                                                  ");
        sender.sendMessage("§6§l[Bank Admin] §7Administrative Commands");
        sender.sendMessage("");
        sender.sendMessage("§e/bankadmin reconcile §8- §7Run EOD reconciliation");
        sender.sendMessage("§e/bankadmin audit <player> [days] §8- §7View audit trail");
        sender.sendMessage("§e/bankadmin setrate <rate> §8- §7Set interest rate");
        sender.sendMessage("§e/bankadmin stats §8- §7View global statistics");
        sender.sendMessage("§e/bankadmin reload §8- §7Reload banking system");
        sender.sendMessage("§8§m                                                  ");
    }

    private String getStatusColor(Transaction.TransactionStatus status) {
        return switch (status) {
            case COMPLETED -> "§a";
            case PENDING -> "§e";
            case FAILED -> "§c";
            case REVERSED -> "§c";
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (!sender.hasPermission("skytree.bank.admin"))
            return new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("reconcile", "audit", "setrate", "stats", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("audit")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
