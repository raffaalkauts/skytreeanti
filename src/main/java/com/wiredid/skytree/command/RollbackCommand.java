package com.wiredid.skytree.command;

import com.wiredid.skytree.api.ActionLogger;
import com.wiredid.skytree.api.IslandService;
import com.wiredid.skytree.model.Island;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command for rolling back island actions
 */
public class RollbackCommand implements CommandExecutor, TabCompleter {

    private final IslandService islandService;
    private final ActionLogger logger;

    public RollbackCommand(IslandService islandService, ActionLogger logger) {
        this.islandService = islandService;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Island island = islandService.getIsland(player.getUniqueId()).orElse(null);
        if (island == null) {
            player.sendMessage("§c§l[Skytree] §cYou don't have an island!");
            return true;
        }

        // Only owner can rollback
        if (!island.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("§c§l[Skytree] §cOnly the island owner can perform rollbacks.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§c§l[Skytree] §cUsage: /is rollback <count>");
            return true;
        }

        try {
            int count = Integer.parseInt(args[0]);
            if (count <= 0 || count > 100) {
                player.sendMessage("§c§l[Skytree] §cRollback count must be between 1 and 100.");
                return true;
            }

            logger.rollback(island.getId(), count);
            player.sendMessage("§a§l[Skytree] §aSuccessfully rolled back §e" + count + " §aactions!");

        } catch (NumberFormatException e) {
            player.sendMessage("§c§l[Skytree] §cInvalid number.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("10", "20", "50", "100");
        }
        return new ArrayList<>();
    }
}
