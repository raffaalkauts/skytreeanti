package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChatToggleCommand implements CommandExecutor {

    private final SkytreePlugin plugin;

    public ChatToggleCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        switch (label.toLowerCase()) {
            case "ic" -> {
                plugin.getChatListener().setChannel(player, "ISLAND");
                player.sendMessage("§b§l[Skytree] §fChat channel set to §bISLAND");
            }
            case "lc" -> {
                plugin.getChatListener().setChannel(player, "LOCAL");
                player.sendMessage("§e§l[Skytree] §fChat channel set to §eLOCAL");
            }
            case "gc" -> {
                plugin.getChatListener().setChannel(player, "GLOBAL");
                player.sendMessage("§a§l[Skytree] §fChat channel set to §aGLOBAL");
            }
            case "chathist" -> plugin.getChatHistoryGUI().open(player);
        }

        return true;
    }
}
