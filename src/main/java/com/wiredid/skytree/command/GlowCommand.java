package com.wiredid.skytree.command;

import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import org.bukkit.command.TabCompleter;
import java.util.Collections;
import java.util.List;

public class GlowCommand implements CommandExecutor, TabCompleter {

    private final com.wiredid.skytree.SkytreePlugin plugin;

    public GlowCommand(com.wiredid.skytree.SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ComponentUtil.parse("§cOnly players can use this command."));
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.getRankService().hasPermission(player.getUniqueId(), "skytree.rank.glow")) {
            player.sendMessage(ComponentUtil.parse("§cYou need §d§lDIVINE §crank or higher to use this!"));
            return true;
        }

        boolean glowing = !player.isGlowing();
        player.setGlowing(glowing);

        if (glowing) {
            player.sendMessage(ComponentUtil.parse("§aGlow mode enabled!"));
        } else {
            player.sendMessage(ComponentUtil.parse("§cGlow mode disabled."));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        return Collections.emptyList();
    }
}
