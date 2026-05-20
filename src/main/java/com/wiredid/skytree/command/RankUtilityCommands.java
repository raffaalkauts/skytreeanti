package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RankUtilityCommands implements CommandExecutor, org.bukkit.command.TabCompleter {

    private final SkytreePlugin plugin;

    public RankUtilityCommands(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player))
            return true;

        String cmdName = command.getName().toLowerCase();
        switch (cmdName) {
            case "feed" -> {
                if (plugin.getRankService().hasPermission(player.getUniqueId(), "skytree.rank.feed")) {
                    player.setFoodLevel(20);
                    player.setSaturation(20f);
                    player.sendMessage("§a§l[Skytree] §aYou have been fed!");
                } else {
                    player.sendMessage("§cYou need §b§lBERYL §crank or higher to use this!");
                }
            }
            case "heal" -> {
                if (plugin.getRankService().hasPermission(player.getUniqueId(), "skytree.rank.heal")) {
                    var maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealthAttr != null) {
                        player.setHealth(maxHealthAttr.getValue());
                    }
                    player.setFoodLevel(20);
                    player.sendMessage("§a§l[Skytree] §aYou have been healed!");
                } else {
                    player.sendMessage("§cYou need §c§lGARNET §crank or higher to use this!");
                }
            }
            case "fly" -> {
                if (plugin.getRankService().hasPermission(player.getUniqueId(), "skytree.rank.fly")) {
                    boolean flying = !player.getAllowFlight();
                    player.setAllowFlight(flying);
                    player.setFlying(flying);
                    if (flying) {
                        player.setMetadata("skytree_fly", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    } else {
                        player.removeMetadata("skytree_fly", plugin);
                    }
                    player.sendMessage("§a§l[Skytree] §aFlight " + (flying ? "enabled" : "disabled") + "!");
                } else {
                    player.sendMessage("§cYou need §5§lAMETHYST §crank or higher to use this!");
                }
            }
            case "nick" -> {
                if (plugin.getRankService().hasPermission(player.getUniqueId(), "skytree.rank.nick")) {
                    if (args.length == 0) {
                        player.sendMessage("§cUsage: /nick <name|off>");
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("off")) {
                        player.displayName(ComponentUtil.parse(player.getName()));
                        player.sendMessage("§a§l[Skytree] §aNickname removed!");
                    } else {
                        player.displayName(ComponentUtil.parse(args[0]));
                        player.sendMessage("§a§l[Skytree] §aNickname set to " + args[0] + "!");
                    }
                } else {
                    player.sendMessage("§cYou need §a§lEMERALD §crank or higher to use this!");
                }
            }
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (label.equalsIgnoreCase("nick")) {
            if (args.length == 1) {
                return java.util.List.of("off");
            }
        }
        return java.util.Collections.emptyList();
    }
}
