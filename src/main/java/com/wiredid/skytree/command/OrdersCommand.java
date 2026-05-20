package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.gui.OrdersGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OrdersCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;

    public OrdersCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            plugin.getOrdersGUI().open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "view":
            case "my":
                plugin.getOrdersGUI().open(player, OrdersGUI.ViewMode.MY_ORDERS);
                break;
            case "help":
                sendHelp(player);
                break;
            default:
                player.sendMessage("§c§l[Market] §7Unknown subcommand. Use §e/orders help");
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(" ");
        player.sendMessage("§b§lMARKET ORDERS HELP");
        player.sendMessage("§8§m------------------------");
        player.sendMessage("§e/orders §7- Open the order market");
        player.sendMessage("§e/orders view §7- View your active orders");
        player.sendMessage("§8§m------------------------");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("view", "my", "help").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
