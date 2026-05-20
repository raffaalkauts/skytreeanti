package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;

import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import org.bukkit.command.TabCompleter;
import java.util.Collections;
import java.util.List;

public class TagCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;

    public TagCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ComponentUtil.parse("§cOnly players can use this command."));
            return true;
        }

        plugin.getTagGUI().open((Player) sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        return Collections.emptyList();
    }
}
