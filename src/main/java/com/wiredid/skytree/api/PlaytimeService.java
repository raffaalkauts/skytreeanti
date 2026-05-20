package com.wiredid.skytree.api;

import java.util.UUID;

public interface PlaytimeService {
    long getPlaytimeMillis(UUID player);

    String getFormattedPlaytime(UUID player);

    java.util.Map<UUID, Long> getAllPlaytimeMillis();
}
