package com.wiredid.skytree.command;

import com.wiredid.skytree.api.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.wiredid.skytree.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pay command (/pay <player> <amount>)
 */
public class PayCommand implements CommandExecutor, TabCompleter {

    private final EconomyService economy;

    public PayCommand(EconomyService economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c§l[Skytree] §cUsage: /pay <player> <amount>");
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            player.sendMessage("§c§l[Skytree] §cPlayer not found!");
            return true;
        }

        double amount;
        try {
            amount = NumberUtil.parseSmartNumber(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c§l[Skytree] §cInvalid amount! Use 100, 1k, 1m etc.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage("§c§l[Skytree] §cAmount must be positive!");
            return true;
        }

        if (economy.transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
            player.sendMessage("§a§l[Skytree] §aSent " + economy.format(amount) + " to " + target.getName());
            target.sendMessage("§a§l[Skytree] §aReceived " + economy.format(amount) + " from " + player.getName());
        } else {
            player.sendMessage("§c§l[Skytree] §cInsufficient funds!");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull org.bukkit.command.CommandSender sender,
            @NotNull org.bukkit.command.Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
