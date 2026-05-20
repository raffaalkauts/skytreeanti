package com.wiredid.skytree.economy;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.AuctionHouse;
import com.wiredid.skytree.banking.persistence.BankPersistenceService;
import com.wiredid.skytree.banking.util.BankUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    private final SkytreePlugin plugin;
    private final BankPersistenceService bankPersistence;

    private double currentM2;
    private double currentPriceMultiplier = 1.0;
    private double currentDynamicRate;
    private double totalReserve;

    private double targetM2;
    private double elasticity;
    private double priceMinMultiplier;
    private double priceMaxMultiplier;
    private double baseInterestRate;
    private double minInterestRate = 0.0001;
    private double maxInterestRate = 0.05;
    private int recalcIntervalTicks;

    private final Map<String, RareItemConfig> rareItems = new HashMap<>();
    private RareItemConfig spawnerDefault;
    private RareItemConfig spawnEggDefault;

    private final File dataFile;
    private final YamlConfiguration dataConfig;

    public EconomyManager(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.bankPersistence = new BankPersistenceService(plugin);

        this.dataFile = new File(plugin.getDataFolder(), "economy_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[EconomyManager] Failed to create economy_data.yml: " + e.getMessage());
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        loadConfig();
        loadEconomyData();
        startRecalcTask();
    }

    private void loadConfig() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("economy_manager");
        if (sec == null) {
            targetM2 = 50000.0;
            elasticity = 0.5;
            priceMinMultiplier = 0.5;
            priceMaxMultiplier = 3.0;
            baseInterestRate = 0.0011;
            recalcIntervalTicks = 20;
            return;
        }

        targetM2 = sec.getDouble("target_m2", 50000.0);
        elasticity = sec.getDouble("elasticity", 0.5);
        priceMinMultiplier = sec.getDouble("price_min_multiplier", 0.5);
        priceMaxMultiplier = sec.getDouble("price_max_multiplier", 3.0);
        baseInterestRate = sec.getDouble("base_interest_rate", 0.0011);
        recalcIntervalTicks = sec.getInt("recalc_interval_ticks", 20);

        rareItems.clear();
        spawnerDefault = null;
        spawnEggDefault = null;
        ConfigurationSection rareSec = sec.getConfigurationSection("rare_items");
        if (rareSec != null) {
            for (String key : rareSec.getKeys(false)) {
                double mult = rareSec.getDouble(key + ".multiplier", 10.0);
                if (key.equals("spawner_default")) {
                    spawnerDefault = new RareItemConfig(mult);
                } else if (key.equals("spawn_egg_default")) {
                    spawnEggDefault = new RareItemConfig(mult);
                } else {
                    rareItems.put(key, new RareItemConfig(mult));
                }
            }
        }
    }

    private void loadEconomyData() {
        this.currentM2 = dataConfig.getDouble("last_m2_snapshot", targetM2);
        this.currentPriceMultiplier = dataConfig.getDouble("last_price_multiplier", 1.0);
        this.currentDynamicRate = dataConfig.getDouble("last_dynamic_rate", baseInterestRate);
        this.totalReserve = dataConfig.getDouble("total_reserve", 0.0);
    }

    private void startRecalcTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::recalculateM2,
                recalcIntervalTicks, recalcIntervalTicks);
    }

    public synchronized void recalculateM2() {
        try {
            double playerBalances = plugin.getEconomyService().getAllBalances().values()
                    .stream().mapToDouble(d -> d).sum();

            double bankDeposits = 0;
            for (UUID id : bankPersistence.getAllAccountIds()) {
                bankDeposits += BankUtil.toUSDT(bankPersistence.loadAccount(id).getBalance());
            }

            double ahLocked = 0;
            for (AuctionHouse listing : plugin.getAuctionHouseService().getActiveListings()) {
                ahLocked += listing.getTotalPrice();
            }

            double activeBonds = plugin.getInvestmentService() != null
                    ? ((com.wiredid.skytree.impl.SkytreeInvestmentService) plugin.getInvestmentService()).getTotalActiveBonds()
                    : 0;

            currentM2 = playerBalances + bankDeposits + ahLocked + activeBonds;

            if (currentM2 > 0 && targetM2 > 0) {
                double ratio = currentM2 / targetM2;
                currentPriceMultiplier = 1.0 + elasticity * (ratio - 1.0);
                currentPriceMultiplier = clamp(currentPriceMultiplier, priceMinMultiplier, priceMaxMultiplier);

                currentDynamicRate = baseInterestRate * (targetM2 / currentM2);
                currentDynamicRate = clamp(currentDynamicRate, minInterestRate, maxInterestRate);
            }

            saveEconomyData();
        } catch (Exception e) {
            plugin.getLogger().warning("[EconomyManager] Error recalculating M2: " + e.getMessage());
        }
    }

    private RareItemConfig findRareConfig(String itemId) {
        RareItemConfig exact = rareItems.get(itemId);
        if (exact != null) return exact;

        if (itemId.startsWith("spawner_") && spawnerDefault != null) {
            return spawnerDefault;
        }
        if (itemId.startsWith("spawn_egg_") && spawnEggDefault != null) {
            return spawnEggDefault;
        }
        return null;
    }

    public double getAdjustedPrice(String itemId, double basePrice) {
        RareItemConfig rare = findRareConfig(itemId);
        if (rare != null) {
            return basePrice * rare.multiplier * 10;
        }
        return basePrice * currentPriceMultiplier;
    }

    public double getAdjustedSellPrice(String itemId, double baseSell) {
        if (findRareConfig(itemId) != null) {
            return 0;
        }
        return baseSell * currentPriceMultiplier;
    }

    public synchronized void addToReserve(double amount) {
        this.totalReserve += amount;
        saveEconomyData();
    }

    public double getDynamicInterestRate() {
        return currentDynamicRate;
    }

    public double getPriceMultiplier() {
        return currentPriceMultiplier;
    }

    public double getCurrentM2() {
        return currentM2;
    }

    public double getTotalReserve() {
        return totalReserve;
    }

    public boolean isRareItem(String itemId) {
        return findRareConfig(itemId) != null;
    }

    private void saveEconomyData() {
        dataConfig.set("last_m2_snapshot", currentM2);
        dataConfig.set("last_price_multiplier", currentPriceMultiplier);
        dataConfig.set("last_dynamic_rate", currentDynamicRate);
        dataConfig.set("total_reserve", totalReserve);
        dataConfig.set("target_m2", targetM2);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[EconomyManager] Failed to save economy_data.yml: " + e.getMessage());
        }
    }

    public void onDisable() {
        recalculateM2();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class RareItemConfig {
        final double multiplier;
        RareItemConfig(double multiplier) {
            this.multiplier = multiplier;
        }
    }
}
