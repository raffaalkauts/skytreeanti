package com.wiredid.skytree.banking.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter for anti-abuse
 * Limits transactions to 10 per minute per player
 */
public class RateLimiter {

    private final Map<UUID, Queue<Long>> transactionTimestamps;
    private final int maxTransactionsPerMinute;
    private final long windowMs;

    public RateLimiter(int maxTransactionsPerMinute) {
        this.transactionTimestamps = new ConcurrentHashMap<>();
        this.maxTransactionsPerMinute = maxTransactionsPerMinute;
        this.windowMs = 60000; // 1 minute
    }

    /**
     * Check if player is rate limited
     */
    public boolean isRateLimited(UUID playerId) {
        Queue<Long> timestamps = transactionTimestamps.computeIfAbsent(playerId, k -> new LinkedList<>());
        long now = System.currentTimeMillis();

        // Remove old timestamps
        timestamps.removeIf(t -> now - t > windowMs);

        return timestamps.size() >= maxTransactionsPerMinute;
    }

    /**
     * Record a transaction
     */
    public void recordTransaction(UUID playerId) {
        Queue<Long> timestamps = transactionTimestamps.computeIfAbsent(playerId, k -> new LinkedList<>());
        timestamps.add(System.currentTimeMillis());
    }

    /**
     * Get remaining transactions allowed
     */
    public int getRemainingTransactions(UUID playerId) {
        Queue<Long> timestamps = transactionTimestamps.get(playerId);
        if (timestamps == null) {
            return maxTransactionsPerMinute;
        }

        long now = System.currentTimeMillis();
        timestamps.removeIf(t -> now - t > windowMs);

        return Math.max(0, maxTransactionsPerMinute - timestamps.size());
    }

    /**
     * Clear rate limit for a player (admin use)
     */
    public void clearRateLimit(UUID playerId) {
        transactionTimestamps.remove(playerId);
    }
}
