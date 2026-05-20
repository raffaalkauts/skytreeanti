package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.fishing.EnchantService;
import com.wiredid.skytree.fishing.FishingModels;
import com.wiredid.skytree.fishing.FishingService;
import com.wiredid.skytree.fishing.gui.ActiveRodGUI;
import com.wiredid.skytree.fishing.gui.EnchantGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RodCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final FishingService fishingService;
    private final com.wiredid.skytree.fishing.RodStorage rodStorage;
    private final EnchantService enchantService;

    public RodCommand(SkytreePlugin plugin, FishingService fishingService,
            com.wiredid.skytree.fishing.RodStorage rodStorage, EnchantService enchantService) {
        this.plugin = plugin;
        this.fishingService = fishingService;
        this.rodStorage = rodStorage;
        this.enchantService = enchantService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            // Open Active Rod GUI
            new ActiveRodGUI(plugin, rodStorage).open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "enchant":
                new EnchantGUI(plugin, enchantService).open(player);
                break;
            case "exchange":
                new com.wiredid.skytree.fishing.gui.RodExchangeGUI(plugin, fishingService).open(player);
                break;
            case "give":
                if (!player.hasPermission("skytree.admin")) {
                    player.sendMessage("§cNo permission.");
                    return true;
                }
                handleGive(player, args);
                break;
            default:
                player.sendMessage("§cUnknown subcommand. Use: enchant, exchange, give");
        }

        return true;
    }

    private void handleGive(Player player, String[] args) {
        // /rod give <player> <tier>
        if (args.length < 3) {
            player.sendMessage("§cUsage: /rod give <player> <tier>");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found.");
            return;
        }

        try {
            FishingModels.RodTier tier = FishingModels.RodTier.valueOf(args[2].toUpperCase());
            ItemStack rod = fishingService.createRod(tier);
            java.util.Map<Integer, ItemStack> left = target.getInventory().addItem(rod);
            if (!left.isEmpty()) {
                left.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
                player.sendMessage("§e[Skytree] §6Target inventory full, rod dropped at their feet.");
            }
            player.sendMessage("§aGiven " + tier.name() + " to " + target.getName());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid tier. Options: BASIC, ADVANCED, MYTHIC, RELIC");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("enchant", "exchange", "give").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.stream(FishingModels.RodTier.values())
                    .map(Enum::name)
                    .filter(s -> s.startsWith(args[2].toUpperCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
