package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.RankService;
import com.wiredid.skytree.model.PlayerData;
import com.wiredid.skytree.model.Rank;

import java.util.UUID;

public class RankServiceImpl implements RankService {

    private final SkytreePlugin plugin;

    public RankServiceImpl(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Rank getRank(UUID uuid) {
        return plugin.getPersistenceService().loadPlayerData(uuid).getRank();
    }

    @Override
    public void setRank(UUID uuid, Rank rank) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(uuid);
        data.setRank(rank);
        plugin.getPersistenceService().savePlayerData(data);
    }

    @Override
    public String getPrefix(UUID uuid) {
        return getRank(uuid).getPrefix();
    }

    @Override
    public double getMultiplier(UUID uuid) {
        return getRank(uuid).getMultiplier();
    }

    @Override
    public int getMemberBonus(UUID uuid) {
        return getRank(uuid).getMemberBonus();
    }

    @Override
    public int getSpawnerBonus(UUID uuid) {
        return getRank(uuid).getSpawnerBonus();
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        Rank rank = getRank(uuid);
        if (rank.isAtLeast(Rank.ADMIN))
            return true;

        return switch (permission.toLowerCase()) {
            case "skytree.rank.feed" -> rank.isAtLeast(Rank.BERYL);
            case "skytree.rank.heal" -> rank.isAtLeast(Rank.GARNET);
            case "skytree.rank.fly" -> rank.isAtLeast(Rank.AMETHYST);
            case "skytree.rank.nick" -> rank.isAtLeast(Rank.EMERALD);
            case "skytree.rank.glow" -> rank.isAtLeast(Rank.DIVINE);
            default -> false;
        };
    }

    @Override
    public void grantPermission(UUID uuid, String permission) {
        // Placeholder implementation
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player != null) {
            org.bukkit.permissions.PermissionAttachment attachment = player.addAttachment(plugin);
            attachment.setPermission(permission, true);
        }
    }
}
