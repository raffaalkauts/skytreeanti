package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.CustomEnchant;
import com.wiredid.skytree.system.EnchantRegistry;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnchantCommand implements CommandExecutor, TabCompleter {

    private final EnchantRegistry registry;

    public EnchantCommand(SkytreePlugin plugin, EnchantRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§b§l§m---§r §b§lCustom Enchants §b§l§m---");
            for (CustomEnchant enchant : registry.getAllEnchants()) {
                sender.sendMessage(" §8- " + enchant.getRarity().getPrefix() + " " + enchant.getDisplayName()
                        + " §7(Max: " + enchant.getMaxLevel() + ")");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("skytree.admin.enchants")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage("§cUsage: /ce give <player> <enchant_id> <level>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            CustomEnchant enchant = registry.getEnchant(args[2]);
            if (enchant == null) {
                sender.sendMessage("§cEnchant not found.");
                return true;
            }

            int level;
            try {
                level = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid level.");
                return true;
            }

            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = book.getItemMeta();
            meta.displayName(ComponentUtil.parse("§6§lCustom Enchantment Book"));
            book.setItemMeta(meta);

            registry.applyEnchant(book, enchant, level);
            target.getInventory().addItem(book);
            sender.sendMessage("§aGave " + enchant.getDisplayName() + " " + level + " to " + target.getName());
            return true;
        }

        sender.sendMessage("§cUnknown subcommand.");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("list", "give");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return registry.getAllEnchants().stream().map(CustomEnchant::getId).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
