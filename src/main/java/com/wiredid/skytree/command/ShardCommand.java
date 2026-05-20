package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ShardService;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.StringUtil;

public class ShardCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final ShardService shardService;

    public ShardCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.shardService = plugin.getShardService();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> options = new ArrayList<>();
            options.add("shop");
            options.add("balance");

            if (sender.hasPermission("skytree.admin")) {
                options.add("give");
                options.add("set");
                options.add("take");
            }

            StringUtil.copyPartialMatches(args[0], options, completions);
            return completions;
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set")
                    || args[0].equalsIgnoreCase("take")) {
                return null; // Return null to use player list
            }
        }
        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ComponentUtil.parse("§cOnly players can use this command."));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            plugin.getShardShopGUI().open(player);
            return true;
        }

        String action = args[0];
        if (action.equalsIgnoreCase("shop") || action.equalsIgnoreCase("market")) {
            plugin.getShardShopGUI().open(player);
            return true;
        }

        if (action.equalsIgnoreCase("give") || action.equalsIgnoreCase("set") || action.equalsIgnoreCase("take")) {
            if (!player.hasPermission("skytree.admin")) {
                player.sendMessage(ComponentUtil.parse("§cNo permission."));
                return true;
            }
            handleAdmin(player, action, args);
            return true;
        }

        if (action.equalsIgnoreCase("balance") || action.equalsIgnoreCase("bal")) {
            long shards = shardService.getShards(player.getUniqueId());
            player.sendMessage(ComponentUtil.parse("§eYour Shards: §b" + String.format("%,d", shards)));
            return true;
        }

        player.sendMessage(ComponentUtil.parse("§cUsage: /shards [shop|balance]"));
        return true;
    }

    private void handleAdmin(Player player, String action, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ComponentUtil.parse("§cUsage: /shards " + action + " <player> <amount>"));
            return;
        }

        String targetName = args[1];

        // Prevent selector crashes
        if (targetName.startsWith("@")) {
            player.sendMessage(ComponentUtil.parse("§cSelectors are not supported."));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ComponentUtil.parse("§cPlayer not found."));
            return;
        }

        try {
            int amount = (int) NumberUtil.parseSmartNumber(args[2]);

            if (action.equalsIgnoreCase("give")) {
                shardService.addShards(target.getUniqueId(), amount);
                player.sendMessage(ComponentUtil.parse("§aGave §e" + amount + " §ashards to §e" + target.getName()));
                if (target.isOnline()) {
                    ((Player) target).sendMessage(ComponentUtil.parse("§aReceived §e" + amount + " §ashards."));
                }
            } else if (action.equalsIgnoreCase("set")) {
                shardService.setShards(target.getUniqueId(), amount);
                player.sendMessage(ComponentUtil.parse("§aSet §e" + target.getName() + "'s §ashards to §e" + amount));
                if (target.isOnline()) {
                    ((Player) target).sendMessage(ComponentUtil.parse("§aShards set to §e" + amount));
                }
            } else if (action.equalsIgnoreCase("take")) {
                shardService.removeShards(target.getUniqueId(), amount);
                player.sendMessage(ComponentUtil.parse("§aTook §e" + amount + " §ashards from §e" + target.getName()));
                if (target.isOnline()) {
                    ((Player) target).sendMessage(ComponentUtil.parse("§cLost §e" + amount + " §cshards."));
                }
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ComponentUtil.parse("§cInvalid amount."));
        }
    }
}
