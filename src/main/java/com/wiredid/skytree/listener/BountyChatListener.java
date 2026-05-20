package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class BountyChatListener implements Listener {

    private final SkytreePlugin plugin;

    public BountyChatListener(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isWaitingForBountyItem(player))
            return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (message.equalsIgnoreCase("cancel")) {
            plugin.stopBountyCreation(player);
            player.sendMessage("§c§l[Bounty] §7Creation cancelled.");
            return;
        }

        // 1. Try exact Custom Item match first
        if (plugin.getItemRegistry().getItem(message) != null) {
            final String finalId = message; // Case sensitive for custom items usually? Or assume lowercase.
            // Let's assume registry handles it or we use raw input.
            // Actually, existing code used toLowerCase().
            plugin.stopBountyCreation(player);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getOrderSetupGUI().open(player, finalId);
            });
            return;
        }

        // 2. Try exact Material match
        String upper = message.toUpperCase();
        Material exactMat = Material.matchMaterial(upper);
        if (exactMat != null) {
            plugin.stopBountyCreation(player);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getOrderSetupGUI().open(player, exactMat.name());
            });
            return;
        }

        // 3. Fuzzy Search for Materials
        // Count matches first
        long matchCount = java.util.Arrays.stream(Material.values())
                .filter(m -> m.isItem() && !m.isAir() && !m.isLegacy())
                .filter(m -> m.name().contains(upper))
                .count();

        if (matchCount == 1) {
            // Auto select single match
            Material match = java.util.Arrays.stream(Material.values())
                    .filter(m -> m.isItem() && !m.isAir() && !m.isLegacy())
                    .filter(m -> m.name().contains(upper))
                    .findFirst().orElse(null);

            if (match != null) {
                plugin.stopBountyCreation(player);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getOrderSetupGUI().open(player, match.name());
                });
                return;
            }
        } else if (matchCount > 1) {
            // Multiple matches -> Open Selection GUI
            plugin.stopBountyCreation(player);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                new com.wiredid.skytree.gui.MaterialSelectorGUI(plugin).openSearchResults(player, upper);
            });
            return;
        }

        // 4. No matches
        player.sendMessage("§c§l[Bounty] §7No items found matching: §e" + message);
        player.sendMessage("§7Please try again or type 'cancel'.");
    }
}
