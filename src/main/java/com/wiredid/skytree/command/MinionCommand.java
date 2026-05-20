package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.MinionService;
import com.wiredid.skytree.model.MinionData;
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

public class MinionCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final MinionService minionService;

    public MinionCommand(SkytreePlugin plugin, MinionService minionService) {
        this.plugin = plugin;
        this.minionService = minionService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("give") && player.hasPermission("skytree.admin")) {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /minion give <farmer|miner|lumberjack|fisher|auto_sieve>");
                    return true;
                }
                String type = args[1].toLowerCase();
                ItemStack item = plugin.getItemRegistry().getItem("minion_" + type);
                if (item != null) {
                    player.getInventory().addItem(item);
                    player.sendMessage("§a[Minion] §fGave you a §e" + type + " §fminion.");
                } else {
                    player.sendMessage("§cInvalid minion type.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("collectall")) {
                collectAllMinions(player);
                return true;
            }
        }

        List<MinionData> minions = minionService.getPlayerMinions(player.getUniqueId());
        if (minions.isEmpty()) {
            player.sendMessage("§cYou have no active minions.");
            player.sendMessage("§cUsage: /minion <collectall|give>");
        } else {
            player.sendMessage("§6§lYour Minions:");
            for (MinionData m : minions) {
                player.sendMessage(" §7- §e" + m.getType().name() + " §7(Lv." + m.getLevel() + ") §7@ " +
                        m.getLocation().getBlockX() + ", " + m.getLocation().getBlockZ());
            }
        }
        return true;
    }

    private void collectAllMinions(Player player) {
        List<MinionData> playerMinions = minionService.getPlayerMinions(player.getUniqueId());
        if (playerMinions.isEmpty()) {
            player.sendMessage("§cYou don't have any active minions.");
            return;
        }

        int count = 0;
        for (MinionData data : playerMinions) {
            List<ItemStack> items = minionService.clearStorage(data.getMinionId());
            if (!items.isEmpty()) {
                for (ItemStack item : items) {
                    player.getInventory().addItem(item).values()
                            .forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));
                }
                count++;
            }
        }

        if (count > 0) {
            player.sendMessage("§a§l[Minion] §aCollected items from §e" + count + " §aminions!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        } else {
            player.sendMessage("§eNo items to collect from your minions.");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("collectall"));
            if (sender.hasPermission("skytree.admin"))
                subs.add("give");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("farmer", "miner", "lumberjack", "fisher", "auto_sieve").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
