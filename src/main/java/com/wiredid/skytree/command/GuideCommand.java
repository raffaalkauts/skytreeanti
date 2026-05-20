package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.gui.SkytreeGuide;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GuideCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final ItemRegistry itemRegistry;

    public GuideCommand(SkytreePlugin plugin, ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("get")) {
            player.getInventory().addItem(itemRegistry.getItem("skytree_guide"));
            player.sendMessage("§a[Skytree] §7You received a §2Skytree Guide§7.");
            return true;
        }

        // Default: Open GUI
        new SkytreeGuide(plugin, itemRegistry).open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull org.bukkit.command.CommandSender sender,
            @NotNull org.bukkit.command.Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("get").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
