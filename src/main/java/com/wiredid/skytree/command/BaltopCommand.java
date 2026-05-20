package com.wiredid.skytree.command;

import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BaltopCommand implements CommandExecutor, TabCompleter {

    private final EconomyService economyService;
    private static final int PAGE_SIZE = 10;

    public BaltopCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException ignored) {}
        }

        Map<UUID, Double> all = economyService.getAllBalances();
        List<Map.Entry<UUID, Double>> sorted = all.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            sender.sendMessage("§7No balance data available.");
            return true;
        }

        int totalPages = (sorted.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, sorted.size());

        sender.sendMessage("§6§l=== Balance Top (Page " + page + "/" + totalPages + ") ===");

        for (int i = start; i < end; i++) {
            Map.Entry<UUID, Double> entry = sorted.get(i);
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String name = player.getName() != null ? player.getName() : "Unknown";
            sender.sendMessage("§e#" + (i + 1) + " §7" + name + " - §a" + NumberUtil.formatCurrency(entry.getValue()));
        }

        if (page < totalPages) {
            sender.sendMessage("§7Next page: §e/" + label + " " + (page + 1));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        return new ArrayList<>();
    }
}
