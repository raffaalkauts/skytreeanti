package com.wiredid.skytree.api;

import com.wiredid.skytree.model.Rank;
import java.util.UUID;

public interface RankService {
    Rank getRank(UUID uuid);

    void setRank(UUID uuid, Rank rank);

    String getPrefix(UUID uuid);

    double getMultiplier(UUID uuid);

    int getMemberBonus(UUID uuid);

    int getSpawnerBonus(UUID uuid);

    boolean hasPermission(UUID uuid, String permission);

    void grantPermission(UUID uuid, String permission);
}
