package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.BountyService;
import com.wiredid.skytree.model.Bounty;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SkytreeBountyService implements BountyService {

    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, Bounty> bounties = new HashMap<>();

    public SkytreeBountyService(SkytreePlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "bounties.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create bounties.yml!");
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    private void loadAll() {
        for (String key : config.getKeys(false)) {
            try {
                UUID target = UUID.fromString(key);
                UUID issuer = UUID.fromString(config.getString(key + ".issuer"));
                double amount = config.getDouble(key + ".amount");
                long timestamp = config.getLong(key + ".timestamp");
                bounties.put(target, new Bounty(target, issuer, amount, timestamp));
            } catch (Exception ignored) {
            }
        }
    }

    private void saveAll() {
        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }

        for (Bounty bounty : bounties.values()) {
            String path = bounty.getTarget().toString();
            config.set(path + ".issuer", bounty.getIssuer().toString());
            config.set(path + ".amount", bounty.getAmount());
            config.set(path + ".timestamp", bounty.getTimestamp());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addBounty(UUID target, UUID issuer, double amount) {
        Bounty existing = bounties.get(target);
        if (existing != null) {
            double newAmount = existing.getAmount() + amount;
            bounties.put(target, new Bounty(target, existing.getIssuer(), newAmount, System.currentTimeMillis()));
        } else {
            bounties.put(target, new Bounty(target, issuer, amount));
        }
        saveAll();
    }

    @Override
    public Optional<Bounty> getBounty(UUID target) {
        return Optional.ofNullable(bounties.get(target));
    }

    @Override
    public List<Bounty> getAllBounties() {
        return new ArrayList<>(bounties.values());
    }

    @Override
    public List<Bounty> getTopBounties(int limit) {
        return bounties.values().stream()
                .sorted((b1, b2) -> Double.compare(b2.getAmount(), b1.getAmount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasBounty(UUID target) {
        return bounties.containsKey(target);
    }

    @Override
    public void removeBounty(UUID target) {
        bounties.remove(target);
        saveAll();
    }
}
