package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
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

public class FishCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final com.wiredid.skytree.fishing.gui.FishStorageGUI fishStorageGUI;

    public FishCommand(SkytreePlugin plugin,
            com.wiredid.skytree.fishing.gui.FishStorageGUI fishStorageGUI) {
        this.plugin = plugin;
        this.fishStorageGUI = fishStorageGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            fishStorageGUI.open(player, 0);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell":
                plugin.getFishSellGUI().open(player);
                break;
            case "storage":
                fishStorageGUI.open(player, 0);
                break;
            case "shop":
                plugin.getFishShopGUI().open(player);
                break;
            case "guide":
                plugin.getFishPriceGuideGUI().open(player);
                break;
            default:
                fishStorageGUI.open(player, 0);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("sell", "storage", "shop", "guide").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
