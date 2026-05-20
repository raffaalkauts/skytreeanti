package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SortCommand implements CommandExecutor, TabCompleter {

    public SortCommand(SkytreePlugin plugin) {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        boolean onlyHotbar = args.length > 0 && args[0].equalsIgnoreCase("hotbar");
        sortInventory(player, onlyHotbar);
        return true;
    }

    private void sortInventory(Player player, boolean onlyHotbar) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> items = new ArrayList<>();

        int startSlot = onlyHotbar ? 0 : 0;
        int endSlot = onlyHotbar ? 9 : 36;

        for (int i = startSlot; i < endSlot; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
                inv.setItem(i, null);
            }
        }

        if (items.isEmpty()) {
            player.sendMessage("§7Nothing to sort.");
            return;
        }

        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack item : items) {
            boolean found = false;
            for (ItemStack m : merged) {
                if (m.isSimilar(item)) {
                    int canAdd = m.getMaxStackSize() - m.getAmount();
                    int toAdd = Math.min(canAdd, item.getAmount());
                    m.setAmount(m.getAmount() + toAdd);
                    item.setAmount(item.getAmount() - toAdd);
                    if (item.getAmount() <= 0) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found && item.getAmount() > 0) {
                merged.add(item);
            }
        }

        merged.sort(Comparator.comparing((ItemStack is) -> is.getType().name()));

        for (int i = 0; i < Math.min(merged.size(), endSlot - startSlot); i++) {
            inv.setItem(startSlot + i, merged.get(i));
        }

        player.sendMessage("§a" + (onlyHotbar ? "Hotbar" : "Inventory") + " sorted!");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("all", "hotbar").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
