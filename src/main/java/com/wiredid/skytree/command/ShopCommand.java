package com.wiredid.skytree.command;

import com.wiredid.skytree.api.ShopService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Shop command (/shop)
 */
public class ShopCommand implements CommandExecutor, TabCompleter {

    private final ShopService shopService;

    public ShopCommand(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("skytree.admin")) {
                player.sendMessage("§cYou don't have permission to reload the shop!");
                return true;
            }
            shopService.reload();
            player.sendMessage("§a§l[Shop] §7Config reloaded and inventories rebuilt!");
            return true;
        }

        shopService.openShop(player);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("skytree.admin")) {
                completions.add("reload");
            }
        }

        return completions;
    }
}
