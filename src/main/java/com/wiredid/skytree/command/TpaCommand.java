package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.TpaService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TpaCommand implements CommandExecutor, TabCompleter {

    private final TpaService tpaService;

    public TpaCommand(SkytreePlugin plugin, TpaService tpaService) {
        this.tpaService = tpaService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player player = (Player) sender;

        if (label.equalsIgnoreCase("tpaccept")) {
            tpaService.acceptRequest(player);
            return true;
        }

        if (label.equalsIgnoreCase("tpdeny")) {
            tpaService.denyRequest(player);
            return true;
        }

        // Determine if this is a TPAHERE or TPA
        boolean isHere = label.equalsIgnoreCase("tpahere");

        if (args.length < 1) {
            player.sendMessage("§cUsage: /tpa <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cPlayer not found.");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§cYou cannot TPA to yourself.");
            return true;
        }

        tpaService.sendRequest(player, target, isHere);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull org.bukkit.command.CommandSender sender,
            @NotNull org.bukkit.command.Command command, @NotNull String alias, @NotNull String[] args) {
        if ((alias.equalsIgnoreCase("tpa") || alias.equalsIgnoreCase("tpahere")) && args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
