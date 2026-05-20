package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.BaitType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BaitCommand implements CommandExecutor {

    private final SkytreePlugin plugin;

    public BaitCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        if (args.length == 0) {
            plugin.getBaitGUI().open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("give") && player.hasPermission("skytree.admin")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /bait give <player> <type> [amount]");
                return true;
            }

            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cPlayer not found.");
                return true;
            }

            BaitType type = BaitType.fromString(args[2]);
            if (type == null) {
                player.sendMessage("§cInvalid bait type.");
                return true;
            }

            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid amount.");
                    return true;
                }
            }

            plugin.getBaitService().giveBait(target.getUniqueId(), type, amount);
            player.sendMessage("§aGave §e" + amount + "x " + type.name() + " §abait to §f" + target.getName());
            return true;
        }

        return false;
    }
}
