package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.AuctionHouseService;
import com.wiredid.skytree.gui.AuctionHouseGUI;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.List;

public class AuctionHouseCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final AuctionHouseService service;

    public AuctionHouseCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.service = plugin.getAuctionHouseService();
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
            plugin.getAuctionHouseGUI().open(player, AuctionHouseGUI.ViewType.LISTINGS, 0,
                    AuctionHouseGUI.SortType.NEWEST);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "sell":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /ah sell <price>");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage("§cYou must hold an item to sell it.");
                    return true;
                }

                double price;
                try {
                    price = NumberUtil.parseSmartNumber(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid price format. Use 100, 1k, 1m etc.");
                    return true;
                }

                if (price <= 0) {
                    player.sendMessage("§cPrice must be positive.");
                    return true;
                }

                // Default 48h
                long duration = 48 * 3600 * 1000L;

                if (service.createListing(player, item, price, duration) != null) {
                    player.sendMessage("§aItem listed in Auction House for " + NumberUtil.formatCurrency(price) + "!");
                } else {
                    player.sendMessage("§cFailed to list item. (Unknown error)");
                }
                break;

            case "listings":
            case "selling":
                plugin.getAuctionHouseGUI().open(player, AuctionHouseGUI.ViewType.MY_LISTINGS, 0,
                        AuctionHouseGUI.SortType.NEWEST);
                break;

            case "orders":
                plugin.getAuctionHouseGUI().open(player, AuctionHouseGUI.ViewType.ORDERS, 0,
                        AuctionHouseGUI.SortType.NEWEST);
                break;

            case "help":
            default:
                player.sendMessage("§6Auction House Help:");
                player.sendMessage("§e/ah §7- Open Auction House");
                player.sendMessage("§e/ah sell <price> §7- Sell held item");
                player.sendMessage("§e/ah listings §7- View your listings");
                player.sendMessage("§e/ah orders §7- View Buy Orders");
                break;
        }

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("sell");
            options.add("listings");
            options.add("orders");
            options.add("help");
            return options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            return java.util.Arrays.asList("10", "100", "1000");
        }
        return new ArrayList<>();
    }
}
