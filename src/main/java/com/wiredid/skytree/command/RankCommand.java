package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.gui.RankOverviewGUI;
import com.wiredid.skytree.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RankCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final RankOverviewGUI rankOverviewGUI;

    public RankCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.rankOverviewGUI = new RankOverviewGUI(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length == 0) {
            rankOverviewGUI.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§8§m--------------------------------");
            player.sendMessage(" §b§lSKYTREE RANKS");
            player.sendMessage("§8§m--------------------------------");
            for (Rank rank : Rank.values()) {
                // Skips NONE/default if desired, but showing all is fine

                player.sendMessage(
                        " " + rank.getPrefix() + " §7- " + (rank == Rank.DIVINE ? "§cOp Only" : "§eUnlock via /rank"));
            }
            player.sendMessage("§8§m--------------------------------");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!player.hasPermission("skytree.admin")) {
                player.sendMessage("§cInsufficient permissions.");
                return true;
            }

            if (args.length < 3) {
                player.sendMessage("§cUsage: /rank set <player> <rank>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cPlayer not found.");
                return true;
            }

            try {
                Rank rank = Rank.valueOf(args[2].toUpperCase());
                plugin.getRankService().setRank(target.getUniqueId(), rank);
                player.sendMessage("§a§l[Skytree] §aSuccessfully set " + target.getName() + "'s rank to "
                        + rank.getPrefix() + "§a!");
                target.sendMessage("§a§l[Skytree] §aYour rank has been updated to " + rank.getPrefix() + "§a!");

                // Log to Admin
                plugin.getAdminService().logAction(player.getUniqueId(), "RANK",
                        "Set " + target.getName() + "'s rank to " + rank.name());
            } catch (IllegalArgumentException e) {
                player.sendMessage(
                        "§cInvalid rank. Available: IOLITE, BERYL, GARNET, AMETHYST, EMERALD, DIVINE, ADMIN, CO_OWNER, OWNER");
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("list");
            if (sender.hasPermission("skytree.admin"))
                completions.add("set");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            for (Player p : Bukkit.getOnlinePlayers())
                completions.add(p.getName());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            for (Rank r : Rank.values())
                completions.add(r.name());
        }
        return completions;
    }
}
