package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.PlayerData;
import com.wiredid.skytree.model.Rank;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HomeCommand implements CommandExecutor, TabCompleter {
    private final SkytreePlugin plugin;

    public HomeCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00A7cOnly players can use this command!");
            return true;
        }
        Player player = (Player) sender;
        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());

        if (label.equalsIgnoreCase("sethome")) {
            String homeName = args.length > 0 ? args[0].toLowerCase() : "home";
            int maxHomes = plugin.getConfig().getInt("homes.max_homes", 10);

            if (data.getHomes().size() >= maxHomes && !data.getHomes().containsKey(homeName)) {
                player.sendMessage(ComponentUtil.parse("\u00A7cYou have reached the maximum of " + maxHomes + " homes!"));
                return true;
            }

            data.addHome(homeName, player.getLocation());
            plugin.getPersistenceService().savePlayerData(data);
            player.sendMessage(ComponentUtil.parse("\u00A7aHome '\u00A7e" + homeName + "\u00A7a' set at your current location!"));
            return true;

        } else if (label.equalsIgnoreCase("delhome")) {
            if (args.length == 0) {
                player.sendMessage(ComponentUtil.parse("\u00A7cUsage: /delhome <name>"));
                return true;
            }
            String homeName = args[0].toLowerCase();

            if (!data.getHomes().containsKey(homeName)) {
                player.sendMessage(ComponentUtil.parse("\u00A7cHome '\u00A7e" + homeName + "\u00A7c' not found!"));
                return true;
            }

            data.removeHome(homeName);
            plugin.getPersistenceService().savePlayerData(data);
            player.sendMessage(ComponentUtil.parse("\u00A7aHome '\u00A7e" + homeName + "\u00A7a' deleted!"));
            return true;

        } else if (label.equalsIgnoreCase("home")) {
            if (args.length == 0) {
                openOrTeleportOwnHomes(player, data);
                return true;
            }

            String sub = args[0].toLowerCase();
            if (sub.equals("list") || sub.equals("gui")) {
                plugin.getHomeGUI().open(player);
                return true;
            }

            if (data.getHomes().containsKey(sub)) {
                player.teleport(data.getHome(sub));
                player.sendMessage(ComponentUtil.parse("\u00A7aTeleported to '\u00A7e" + sub + "\u00A7a'!"));
                return true;
            }

            if (args.length == 1 && canViewOtherPlayersHomes(player)) {
                OfflinePlayer target = resolveTarget(args[0]);
                if (target == null) {
                    player.sendMessage(ComponentUtil.parse("\u00A7cPlayer '\u00A7e" + args[0] + "\u00A7c' could not be found!"));
                    return true;
                }

                String targetName = target.getName() != null ? target.getName() : args[0];
                PlayerData targetData = plugin.getPersistenceService().loadPlayerData(target.getUniqueId());
                if (targetData.getHomes().isEmpty()) {
                    player.sendMessage(ComponentUtil.parse("\u00A7cPlayer '\u00A7e" + targetName + "\u00A7c' has no saved homes!"));
                    return true;
                }

                plugin.getHomeGUI().open(player, target.getUniqueId(), targetName);
            } else {
                player.sendMessage(ComponentUtil.parse("\u00A7cHome '\u00A7e" + sub + "\u00A7c' not found!"));
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player))
            return Collections.emptyList();
        Player player = (Player) sender;
        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());

        if (label.equalsIgnoreCase("delhome") || label.equalsIgnoreCase("home")) {
            if (args.length == 1) {
                List<String> list = new ArrayList<>();
                if (label.equalsIgnoreCase("home") && canViewOtherPlayersHomes(player)) {
                    list.addAll(getKnownPlayerNames());
                } else {
                    list.addAll(data.getHomes().keySet());
                    if (label.equalsIgnoreCase("home")) {
                        list.add("list");
                    }
                }
                return list.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private void openOrTeleportOwnHomes(Player player, PlayerData data) {
        if (data.getHomes().containsKey("home") && data.getHomes().size() == 1) {
            player.teleport(data.getHome("home"));
            player.sendMessage(ComponentUtil.parse("\u00A7aTeleported to home!"));
        } else if (data.getHomes().isEmpty()) {
            player.sendMessage(ComponentUtil.parse("\u00A7cYou have no homes set. Use \u00A7e/sethome [name]"));
        } else {
            plugin.getHomeGUI().open(player);
        }
    }

    private boolean canViewOtherPlayersHomes(Player player) {
        Rank rank = plugin.getRankService().getRank(player.getUniqueId());
        return rank == Rank.CO_OWNER || rank == Rank.DEVELOPER || rank == Rank.OWNER;
    }

    private OfflinePlayer resolveTarget(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return offline;
        }
        return null;
    }

    private List<String> getKnownPlayerNames() {
        Set<String> names = new LinkedHashSet<>();
        Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .forEach(names::add);
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null) {
                names.add(offlinePlayer.getName());
            }
        }
        names.add("list");
        return new ArrayList<>(names);
    }
}
