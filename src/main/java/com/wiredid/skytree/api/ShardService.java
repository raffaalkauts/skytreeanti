package com.wiredid.skytree.api;

import java.util.UUID;

public interface ShardService {
    void addShards(UUID player, int amount);

    void removeShards(UUID player, int amount);

    void setShards(UUID player, int amount);

    int getShards(UUID player);

    boolean hasShards(UUID player, int amount);

    java.util.Map<UUID, Integer> getAllShards();
}
