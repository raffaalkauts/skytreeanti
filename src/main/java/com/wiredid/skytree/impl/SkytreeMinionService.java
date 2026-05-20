package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.MinionService;
import com.wiredid.skytree.minion.MinionRegistry;
import com.wiredid.skytree.minion.MinionTask;
import com.wiredid.skytree.minion.handler.*;
import com.wiredid.skytree.model.MinionData;
import com.wiredid.skytree.model.MinionSkin;
import com.wiredid.skytree.model.MinionType;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkytreeMinionService implements MinionService {

    private final SkytreePlugin plugin;
    private final Map<UUID, MinionData> minions = new ConcurrentHashMap<>();
    private final Map<UUID, ArmorStand> minionEntities = new ConcurrentHashMap<>();
    private final MinionTask aiTask;
    private final NamespacedKey minionKey;

    public SkytreeMinionService(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.minionKey = new NamespacedKey(plugin, "minion_id");
        this.aiTask = new MinionTask(plugin, this);
        
        // Register Handlers
        MinerHandler miner = new MinerHandler(plugin);
        FarmerHandler farmer = new FarmerHandler();
        SlayerHandler slayer = new SlayerHandler();
        
        MinionRegistry.register(MinionType.MINER, miner::handle);
        MinionRegistry.register(MinionType.FARMER, farmer::handle);
        MinionRegistry.register(MinionType.SLAYER, slayer::handle);
        // Add more as implemented...

        load();
        long aiInterval = plugin.getConfig().getLong("minions.ai_tick_interval_ticks", 20);
        aiTask.runTaskTimer(plugin, aiInterval, aiInterval);
    }

    private double getUpgradeBaseCost() {
        return plugin.getConfig().getDouble("minions.upgrade_base_cost", 10000.0);
    }

    private double getUpgradeCostMultiplier() {
        return plugin.getConfig().getDouble("minions.upgrade_cost_multiplier", 2.0);
    }

    @Override
    public MinionData placeMinion(UUID playerId, Location location, MinionType type) {
        MinionData data = new MinionData(playerId, null, type, location.getBlock().getLocation());
        minions.put(data.getMinionId(), data);
        spawnMinionEntity(data);
        save();
        return data;
    }

    @Override
    public boolean removeMinion(UUID minionId) {
        MinionData data = minions.remove(minionId);
        if (data != null) {
            ArmorStand as = minionEntities.remove(minionId);
            if (as != null) as.remove();
            save();
            return true;
        }
        return false;
    }

    @Override
    public boolean upgradeMinion(UUID minionId, UUID playerId) {
        MinionData data = minions.get(minionId);
        if (data == null) return false;

        double rawCost = getUpgradeBaseCost() * Math.pow(getUpgradeCostMultiplier(), data.getLevel());
        double cost = plugin.getEconomyManager() != null
                ? rawCost * plugin.getEconomyManager().getPriceMultiplier()
                : rawCost;
        if (plugin.getEconomyService().getBalance(playerId) < cost) return false;

        if (data.upgrade()) {
            plugin.getEconomyService().removeBalance(playerId, cost);
            if (plugin.getEconomyManager() != null) {
                plugin.getEconomyManager().addToReserve(cost);
            }
            updateMinionEntity(data);
            save();
            return true;
        }
        return false;
    }

    @Override
    public boolean changeSkin(UUID minionId, MinionSkin skin, UUID playerId) {
        MinionData data = minions.get(minionId);
        if (data == null) return false;
        if (skin.requiresUnlock() && !hasSkinUnlocked(playerId, skin)) return false;

        data.setSkin(skin);
        updateMinionEntity(data);
        save();
        return true;
    }

    @Override
    public MinionData getMinionData(UUID minionId) {
        return minions.get(minionId);
    }

    @Override
    public MinionData getMinionAtLocation(Location location) {
        for (MinionData data : minions.values()) {
            if (data.getLocation().equals(location.getBlock().getLocation())) return data;
        }
        return null;
    }

    @Override
    public List<MinionData> getAllMinionsByIsland(UUID islandId) {
        return minions.values().stream().filter(m -> Objects.equals(m.getIslandId(), islandId)).toList();
    }

    @Override
    public List<MinionData> getPlayerMinions(UUID playerId) {
        if (playerId == null) return new ArrayList<>(minions.values());
        return minions.values().stream().filter(m -> m.getOwnerId().equals(playerId)).toList();
    }

    @Override
    public boolean addToStorage(UUID minionId, ItemStack item) {
        MinionData data = minions.get(minionId);
        return data != null && data.addToStorage(item);
    }

    @Override
    public List<ItemStack> clearStorage(UUID minionId) {
        MinionData data = minions.get(minionId);
        if (data == null) return Collections.emptyList();
        List<ItemStack> items = new ArrayList<>(data.getStorage());
        data.setStorage(new ArrayList<>());
        save();
        return items;
    }

    @Override
    public void executeMinionAI(UUID minionId) {
        MinionData data = minions.get(minionId);
        if (data != null && data.isActive() && data.getLocation().isChunkLoaded()) {
            MinionRegistry.execute(data);
        }
    }

    @Override
    public boolean hasSkinUnlocked(UUID playerId, MinionSkin skin) {
        if (!skin.requiresUnlock()) return true;
        return plugin.getPersistenceService().loadPlayerData(playerId).getUnlockedSkins().contains(skin.name());
    }

    @Override
    public boolean unlockSkin(UUID playerId, MinionSkin skin) {
        com.wiredid.skytree.model.PlayerData data = plugin.getPersistenceService().loadPlayerData(playerId);
        if (data.getUnlockedSkins().add(skin.name())) {
            plugin.getPersistenceService().savePlayerData(data);
            return true;
        }
        return false;
    }

    @Override
    public void saveMinionData(MinionData data) {
        save();
    }

    private void spawnMinionEntity(MinionData data) {
        Location loc = data.getLocation().clone().add(0.5, 0, 0.5);
        // Clean up any existing entities at this location with same metadata
        loc.getWorld().getNearbyEntities(loc, 0.1, 0.1, 0.1).forEach(e -> {
            if (e instanceof ArmorStand && e.getPersistentDataContainer().has(minionKey, PersistentDataType.STRING)) {
                e.remove();
            }
        });

        ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        as.setSmall(true);
        as.setArms(true);
        as.setBasePlate(false);
        as.setCustomNameVisible(true);
        as.getPersistentDataContainer().set(minionKey, PersistentDataType.STRING, data.getMinionId().toString());

        minionEntities.put(data.getMinionId(), as);
        updateMinionEntity(data);
    }

    private void updateMinionEntity(MinionData data) {
        ArmorStand as = minionEntities.get(data.getMinionId());
        if (as == null || !as.isValid()) return;

        as.customName(ComponentUtil.parse("§6§l" + data.getType().name() + " §7[Lvl " + data.getLevel() + "]"));
        
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        // Skin applying logic would go here
        as.getEquipment().setHelmet(head);

        Material tool = switch (data.getType()) {
            case FARMER -> Material.DIAMOND_HOE;
            case MINER -> Material.DIAMOND_PICKAXE;
            case LUMBERJACK -> Material.DIAMOND_AXE;
            case FISHER -> Material.FISHING_ROD;
            default -> Material.AIR;
        };
        as.getEquipment().setItemInMainHand(new ItemStack(tool));
    }

    public void onDisable() {
        minionEntities.values().forEach(e -> { if (e != null) e.remove(); });
        save();
    }

    public final void save() {
        // Implementation using PersistenceService or simple File
        plugin.getPersistenceService().saveAllMinions(new ArrayList<>(minions.values()));
    }

    public final void load() {
        List<MinionData> loaded = plugin.getPersistenceService().loadAllMinions();
        if (loaded != null) {
            loaded.forEach(data -> {
                minions.put(data.getMinionId(), data);
                if (data.getLocation().isChunkLoaded()) spawnMinionEntity(data);
            });
        }
    }
}
