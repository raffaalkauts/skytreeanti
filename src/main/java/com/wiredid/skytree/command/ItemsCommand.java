package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.api.RecipeService;
import com.wiredid.skytree.api.WorthService;
import com.wiredid.skytree.gui.ItemCatalogGUI;
import com.wiredid.skytree.gui.RecipeViewerGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemsCommand implements CommandExecutor, TabCompleter {

    private final ItemCatalogGUI catalogGUI;
    private final ItemRegistry itemRegistry;
    private final RecipeService recipeService;
    private final SkytreePlugin plugin;

    public ItemsCommand(SkytreePlugin plugin, ItemRegistry itemRegistry, RecipeService recipeService, WorthService worthService) {
        this.catalogGUI = new ItemCatalogGUI(plugin, itemRegistry, worthService);
        this.itemRegistry = itemRegistry;
        this.recipeService = recipeService;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            catalogGUI.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage("§6§l/items §e- Open item catalog");
            player.sendMessage("§6§l/items list §e- List all item IDs");
            player.sendMessage("§6§l/items <id> §e- View recipe for an item");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            java.util.Set<String> ids = itemRegistry.getAllItemIds();
            player.sendMessage("§6§lItem IDs (§e" + ids.size() + "§6§l):");
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String id : ids) {
                sb.append("§7").append(id).append("§8, ");
                count++;
                if (count % 5 == 0) {
                    player.sendMessage(sb.toString());
                    sb = new StringBuilder();
                }
            }
            if (sb.length() > 0) {
                player.sendMessage(sb.toString());
            }
            return true;
        }

        // Try to show recipe for specific item
        String itemId = args[0].toLowerCase();
        ItemStack item = itemRegistry.getItem(itemId);
        if (item != null) {
            RecipeViewerGUI recipeViewer = new RecipeViewerGUI(plugin, itemRegistry, recipeService);
            recipeViewer.open(player, itemId);
        } else {
            player.sendMessage("§cUnknown item ID: " + itemId + "§7. Use §e/items list §7to see all items.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("help");
            suggestions.add("list");
            java.util.Set<String> ids = itemRegistry.getAllItemIds();
            if (ids != null) {
                suggestions.addAll(ids.stream()
                        .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList()));
            }
            return suggestions;
        }
        return new ArrayList<>();
    }
}
