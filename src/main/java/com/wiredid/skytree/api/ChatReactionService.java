package com.wiredid.skytree.api;

import com.wiredid.skytree.model.ChatMessage;
import com.wiredid.skytree.model.ChatReaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing chat reactions and message history
 */
public interface ChatReactionService {

    /**
     * Add reaction to a message
     * 
     * @param messageId Message UUID
     * @param playerId  Player adding the reaction
     * @param emoji     Emoji to add
     * @return true if added
     */
    boolean addReaction(UUID messageId, UUID playerId, String emoji);

    /**
     * Remove reaction from a message
     * 
     * @param messageId Message UUID
     * @param playerId  Player removing the reaction
     * @return true if removed
     */
    boolean removeReaction(UUID messageId, UUID playerId);

    /**
     * Get all reactions for a message
     * 
     * @param messageId Message UUID
     * @return List of reactions
     */
    List<ChatReaction> getReactions(UUID messageId);

    /**
     * Get reaction counts for a message
     * 
     * @param messageId Message UUID
     * @return Map of emoji to count
     */
    Map<String, Integer> getReactionCounts(UUID messageId);

    /**
     * Store a chat message in history
     * 
     * @param message ChatMessage to store
     */
    void storeMessage(ChatMessage message);

    /**
     * Get message history for a player
     * 
     * @param playerId Player UUID
     * @param count    Number of messages to retrieve
     * @return List of messages (newest first)
     */
    List<ChatMessage> getMessageHistory(UUID playerId, int count);

    /**
     * Get message history for a channel
     * 
     * @param channel Channel name
     * @param count   Number of messages
     * @return List of messages
     */
    List<ChatMessage> getChannelHistory(String channel, int count);

    /**
     * Get a specific message
     * 
     * @param messageId Message UUID
     * @return ChatMessage or null
     */
    ChatMessage getMessage(UUID messageId);

    /**
     * Clear old messages (called by scheduled task)
     * 
     * @param olderThanMs Messages older than this (in ms) will be deleted
     */
    void clearOldMessages(long olderThanMs);

    /**
     * Check if emoji is allowed
     * 
     * @param emoji Emoji to check
     * @return true if allowed
     */
    boolean isEmojiAllowed(String emoji);
}
