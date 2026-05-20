package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.model.TrustLevel;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.stream.Collectors;

/**
 * Commands for managing island trust
 */
public class TrustCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;

    public TrustCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Island island = plugin.getIslandService().getIsland(player.getUniqueId()).orElse(null);
        if (island == null) {
            player.sendMessage("§c§l[Skytree] §cYou don't have an island to manage trust!");
            return true;
        }

        if (!island.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("§c§l[Skytree] §cOnly the island owner can manage trust.");
            return true;
        }

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "trust" -> handleTrust(player, island, args);
            case "untrust" -> handleUntrust(player, island, args);
            case "trustlist" -> plugin.getTrustGUI().open(player, island);
        }

        return true;
    }

    private void handleTrust(Player player, Island island, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§c§l[Skytree] §cUsage: /trust <player> [level]");
            player.sendMessage("§7Levels: VISITOR, BUILDER, MODERATOR, CO_OWNER");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§c§l[Skytree] §cPlayer not found or not online.");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§c§l[Skytree] §cYou cannot trust yourself!");
            return;
        }

        TrustLevel level = TrustLevel.BUILDER;
        if (args.length >= 2) {
            try {
                level = TrustLevel.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage("§c§l[Skytree] §cInvalid trust level! Using BUILDER.");
            }
        }

        plugin.getIslandService().trustPlayer(island, target.getUniqueId(), level);
        player.sendMessage("§a§l[Skytree] §aTrusted " + target.getName() + " with level §e" + level.name());
        target.sendMessage("§a§l[Skytree] §aYou are now trusted on " + player.getName() + "'s island with level §e"
                + level.name());
    }

    private void handleUntrust(Player player, Island island, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§c§l[Skytree] §cUsage: /untrust <player>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!island.getTrustedPlayers().containsKey(target.getUniqueId())) {
            player.sendMessage("§c§l[Skytree] §cThat player is not trusted on your island.");
            return;
        }

        plugin.getIslandService().untrustPlayer(island, target.getUniqueId());
        player.sendMessage("§c§l[Skytree] §cRemoved trust for " + target.getName());

        if (target.isOnline()) {
            target.getPlayer()
                    .sendMessage("§c§l[Skytree] §cYou are no longer trusted on " + player.getName() + "'s island.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player))
            return new ArrayList<>();

        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && command.getName().equalsIgnoreCase("trust")) {
            return Arrays.stream(TrustLevel.values())
                    .map(Enum::name)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
