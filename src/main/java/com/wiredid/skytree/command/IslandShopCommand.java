package com.wiredid.skytree.command;

import com.wiredid.skytree.api.IslandShopService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class IslandShopCommand implements CommandExecutor {

    private final IslandShopService islandShopService;

    public IslandShopCommand(IslandShopService islandShopService) {
        this.islandShopService = islandShopService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(player, args);
                break;
            case "remove":
                handleRemove(player);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /islandshop create <buyPrice> <sellPrice>");
            return;
        }

        double buy;
        double sell;
        try {
            buy = com.wiredid.skytree.util.NumberUtil.parseSmartNumber(args[1]);
            sell = com.wiredid.skytree.util.NumberUtil.parseSmartNumber(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid prices! Use 100, 1k, 1m etc.");
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || (target.getType() != Material.CHEST && target.getType() != Material.TRAPPED_CHEST
                && target.getType() != Material.BARREL)) {
            player.sendMessage("§cYou must look at a chest, trapped chest, or barrel.");
            return;
        }

        islandShopService.createShop(player, target, buy, sell);
    }

    private void handleRemove(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage("§cYou must look at a shop.");
            return;
        }

        if (islandShopService.getShop(target).isPresent()) {
            islandShopService.removeShop(target);
            player.sendMessage("§aIsland Shop removed.");
        } else {
            player.sendMessage("§cThere is no shop at that location.");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§lIsland Shop Help");
        player.sendMessage("§e/islandshop create <buy> <sell> §7- Create a shop");
        player.sendMessage("§e/islandshop remove §7- Remove a shop");
    }
}
