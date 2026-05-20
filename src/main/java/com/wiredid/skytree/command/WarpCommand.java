package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Island;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Warp management for islands
 */
public class WarpCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;

    public WarpCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        Island island = plugin.getIslandService().getIsland(player.getUniqueId()).orElse(null);

        if (island == null) {
            player.sendMessage("§c§l[Skytree] §cYou don't have an island!");
            return true;
        }

        if (args.length == 0) {
            plugin.getWarpGUI().open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "set" -> {
                if (args.length < 2) {
                    player.sendMessage("§c§l[Skytree] §cUsage: /warp set <name>");
                    return true;
                }
                setWarp(player, island, args[1]);
            }
            case "delete", "del" -> {
                if (args.length < 2) {
                    player.sendMessage("§c§l[Skytree] §cUsage: /warp delete <name>");
                    return true;
                }
                deleteWarp(player, island, args[1]);
            }
            case "list" -> listWarps(player, island);
            default -> {
                // Teleport to warp
                teleportToWarp(player, island, args[0]);
            }
        }

        return true;
    }

    private void setWarp(Player player, Island island, String name) {
        int maxWarps = plugin.getConfig().getInt("warps.max_warps", 5);
        if (island.getWarps().getWarpCount() >= maxWarps) {
            player.sendMessage("§c§l[Skytree] §cMaximum " + maxWarps + " warps allowed!");
            player.sendMessage("§7Delete a warp with §e/warp delete <name>");
            return;
        }

        island.getWarps().setWarp(name, player.getLocation());
        plugin.getPersistenceService().saveIsland(island);

        player.sendMessage("§a§l[Skytree] §aWarp §e" + name + " §aset!");
        player.sendMessage("§7Use §e/warp " + name + " §7to teleport here");
    }

    private void deleteWarp(Player player, Island island, String name) {
        if (!island.getWarps().hasWarp(name)) {
            player.sendMessage("§c§l[Skytree] §cWarp §e" + name + " §cdoesn't exist!");
            return;
        }

        island.getWarps().removeWarp(name);
        plugin.getPersistenceService().saveIsland(island);

        player.sendMessage("§a§l[Skytree] §aWarp §e" + name + " §adeleted!");
    }

    private void teleportToWarp(Player player, Island island, String name) {
        if (!island.getWarps().hasWarp(name)) {
            player.sendMessage("§c§l[Skytree] §cWarp §e" + name + " §cdoesn't exist!");
            player.sendMessage("§7Use §e/warp list §7to see all warps");
            return;
        }

        player.teleport(island.getWarps().getWarp(name));
        player.sendMessage("§a§l[Skytree] §aTeleported to warp §e" + name + "§a!");
    }

    private void listWarps(Player player, Island island) {
        if (island.getWarps().getWarpCount() == 0) {
            player.sendMessage("§e§l[Skytree] §7No warps set!");
            player.sendMessage("§7Create one with §e/warp set <name>");
            return;
        }

        player.sendMessage("§6§l=== Island Warps ===");
        for (String warp : island.getWarps().getAllWarps().keySet()) {
            player.sendMessage("§e/warp " + warp);
        }
        player.sendMessage("§7Warps: §e" + island.getWarps().getWarpCount() + "/" + plugin.getConfig().getInt("warps.max_warps", 5));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player))
            return new ArrayList<>();

        Player player = (Player) sender;
        Island island = plugin.getIslandService().getIsland(player.getUniqueId()).orElse(null);

        if (island == null)
            return new ArrayList<>();

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(Arrays.asList("set", "delete", "list"));
            suggestions.addAll(island.getWarps().getAllWarps().keySet());
            return suggestions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            return new ArrayList<>(island.getWarps().getAllWarps().keySet());
        }

        return new ArrayList<>();
    }
}
