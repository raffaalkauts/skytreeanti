package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.InvestmentService;
import com.wiredid.skytree.model.Investment;
import com.wiredid.skytree.model.InvestmentType;
import com.wiredid.skytree.model.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class SkytreeInvestmentService implements InvestmentService {

    private final SkytreePlugin plugin;
    private final Map<String, StockConfig> stocks = new HashMap<>();
    private final Map<String, BondConfig> bonds = new HashMap<>();
    private final Map<String, Double> currentPrices = new HashMap<>();
    private double totalActiveBonds;

    public SkytreeInvestmentService(SkytreePlugin plugin) {
        this.plugin = plugin;
        reload();
        startPriceUpdateTask();
    }

    @Override
    public void reload() {
        stocks.clear();
        bonds.clear();
        File file = new File(plugin.getDataFolder(), "investments.yml");
        if (!file.exists()) {
            plugin.saveResource("investments.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Load Stocks
        ConfigurationSection sSec = config.getConfigurationSection("stocks");
        if (sSec != null) {
            for (String key : sSec.getKeys(false)) {
                ConfigurationSection stock = sSec.getConfigurationSection(key);
                stocks.put(key, new StockConfig(
                        stock.getString("name"),
                        stock.getDouble("initial_price"),
                        stock.getDouble("volatility"),
                        stock.getDouble("min_price"),
                        stock.getDouble("max_price")));
                currentPrices.putIfAbsent(key, stock.getDouble("initial_price"));
            }
        }

        // Load Bonds
        ConfigurationSection bSec = config.getConfigurationSection("bonds");
        if (bSec != null) {
            for (String key : bSec.getKeys(false)) {
                ConfigurationSection bond = bSec.getConfigurationSection(key);
                bonds.put(key, new BondConfig(
                        bond.getString("name"),
                        bond.getInt("duration_min"),
                        bond.getDouble("return_multiplier"),
                        bond.getDouble("min_investment")));
            }
        }
    }

    private void startPriceUpdateTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::updateStockPrices, 1200L, 1200L); // Every
                                                                                                                     // minute
    }

    @Override
    public boolean buyStock(UUID playerId, String stockId, int shares) {
        StockConfig config = stocks.get(stockId);
        if (config == null)
            return false;

        double price = currentPrices.get(stockId);
        double cost = price * shares;

        if (plugin.getEconomyService().removeBalance(playerId, cost)) {
            PlayerData data = plugin.getPersistenceService().loadPlayerData(playerId);
            data.getInvestments().add(Investment.createStock(playerId, stockId, shares, price));
            plugin.getPersistenceService().savePlayerData(data);
            return true;
        }
        return false;
    }

    @Override
    public boolean sellStock(UUID playerId, String stockId, int shares) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(playerId);
        int owned = getPlayerShares(playerId, stockId);
        if (owned < shares)
            return false;

        double price = currentPrices.get(stockId);
        double profit = price * shares;

        // Remove shares from investments
        int toRemove = shares;
        Iterator<Investment> it = data.getInvestments().iterator();
        while (it.hasNext() && toRemove > 0) {
            Investment inv = it.next();
            if (inv.getType() == InvestmentType.STOCK && inv.getAssetId().equals(stockId) && inv.isActive()) {
                if (inv.getShares() <= toRemove) {
                    toRemove -= inv.getShares();
                    it.remove();
                } else {
                    // Split investment
                    Investment remaining = new Investment(
                            UUID.randomUUID(), playerId, InvestmentType.STOCK, stockId,
                            (inv.getShares() - toRemove) * inv.getPurchasePrice(),
                            inv.getShares() - toRemove, inv.getPurchaseTime(),
                            inv.getPurchasePrice(), 0, true);
                    it.remove();
                    data.getInvestments().add(remaining);
                    toRemove = 0;
                }
            }
        }

        plugin.getEconomyService().addBalance(playerId, profit);
        plugin.getPersistenceService().savePlayerData(data);
        return true;
    }

    @Override
    public Investment purchaseBond(UUID playerId, String bondId, double amount) {
        BondConfig config = bonds.get(bondId);
        if (config == null || amount < config.minInvestment)
            return null;

        if (plugin.getEconomyService().removeBalance(playerId, amount)) {
            PlayerData data = plugin.getPersistenceService().loadPlayerData(playerId);
            Investment inv = Investment.createBond(playerId, bondId, amount, (long) config.durationMinutes * 60 * 1000);
            data.getInvestments().add(inv);
            plugin.getPersistenceService().savePlayerData(data);
            totalActiveBonds += amount;
            return inv;
        }
        return null;
    }

    @Override
    public boolean claimBond(UUID playerId, UUID investmentId) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(playerId);
        for (Investment inv : data.getInvestments()) {
            if (inv.getInvestmentId().equals(investmentId) && inv.isActive() && inv.hasMatured()) {
                BondConfig config = bonds.get(inv.getAssetId());
                if (config == null)
                    continue;

                double payout = inv.getAmount() * config.returnMultiplier;
                plugin.getEconomyService().addBalance(playerId, payout);
                inv.setActive(false);
                totalActiveBonds -= inv.getAmount();
                data.getInvestments().remove(inv);
                plugin.getPersistenceService().savePlayerData(data);
                return true;
            }
        }
        return false;
    }

    @Override
    public double getStockPrice(String stockId) {
        return currentPrices.getOrDefault(stockId, 0.0);
    }

    @Override
    public List<Investment> getPlayerInvestments(UUID playerId) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(playerId);
        return data.getInvestments();
    }

    @Override
    public int getPlayerShares(UUID playerId, String stockId) {
        return getPlayerInvestments(playerId).stream()
                .filter(i -> i.getType() == InvestmentType.STOCK && i.getAssetId().equals(stockId) && i.isActive())
                .mapToInt(Investment::getShares)
                .sum();
    }

    @Override
    public void updateStockPrices() {
        Random r = new Random();
        for (String id : stocks.keySet()) {
            StockConfig config = stocks.get(id);
            double current = currentPrices.get(id);

            // Random walk: current * (1 + (volatility * random_normal))
            double change = 1.0 + (config.volatility * r.nextGaussian());
            double next = Math.max(config.minPrice, Math.min(config.maxPrice, current * change));

            currentPrices.put(id, next);
        }
    }

    public double getTotalActiveBonds() {
        return totalActiveBonds;
    }

    @Override
    public double calculatePortfolioValue(UUID playerId) {
        double total = 0;
        for (Investment inv : getPlayerInvestments(playerId)) {
            if (!inv.isActive())
                continue;
            if (inv.getType() == InvestmentType.STOCK) {
                total += inv.calculateValue(getStockPrice(inv.getAssetId()));
            } else {
                total += inv.getAmount();
            }
        }
        return total;
    }

    private static class StockConfig {
        double volatility, minPrice, maxPrice;

        StockConfig(String name, double initialPrice, double volatility, double minPrice, double maxPrice) {
            this.volatility = volatility;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
        }
    }

    private static class BondConfig {
        int durationMinutes;
        double returnMultiplier;
        double minInvestment;

        BondConfig(String name, int durationMinutes, double returnMultiplier, double minInvestment) {
            this.durationMinutes = durationMinutes;
            this.returnMultiplier = returnMultiplier;
            this.minInvestment = minInvestment;
        }
    }
}
