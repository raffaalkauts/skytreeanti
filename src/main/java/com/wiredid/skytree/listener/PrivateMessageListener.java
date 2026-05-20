package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.PlayerData;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrivateMessageListener implements Listener {

    private static final String[] DIRECT_ALIASES = { "msg", "tell", "w", "whisper", "pm", "m", "message" };
    private static final String[] REPLY_ALIASES = { "r", "reply" };

    private final SkytreePlugin plugin;
    private final Map<UUID, UUID> lastDmPartner = new ConcurrentHashMap<>();

    public PrivateMessageListener(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player sender = event.getPlayer();
        String raw = event.getMessage();
        if (raw == null || raw.length() < 2 || raw.charAt(0) != '/') {
            return;
        }

        String[] parts = raw.substring(1).split("\\s+", 3);
        if (parts.length == 0) {
            return;
        }

        String alias = parts[0].toLowerCase();
        if (isDirectAlias(alias)) {
            handleDirectMessage(event, sender, parts);
            return;
        }

        if (isReplyAlias(alias)) {
            handleReplyMessage(event, sender, parts);
        }
    }

    private void handleDirectMessage(PlayerCommandPreprocessEvent event, Player sender, String[] parts) {
        if (parts.length < 3) {
            sender.sendMessage("§cUsage: /" + parts[0] + " <player> <message>");
            event.setCancelled(true);
            return;
        }

        Player target = Bukkit.getPlayerExact(parts[1]);
        if (target == null) {
            target = Bukkit.getPlayer(parts[1]);
        }

        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            event.setCancelled(true);
            return;
        }

        if (!canReceivePrivateMessages(target)) {
            sender.sendMessage("§c" + target.getName() + " has private messages disabled.");
            event.setCancelled(true);
            return;
        }

        sendPrivateMessage(sender, target, joinMessage(parts, 2));
        event.setCancelled(true);
    }

    private void handleReplyMessage(PlayerCommandPreprocessEvent event, Player sender, String[] parts) {
        UUID targetId = lastDmPartner.get(sender.getUniqueId());
        if (targetId == null) {
            sender.sendMessage("§cYou have nobody to reply to.");
            event.setCancelled(true);
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cThat player is no longer online.");
            event.setCancelled(true);
            return;
        }

        if (parts.length < 2) {
            sender.sendMessage("§cUsage: /" + parts[0] + " <message>");
            event.setCancelled(true);
            return;
        }

        if (!canReceivePrivateMessages(target)) {
            sender.sendMessage("§c" + target.getName() + " has private messages disabled.");
            event.setCancelled(true);
            return;
        }

        sendPrivateMessage(sender, target, joinMessage(parts, 1));
        event.setCancelled(true);
    }

    private void sendPrivateMessage(Player sender, Player target, String message) {
        String senderName = sender.getName();
        String targetName = target.getName();

        sender.sendMessage(ComponentUtil.parse("§d[PM] §fTo §b" + targetName + "§8: §f" + message));
        target.sendMessage(ComponentUtil.parse("§d[PM] §fFrom §b" + senderName + "§8: §f" + message));

        lastDmPartner.put(sender.getUniqueId(), target.getUniqueId());
        lastDmPartner.put(target.getUniqueId(), sender.getUniqueId());
    }

    private boolean canReceivePrivateMessages(Player target) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(target.getUniqueId());
        return data.getSettings().getOrDefault("pms", true);
    }

    private boolean isDirectAlias(String alias) {
        for (String value : DIRECT_ALIASES) {
            if (value.equals(alias)) {
                return true;
            }
        }
        return false;
    }

    private boolean isReplyAlias(String alias) {
        for (String value : REPLY_ALIASES) {
            if (value.equals(alias)) {
                return true;
            }
        }
        return false;
    }

    private String joinMessage(String[] parts, int startIndex) {
        if (startIndex >= parts.length) {
            return "";
        }
        if (startIndex == parts.length - 1) {
            return parts[startIndex];
        }
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < parts.length; i++) {
            if (i > startIndex) {
                builder.append(' ');
            }
            builder.append(parts[i]);
        }
        return builder.toString();
    }
}
