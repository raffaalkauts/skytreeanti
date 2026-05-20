package com.wiredid.skytree.command;

import com.wiredid.skytree.api.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.wiredid.skytree.util.NumberUtil;

/**
 * Balance command with admin functions
 * /bal - Check balance
 * /setbal <player> <amount> - Set player balance (admin)
 * /addbal <player> <amount> - Add to player balance (admin)
 */
public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final EconomyService economy;

    public BalanceCommand(EconomyService economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        // /bal - Check own balance
        if (cmdName.equals("bal") || cmdName.equals("balance") || cmdName.equals("USDT") || cmdName.equals("money")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;
            double balance = economy.getBalance(player.getUniqueId());
            player.sendMessage("§6§l[Skytree] §eYour balance: §f" + economy.format(balance));
            return true;
        }

        // /setbal <player|selector> <amount> - Admin only
        if (cmdName.equals("setbal")) {
            if (!sender.hasPermission("skytree.admin")) {
                sender.sendMessage("§c§l[Skytree] §cYou don't have permission!");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage("§c§l[Skytree] §cUsage: /setbal <player|selector> <amount>");
                return true;
            }

            try {
                double amount = NumberUtil.parseSmartNumber(args[1]);
                if (amount < 0) {
                    sender.sendMessage("§c§l[Skytree] §cAmount must be positive!");
                    return true;
                }

                java.util.List<org.bukkit.OfflinePlayer> targets = com.wiredid.skytree.util.CommandUtil
                        .resolveTargets(sender, args[0]);
                if (targets.isEmpty()) {
                    sender.sendMessage("§c§l[Skytree] §cNo players found matching: " + args[0]);
                    return true;
                }

                for (org.bukkit.OfflinePlayer target : targets) {
                    economy.setBalance(target.getUniqueId(), amount);
                    sender.sendMessage(
                            "§a§l[Skytree] §aSet §f" + (target.getName() != null ? target.getName() : "Unknown")
                                    + "§a's balance to §e" + economy.format(amount));
                    if (target.isOnline() && target.getPlayer() != null) {
                        target.getPlayer().sendMessage(
                                "§6§l[Skytree] §eYour balance has been set to §f" + economy.format(amount));
                    }
                }

            } catch (NumberFormatException e) {
                sender.sendMessage("§c§l[Skytree] §cInvalid amount!");
            }

            return true;
        }

        // /addbal <player|selector> <amount> - Admin only
        if (cmdName.equals("addbal")) {
            if (!sender.hasPermission("skytree.admin")) {
                sender.sendMessage("§c§l[Skytree] §cYou don't have permission!");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage("§c§l[Skytree] §cUsage: /addbal <player|selector> <amount>");
                return true;
            }

            try {
                double amount = NumberUtil.parseSmartNumber(args[1]);

                java.util.List<org.bukkit.OfflinePlayer> targets = com.wiredid.skytree.util.CommandUtil
                        .resolveTargets(sender, args[0]);
                if (targets.isEmpty()) {
                    sender.sendMessage("§c§l[Skytree] §cNo players found matching: " + args[0]);
                    return true;
                }

                for (org.bukkit.OfflinePlayer target : targets) {
                    economy.addBalance(target.getUniqueId(), amount);
                    double newBalance = economy.getBalance(target.getUniqueId());

                    sender.sendMessage("§a§l[Skytree] §aAdded §e" + economy.format(amount) + " §ato §f"
                            + (target.getName() != null ? target.getName() : "Unknown"));
                    sender.sendMessage("§7New balance: §f" + economy.format(newBalance));

                    if (target.isOnline() && target.getPlayer() != null) {
                        target.getPlayer()
                                .sendMessage("§6§l[Skytree] §eYou received §f" + economy.format(amount) + "!");
                    }
                }

            } catch (NumberFormatException e) {
                sender.sendMessage("§c§l[Skytree] §cInvalid amount!");
            }

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmdName = command.getName().toLowerCase();

        // Tab completion for setbal and addbal
        if (cmdName.equals("setbal") || cmdName.equals("addbal")) {
            if (!sender.hasPermission("skytree.admin")) {
                return new ArrayList<>();
            }

            if (args.length == 1) {
                // Player names
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args.length == 2) {
                // Amount suggestions
                return Arrays.asList("100", "1000", "10000", "100000");
            }
        }

        return new ArrayList<>();
    }
}
