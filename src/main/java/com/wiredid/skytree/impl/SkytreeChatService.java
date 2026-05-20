package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ChatService;
import com.wiredid.skytree.model.ChatMessage;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.ProfanityFilter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkytreeChatService implements ChatService {

    private final SkytreePlugin plugin;
    private final List<ChatMessage> history = new ArrayList<>();
    private static final int MAX_HISTORY = 100;

    public SkytreeChatService(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void logMessage(Player sender, Component message) {
        history.add(new ChatMessage(sender.getUniqueId(), sender.getName(), message, System.currentTimeMillis()));
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    @Override
    public List<ChatMessage> getHistory() {
        return Collections.unmodifiableList(history);
    }

    @Override
    public void clearHistory() {
        history.clear();
    }

    @Override
    public String filterProfanity(String message) {
        return ProfanityFilter.filter(message);
    }

    @Override
    public void handleMentions(Player sender, String message) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!message.toLowerCase().contains("@" + target.getName().toLowerCase())) {
                continue;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!target.isOnline()) {
                    return;
                }

                var data = plugin.getPersistenceService().loadPlayerData(target.getUniqueId());
                if (!data.getSettings().getOrDefault("actionbar", true)) {
                    return;
                }

                target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                target.sendActionBar(ComponentUtil.parse("§e§l" + sender.getName() + " §7mentioned you in chat!"));
            });
        }
    }
}
