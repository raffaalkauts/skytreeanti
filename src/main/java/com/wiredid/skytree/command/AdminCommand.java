package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AdminCommand implements CommandExecutor {

    private final SkytreePlugin plugin;

    public AdminCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("skytree.admin")) {
            player.sendMessage("§cYou don't have permission to use the admin dashboard.");
            return true;
        }

        plugin.getAdminDashboardGUI().open(player);
        return true;
    }
}
