package com.wiredid.skytree.economy;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.IslandLevelCalculator;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class JobService {

    public static final List<String> ALL_JOBS = List.of(
            "miner", "farmer", "lumberjack", "fisher", "hunter", "builder", "crafter"
    );

    public static final Map<String, String> JOB_DISPLAY = new LinkedHashMap<>();
    public static final Map<String, Material> JOB_ICONS = new LinkedHashMap<>();

    static {
        JOB_DISPLAY.put("miner", "Miner");
        JOB_DISPLAY.put("farmer", "Farmer");
        JOB_DISPLAY.put("lumberjack", "Lumberjack");
        JOB_DISPLAY.put("fisher", "Fisher");
        JOB_DISPLAY.put("hunter", "Hunter");
        JOB_DISPLAY.put("builder", "Builder");
        JOB_DISPLAY.put("crafter", "Crafter");

        JOB_ICONS.put("miner", Material.DIAMOND_PICKAXE);
        JOB_ICONS.put("farmer", Material.WHEAT);
        JOB_ICONS.put("lumberjack", Material.OAK_LOG);
        JOB_ICONS.put("fisher", Material.FISHING_ROD);
        JOB_ICONS.put("hunter", Material.BOW);
        JOB_ICONS.put("builder", Material.BRICKS);
        JOB_ICONS.put("crafter", Material.CRAFTING_TABLE);
    }

    private final SkytreePlugin plugin;
    private final JobPersistenceService persistence;

    private double basePayout;
    private double worthPercent;
    private double levelBonusPerLevel;
    private double baseXpPerAction;
    private double xpFromPayoutMultiplier;
    private double xpThresholdBase;

    private final Map<String, Double> mobValues = new HashMap<>();

    public JobService(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.persistence = new JobPersistenceService(plugin);
        loadConfig();
    }

    public void reload() {
        persistence.reload();
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("jobs");
        if (sec == null) {
            basePayout = 0.25;
            worthPercent = 0.05;
            levelBonusPerLevel = 0.05;
            baseXpPerAction = 10.0;
            xpFromPayoutMultiplier = 2.0;
            xpThresholdBase = 100.0;
            return;
        }

        basePayout = sec.getDouble("base_payout", 0.25);
        worthPercent = sec.getDouble("worth_percent", 0.05);
        levelBonusPerLevel = sec.getDouble("level_bonus_per_level", 0.05);
        baseXpPerAction = sec.getDouble("base_xp_per_action", 10.0);
        xpFromPayoutMultiplier = sec.getDouble("xp_from_payout_multiplier", 2.0);
        xpThresholdBase = sec.getDouble("xp_threshold_base", 100.0);

        mobValues.clear();
        ConfigurationSection mobSec = sec.getConfigurationSection("mob_values");
        if (mobSec != null) {
            for (String key : mobSec.getKeys(false)) {
                mobValues.put(key.toUpperCase(), mobSec.getDouble(key));
            }
        }
    }

    public JobData getJobData(UUID playerId) {
        return persistence.load(playerId);
    }

    public void saveJobData(UUID playerId, JobData data) {
        persistence.save(playerId, data);
    }

    public double getXpThreshold(int level) {
        return Math.pow(level + 1, 2) * xpThresholdBase;
    }

    public double getLevelBonus(int level) {
        return 1.0 + (level * levelBonusPerLevel);
    }

    public double calculatePayout(double blockValue, int level) {
        double payout = basePayout + (blockValue * worthPercent);
        payout *= getLevelBonus(level);
        if (plugin.getEconomyManager() != null) {
            payout *= plugin.getEconomyManager().getPriceMultiplier();
        }
        return payout;
    }

    public double calculateXp(double payout) {
        return baseXpPerAction + (payout * xpFromPayoutMultiplier);
    }

    public double getMobValue(EntityType type) {
        return mobValues.getOrDefault(type.name(), 1.0);
    }

    public double getBlockValue(Material material) {
        double val = plugin.getWorthService().getItemSellPrice(new org.bukkit.inventory.ItemStack(material));
        if (val > 0) return val;

        val = IslandLevelCalculator.getBlockValue(material);
        return Math.max(val, 0.5);
    }

    public boolean handleJobAction(Player player, String jobId, double worth) {
        UUID uuid = player.getUniqueId();
        JobData data = getJobData(uuid);
        JobData.JobProgress progress = data.getOrCreate(jobId);

        int level = progress.level;
        double payout = calculatePayout(worth, level);
        double xp = calculateXp(payout);

        progress.xp += xp;
        progress.totalEarned += payout;
        progress.actions++;

        plugin.getEconomyService().addBalance(uuid, payout);

        double threshold = getXpThreshold(level);
        while (progress.xp >= threshold) {
            progress.xp -= threshold;
            progress.level++;
            level = progress.level;
            threshold = getXpThreshold(level);
            player.sendMessage("§a§l[Jobs] §7You advanced to §e" + JOB_DISPLAY.get(jobId) + " §7Level §e"
                    + level + "§7!");
        }

        saveJobData(uuid, data);
        return true;
    }

    public void setLevel(UUID playerId, String jobId, int level) {
        JobData data = getJobData(playerId);
        JobData.JobProgress prog = data.getOrCreate(jobId);
        prog.level = level;
        prog.xp = 0;
        saveJobData(playerId, data);
    }

    public void setXp(UUID playerId, String jobId, double xp) {
        JobData data = getJobData(playerId);
        JobData.JobProgress prog = data.getOrCreate(jobId);
        prog.xp = xp;
        saveJobData(playerId, data);
    }

    public void resetPlayer(UUID playerId) {
        JobData data = getJobData(playerId);
        data.getJobs().clear();
        saveJobData(playerId, data);
    }

    public void resetPlayerJob(UUID playerId, String jobId) {
        JobData data = getJobData(playerId);
        data.getJobs().remove(jobId);
        saveJobData(playerId, data);
    }

    public Map<UUID, JobData> getTopPlayers(String jobId, int limit) {
        Map<UUID, JobData> result = new LinkedHashMap<>();
        List<UUID> allPlayers = persistence.getAllPlayerIds();
        allPlayers.sort((a, b) -> {
            JobData da = getJobData(a);
            JobData db = getJobData(b);
            int la = da.getLevel(jobId);
            int lb = db.getLevel(jobId);
            if (la != lb) return lb - la;
            return Double.compare(db.getXp(jobId), da.getXp(jobId));
        });
        for (int i = 0; i < Math.min(limit, allPlayers.size()); i++) {
            UUID id = allPlayers.get(i);
            if (getJobData(id).getLevel(jobId) > 0) {
                result.put(id, getJobData(id));
            }
        }
        return result;
    }

    public double getWorthPercent() { return worthPercent; }
    public double getBasePayout() { return basePayout; }

    public void onDisable() {
        persistence.saveAll();
    }
}
