package com.wiredid.skytree.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Hub command to return to overworld
 */
public class HubCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        // Get overworld
        World overworld = Bukkit.getWorlds().get(0); // First world is usually overworld

        if (overworld == null) {
            player.sendMessage("§c§l[Skytree] §cOverworld not found!");
            return true;
        }

        // Teleport to spawn
        Location spawn = overworld.getSpawnLocation();
        player.teleport(spawn);
        player.sendMessage("§a§l[Skytree] §aTeleported to hub!");

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        return new ArrayList<>();
    }
}
