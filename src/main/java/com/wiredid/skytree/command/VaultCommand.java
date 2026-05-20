package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.PlayerData;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VaultCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;

    public VaultCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        openVault(player);
        return true;
    }

    private void openVault(Player player) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
        Inventory vault = Bukkit.createInventory(player, 54, ComponentUtil.parse("§8§lVAULT §7(Personal Storage)"));

        List<ItemStack> items = data.getVaultItems();
        for (int i = 0; i < Math.min(items.size(), 54); i++) {
            vault.setItem(i, items.get(i));
        }

        player.openInventory(vault);
        player.sendMessage("§aOpened your personal vault.");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        return new ArrayList<>();
    }
}
