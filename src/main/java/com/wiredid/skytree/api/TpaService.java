package com.wiredid.skytree.api;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaService {
    private final SkytreePlugin plugin;
    private final Map<UUID, UUID> pendingRequests = new HashMap<>(); // Target -> Requester
    private final Map<UUID, Boolean> isHereRequest = new HashMap<>(); // Target -> isHere
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public TpaService(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player requester, Player target, boolean isHere) {
        if (pendingRequests.containsKey(target.getUniqueId())) {
            requester.sendMessage("§cThat player already has a pending request.");
            return;
        }

        pendingRequests.put(target.getUniqueId(), requester.getUniqueId());
        isHereRequest.put(target.getUniqueId(), isHere);

        requester.sendMessage("§aTPA request sent to " + target.getName());

        String message = isHere ? "§e" + requester.getName() + " §arequested you to teleport TO THEM. "
                : "§e" + requester.getName() + " §arequested to teleport TO YOU. ";

        target.sendMessage(com.wiredid.skytree.util.ComponentUtil.smartParse(
                message + "\n§7Click to respond: <green><click:run_command:/tpaccept>[ACCEPT]</click> §7or <red><click:run_command:/tpdeny>[DENY]</click>"));

        // Timeout from config
        int timeoutTicks = plugin.getConfig().getInt("tpa.timeout_seconds", 60) * 20;
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingRequests.remove(target.getUniqueId());
            if (requester.isOnline())
                requester.sendMessage("§cTPA request to " + target.getName() + " timed out.");
            if (target.isOnline())
                target.sendMessage("§cTPA request from " + requester.getName() + " timed out.");
        }, timeoutTicks);
        tasks.put(target.getUniqueId(), task);
    }

    public void acceptRequest(Player target) {
        if (!pendingRequests.containsKey(target.getUniqueId())) {
            target.sendMessage("§cNo pending requests.");
            return;
        }

        UUID requesterId = pendingRequests.remove(target.getUniqueId());
        boolean isHere = isHereRequest.remove(target.getUniqueId());
        cancelTask(target.getUniqueId());

        Player requester = plugin.getServer().getPlayer(requesterId);
        if (requester != null && requester.isOnline()) {
            if (isHere) {
                target.teleport(requester.getLocation());
            } else {
                requester.teleport(target.getLocation());
            }
            requester.sendMessage("§aTeleporting...");
            target.sendMessage("§aRequest accepted.");
        } else {
            target.sendMessage("§cPlayer is no longer online.");
        }
    }

    public void denyRequest(Player target) {
        if (!pendingRequests.containsKey(target.getUniqueId())) {
            target.sendMessage("§cNo pending requests.");
            return;
        }

        UUID requesterId = pendingRequests.remove(target.getUniqueId());
        isHereRequest.remove(target.getUniqueId());
        cancelTask(target.getUniqueId());

        Player requester = plugin.getServer().getPlayer(requesterId);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage("§cTPA request denied.");
        }
        target.sendMessage("§cRequest denied.");
    }

    private void cancelTask(UUID targetId) {
        if (tasks.containsKey(targetId)) {
            tasks.get(targetId).cancel();
            tasks.remove(targetId);
        }
    }
}
