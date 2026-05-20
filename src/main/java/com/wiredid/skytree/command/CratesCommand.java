package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.CrateService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CratesCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final CrateService crateService;

    public CratesCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.crateService = plugin.getCrateService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length >= 4 && args[0].equalsIgnoreCase("give")) {
                handleGive(sender, args);
            } else {
                sender.sendMessage("§cThis command can only be used by players (except for 'give').");
            }
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6§l=== Skytree Crates ===");
            player.sendMessage("§e/crates open <id> §7- Open a crate with a key");
            if (player.hasPermission("skytree.admin")) {
                player.sendMessage("§c/crates give <player> <id> <amount>");
                player.sendMessage("§c/crates reload");
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "open" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /crates open <id>");
                    return true;
                }
                String id = args[1].toLowerCase();
                plugin.getCrateOpeningGUI().open(player, id);
            }
            case "give" -> {
                if (!player.hasPermission("skytree.admin")) {
                    player.sendMessage("§cNo permission.");
                    return true;
                }
                handleGive(player, args);
            }
            case "reload" -> {
                if (!player.hasPermission("skytree.admin")) {
                    player.sendMessage("§cNo permission.");
                    return true;
                }
                crateService.reload();
                player.sendMessage("§aCrates reloaded!");
            }
            default -> player.sendMessage("§cUnknown subcommand.");
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /crates give <player> <id> <amount>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }
        String id = args[2].toLowerCase();
        if (crateService.getCrate(id) == null) {
            sender.sendMessage("§cCrate '" + id + "' not found.");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount.");
            return;
        }
        crateService.giveKey(target, id, amount);
        sender.sendMessage("§aGave " + amount + "x " + id + " keys to " + target.getName());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("open"));
            if (sender.hasPermission("skytree.admin")) {
                subs.add("give");
                subs.add("reload");
            }
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("open")) {
                return crateService.getCrates().keySet().stream().filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("give")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return crateService.getCrates().keySet().stream().filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
