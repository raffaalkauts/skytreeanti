package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.listener.ChatListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChannelCommand implements CommandExecutor {

    private final ChatListener listener;

    public ChannelCommand(SkytreePlugin plugin, ChatListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            String current = listener.getChannel(player);
            player.sendMessage("§aCurrent Channel: §e" + current);
            player.sendMessage("§7Usage: /channel <global|island|local>");
            return true;
        }

        String target = args[0].toUpperCase();
        if (target.startsWith("G"))
            target = "GLOBAL";
        else if (target.startsWith("I"))
            target = "ISLAND";
        else if (target.startsWith("L"))
            target = "LOCAL";
        else {
            player.sendMessage("§cInvalid channel. Use: global, island, local");
            return true;
        }

        listener.setChannel(player, target);
        player.sendMessage("§aSwitched chat to: §e" + target);

        if ("ISLAND".equals(target)) {
            player.sendMessage("§7Tip: You can allow use '§b@message§7' to chat here instantly.");
        } else if ("GLOBAL".equals(target)) {
            player.sendMessage("§7Tip: You can allow use '§b!message§7' to chat here instantly.");
        }

        return true;
    }
}
