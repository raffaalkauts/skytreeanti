package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.economy.EconomyManager;
import com.wiredid.skytree.model.Rank;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EconomyAdminCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;

    public EconomyAdminCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isAuthorized(CommandSender sender) {
        if (sender.isOp() || !(sender instanceof Player)) return true;
        Rank rank = plugin.getRankService().getRank(((Player) sender).getUniqueId());
        return rank.isAtLeast(Rank.ADMIN);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isAuthorized(sender)) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /econadmin <stats|set|recalc|reload>");
            return true;
        }

        EconomyManager em = plugin.getEconomyManager();
        if (em == null) {
            sender.sendMessage("§cEconomyManager not initialized.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "stats" -> {
                sender.sendMessage("§6§l=== Economy Manager Stats ===");
                sender.sendMessage("§7Current M2: §e" + NumberUtil.formatCurrency(em.getCurrentM2()));
                sender.sendMessage("§7Price Multiplier: §b" + String.format("%.4f", em.getPriceMultiplier()));
                sender.sendMessage("§7Dynamic Rate: §b" + String.format("%.4f", em.getDynamicInterestRate()));
                sender.sendMessage("§7Total Reserve: §c" + NumberUtil.formatCurrency(em.getTotalReserve()));
            }
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /econadmin set <target|elasticity|rate> <value>");
                    return true;
                }
                try {
                    double value = Double.parseDouble(args[2]);
                    switch (args[1].toLowerCase()) {
                        case "target" -> {
                            plugin.getConfig().set("economy_manager.target_m2", value);
                            plugin.saveConfig();
                            em.recalculateM2();
                            sender.sendMessage("§aTarget M2 set to §e" + NumberUtil.formatCurrency(value));
                        }
                        case "elasticity" -> {
                            plugin.getConfig().set("economy_manager.elasticity", value);
                            plugin.saveConfig();
                            em.recalculateM2();
                            sender.sendMessage("§aElasticity set to §e" + value);
                        }
                        case "rate" -> {
                            plugin.getConfig().set("economy_manager.base_interest_rate", value);
                            plugin.saveConfig();
                            em.recalculateM2();
                            sender.sendMessage("§aBase interest rate set to §e" + value);
                        }
                        default -> sender.sendMessage("§cUnknown property. Use: target, elasticity, or rate.");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid number: " + args[2]);
                }
            }
            case "recalc" -> {
                em.recalculateM2();
                sender.sendMessage("§aM2 recalculated. Current: §e" + NumberUtil.formatCurrency(em.getCurrentM2()));
            }
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getEconomyManager().recalculateM2();
                sender.sendMessage("§aEconomy config reloaded.");
            }
            default -> sender.sendMessage("§cUnknown subcommand. Use: stats, set, recalc, reload");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!isAuthorized(sender)) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            result.addAll(List.of("stats", "set", "recalc", "reload"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            result.addAll(List.of("target", "elasticity", "rate"));
        }
        return result.stream().filter(s -> s.startsWith(args[args.length - 1].toLowerCase())).toList();
    }
}
