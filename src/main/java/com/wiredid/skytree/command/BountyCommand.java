package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.BountyService;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.model.Bounty;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BountyCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final BountyService bountyService;
    private final EconomyService economyService;

    public BountyCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.bountyService = plugin.getBountyService();
        this.economyService = plugin.getEconomyService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // /bounty
        if (args.length == 0) {
            openBountyGUI(player);
            return true;
        }

        // /bounty list
        if (args[0].equalsIgnoreCase("list")) {
            openBountyGUI(player);
            return true; // GUI implementation details later, for now just open
        }

        // /bounty top
        if (args[0].equalsIgnoreCase("top")) {
            int topCount = plugin.getConfig().getInt("bounty.top_count", 10);
            List<Bounty> top = bountyService.getTopBounties(topCount);
            player.sendMessage("§6§lTOP BOUNTIES");
            int rank = 1;
            for (Bounty b : top) {
                String name = Bukkit.getOfflinePlayer(b.getTarget()).getName();
                player.sendMessage("§e#" + rank + " §f" + name + " §7- §a$" + NumberUtil.formatCurrency(b.getAmount()));
                rank++;
            }
            if (top.isEmpty()) {
                player.sendMessage("§7No active bounties.");
            }
            return true;
        }

        // /bounty <player> <amount>
        if (args.length >= 2) {
            String targetName = args[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            // Check if player has played before to avoid junk UUIDs
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage("§cPlayer §e" + targetName + " §cnot found.");
                return true;
            }

            if (target.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage("§cYou cannot put a bounty on yourself!");
                return true;
            }

            try {
                double amount = NumberUtil.parseSmartNumber(args[1]);
                double minBounty = plugin.getConfig().getDouble("bounty.min_amount", 1000);
                if (amount < minBounty) {
                    player.sendMessage("§cMinimum bounty is $" + NumberUtil.formatCurrency(minBounty) + ".");
                    return true;
                }

                if (economyService.getBalance(player.getUniqueId()) < amount) {
                    player.sendMessage("§cInsufficient funds.");
                    return true;
                }

                economyService.removeBalance(player.getUniqueId(), amount);
                bountyService.addBounty(target.getUniqueId(), player.getUniqueId(), amount);

                player.sendMessage(ComponentUtil
                        .parse("§aSuccessfully placed a §2$" + NumberUtil.formatCurrency(amount) + " §abounty on §e"
                                + target.getName() + "§a!"));
                Bukkit.broadcast(ComponentUtil.parse("§6§lBOUNTY! §e" + player.getName() + " §7has placed a §a$"
                        + NumberUtil.formatCurrency(amount) + " §7bounty on §c" + target.getName() + "§7!"));

            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount.");
            }
            return true;
        }

        player.sendMessage("§cUsage: /bounty <player> <amount> or /bounty list");
        return true;
    }

    private void openBountyGUI(Player player) {
        if (plugin.getBountyGUI() != null) {
            plugin.getBountyGUI().open(player);
        } else {
            player.sendMessage("§cBounty GUI not initialized.");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("list");
            completions.add("top");
            // Add online players
            completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
