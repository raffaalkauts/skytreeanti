package com.wiredid.skytree.system;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import java.util.*;
import java.util.stream.Collectors;

public class MobHunt extends EventManager.BaseEvent {

    private final EntityType targetType;

    public MobHunt(SkytreePlugin plugin) {
        super(plugin, 10); // 10 minutes
        EntityType[] pool = { EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER };
        this.targetType = pool[new Random().nextInt(pool.length)];
    }

    @Override
    public String getName() {
        return "Mob Hunt";
    }

    @Override
    public void start() {
        broadcast("The §cMob Hunt §fhas started! Kill as many §e" + targetType.name() + "s §fin the next 10 minutes.");
    }

    @Override
    public void tick() {
    }

    @Override
    public void end() {
        broadcast("The §cMob Hunt §fhas ended!");
        List<Map.Entry<UUID, Double>> top = scores.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(3)
                .collect(Collectors.toList());

        if (top.isEmpty()) {
            broadcast("No one hunted any mobs. Better luck next time!");
        } else {
            for (int i = 0; i < top.size(); i++) {
                UUID pId = top.get(i).getKey();
                double score = top.get(i).getValue();
                String name = Bukkit.getOfflinePlayer(pId).getName();
                broadcast("§e#" + (i + 1) + " §f" + name + " with §c" + (int) score + " §fkills!");

                // Rewards
                if (i == 0)
                    grantReward(pId, 10000);
                else if (i == 1)
                    grantReward(pId, 5000);
                else if (i == 2)
                    grantReward(pId, 2500);
            }
        }
    }

    private void grantReward(UUID playerId, double money) {
        plugin.getEconomyService().addBalance(playerId, money);
        var p = Bukkit.getPlayer(playerId);
        if (p != null)
            p.sendMessage("§aYou received §f₮ " + money + " §afor your hunt placement!");
    }

    @Override
    public String getStatus() {
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        return "§cMob Hunt (" + targetType.name() + ") §f- §e" + (remaining / 60) + "m " + (remaining % 60) + "s";
    }

    public void onKill(UUID playerId, EntityType type) {
        if (type == targetType) {
            scores.put(playerId, scores.getOrDefault(playerId, 0.0) + 1);
        }
    }
}
