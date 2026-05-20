package com.wiredid.skytree.model;

import java.util.UUID;

/**
 * Chat reaction data
 */
public class ChatReaction {
    private UUID messageId;
    private UUID playerId;
    private String emoji;
    private long timestamp;

    public ChatReaction(UUID messageId, UUID playerId, String emoji) {
        this.messageId = messageId;
        this.playerId = playerId;
        this.emoji = emoji;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public UUID getMessageId() {
        return messageId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getEmoji() {
        return emoji;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
