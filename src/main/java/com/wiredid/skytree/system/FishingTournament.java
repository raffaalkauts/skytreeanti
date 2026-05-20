package com.wiredid.skytree.system;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.Bukkit;
import java.util.*;
import java.util.stream.Collectors;

public class FishingTournament extends EventManager.BaseEvent {

    public FishingTournament(SkytreePlugin plugin) {
        super(plugin, 10); // 10 minutes
    }

    @Override
    public String getName() {
        return "Fishing Tournament";
    }

    @Override
    public void start() {
        broadcast("The §bFishing Tournament §fhas started! Catch as many fish as you can in the next 10 minutes.");
    }

    @Override
    public void tick() {
        // Optional logic per tick
    }

    @Override
    public void end() {
        broadcast("The §bFishing Tournament §fhas ended!");
        List<Map.Entry<UUID, Double>> top = scores.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(3)
                .collect(Collectors.toList());

        if (top.isEmpty()) {
            broadcast("No one caught any fish. Better luck next time!");
        } else {
            for (int i = 0; i < top.size(); i++) {
                UUID pId = top.get(i).getKey();
                double score = top.get(i).getValue();
                String name = Bukkit.getOfflinePlayer(pId).getName();
                broadcast("§e#" + (i + 1) + " §f" + name + " with §b" + (int) score + " §ffish!");

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
            p.sendMessage("§aYou received §f₮ " + money + " §afor your tournament placement!");
    }

    @Override
    public String getStatus() {
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        return "§bFishing Tournament §f- §e" + (remaining / 60) + "m " + (remaining % 60) + "s";
    }

    public void onCatch(UUID playerId) {
        scores.put(playerId, scores.getOrDefault(playerId, 0.0) + 1);
    }
}
