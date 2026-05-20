package com.wiredid.skytree.command;

import com.wiredid.skytree.impl.SkytreeItemRegistry;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin command to give custom items
 * Permission: skytree.give (op level 4)
 */
public class GiveCommand implements CommandExecutor, TabCompleter {

    private final SkytreeItemRegistry itemRegistry;

    public GiveCommand(SkytreeItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check
        if (!sender.hasPermission("skytree.give")) {
            sender.sendMessage("§c§l[Skytree] §cYou don't have permission to use this command!");
            sender.sendMessage("§7Required: §eskytree.give §7(OP Level 4)");
            return true;
        }

        // Usage: /skytree_give <player> <item> [amount]
        if (args.length < 2) {
            // Check for list arg
            if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
                listAllItems(sender);
                return true;
            }
            sender.sendMessage("§c§l[Skytree] §cUsage: /skytree_give <player|@a|@p|@r> <item> [amount]");
            sender.sendMessage("§7Example: /skytree_give @a hammer_diamond 1");
            sender.sendMessage("§7Type §e/skytree_give list §7to see all items");
            return true;
        }

        // GUI
        if (args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c§l[Skytree] §cOnly players can open the GUI!");
                return true;
            }
            new com.wiredid.skytree.listener.GiveGUIListener(
                    com.wiredid.skytree.SkytreePlugin.getPlugin(com.wiredid.skytree.SkytreePlugin.class), itemRegistry)
                    .openGUI(player, 0);
            return true;
        }

        // List command alternate
        if (args[0].equalsIgnoreCase("list")) {
            listAllItems(sender);
            return true;
        }

        // Get targets using CommandUtil
        java.util.List<org.bukkit.OfflinePlayer> allTargets = com.wiredid.skytree.util.CommandUtil
                .resolveTargets(sender, args[0]);
        List<Player> targets = new ArrayList<>();

        for (org.bukkit.OfflinePlayer op : allTargets) {
            if (op.isOnline() && op.getPlayer() != null) {
                targets.add(op.getPlayer());
            } else {
                sender.sendMessage("§e[Skytree] Warning: Cannot give items to offline player: "
                        + (op.getName() != null ? op.getName() : "Unknown"));
            }
        }

        if (targets.isEmpty()) {
            sender.sendMessage("§c§l[Skytree] §cNo online players found matching: " + args[0]);
            return true;
        }

        // Get item ID
        String itemId = args[1].toLowerCase();
        ItemStack itemPrototype = itemRegistry.getItem(itemId);

        if (itemPrototype == null) {
            sender.sendMessage("§c§l[Skytree] §cInvalid item: " + itemId);
            sender.sendMessage("§7Type §e/skytree_give list §7to see all items");
            return true;
        }

        // Get amount (default 1)
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1 || amount > 64) {
                    sender.sendMessage("§c§l[Skytree] §cAmount must be between 1 and 64!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c§l[Skytree] §cInvalid amount: " + args[2]);
                return true;
            }
        }

        // Give items
        int successCount = 0;
        String displayName = itemPrototype.hasItemMeta() && itemPrototype.getItemMeta().hasDisplayName()
                ? ComponentUtil.toLegacy(itemPrototype.getItemMeta().displayName())
                : itemPrototype.getType().name().replace("_", " ");

        for (Player target : targets) {
            ItemStack itemToGive = itemPrototype.clone();
            itemToGive.setAmount(amount);
            target.getInventory().addItem(itemToGive);
            target.sendMessage("§a§l[Skytree] §aYou received §f" + amount + "x " +
                    displayName + " §afrom " + sender.getName());
            successCount++;
        }

        sender.sendMessage("§a§l[Skytree] §aGave §f" + amount + "x " + displayName +
                " §ato " + successCount + " player(s).");

        // Admin Logging
        java.util.UUID adminUUID = (sender instanceof Player player) ? player.getUniqueId() : null;
        com.wiredid.skytree.SkytreePlugin plugin = com.wiredid.skytree.SkytreePlugin
                .getPlugin(com.wiredid.skytree.SkytreePlugin.class);
        if (plugin != null) {
            plugin.getAdminService().logAction(adminUUID, "ITEMS",
                    String.format("Gave %dx %s to %s", amount, itemId, args[0]));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("skytree.give")) {
            return completions;
        }

        if (args.length == 1) {
            // Targets
            completions.add("list");
            completions.add("@a");
            completions.add("@p");
            completions.add("@r");
            completions.add("@s");
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 2) {
            // Item IDs
            completions.addAll(((SkytreeItemRegistry) itemRegistry).getAllItemIds());
        } else if (args.length == 3) {
            // Amount
            completions.add("1");
            completions.add("16");
            completions.add("32");
            completions.add("64");
        }

        // Filter
        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .sorted()
                .collect(Collectors.toList());
    }

    private void listAllItems(CommandSender sender) {
        sender.sendMessage("§6§l=== Skytree Custom Items ===");

        List<String> items = new ArrayList<>(((SkytreeItemRegistry) itemRegistry).getAllItemIds());
        Collections.sort(items);

        // Pagination or just comma separated
        StringBuilder sb = new StringBuilder();
        for (String id : items) {
            if (sb.length() > 0)
                sb.append("§7, §f");
            sb.append(id);
        }

        sender.sendMessage("§f" + sb.toString());
        sender.sendMessage("§7Total: §e" + items.size() + " items");
    }
}
