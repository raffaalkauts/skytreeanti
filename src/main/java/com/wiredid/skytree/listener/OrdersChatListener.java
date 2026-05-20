package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;

public class OrdersChatListener implements Listener {

    private final SkytreePlugin plugin;
    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> searchTimeouts = new java.util.HashMap<>();

    public OrdersChatListener(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void startSearch(Player player) {
        player.setMetadata("orders_search", new FixedMetadataValue(plugin, true));
        player.sendMessage(ComponentUtil.parse("§eType search query or §c'cancel'§e. §8(60s timeout)"));

        // Cancel previous timeout if exists
        org.bukkit.scheduler.BukkitTask old = searchTimeouts.remove(player.getUniqueId());
        if (old != null)
            old.cancel();

        // 60s Timeout
        org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.hasMetadata("orders_search")) {
                player.removeMetadata("orders_search", plugin);
                player.sendMessage(ComponentUtil.parse("§cSearch timed out."));
                searchTimeouts.remove(player.getUniqueId());
            }
        }, plugin.getConfig().getLong("chat.timeout_seconds", 60) * 20L);

        searchTimeouts.put(player.getUniqueId(), task);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!player.hasMetadata("orders_search"))
            return;

        event.setCancelled(true);
        player.removeMetadata("orders_search", plugin);

        org.bukkit.scheduler.BukkitTask task = searchTimeouts.remove(player.getUniqueId());
        if (task != null)
            task.cancel();

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(ComponentUtil.parse("§cSearch cancelled."));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getOrdersGUI().open(player));
            return;
        }

        // Apply search and open GUI
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getOrdersGUI().openWithSearch(player, message);
        });
    }
}
