package com.wiredid.skytree.api;

import com.wiredid.skytree.model.ChatMessage;
import org.bukkit.entity.Player;
import java.util.List;

public interface ChatService {
    void logMessage(Player sender, net.kyori.adventure.text.Component message);

    List<ChatMessage> getHistory();

    void clearHistory();

    String filterProfanity(String message);

    void handleMentions(Player sender, String message);
}
