package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.BaitService;
import com.wiredid.skytree.model.BaitData;
import com.wiredid.skytree.model.BaitType;
import com.wiredid.skytree.model.PlayerData;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.UUID;

public class SkytreeBaitService implements BaitService {

    private final SkytreePlugin plugin;
    private YamlConfiguration config;

    public SkytreeBaitService(SkytreePlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    @Override
    public void reload() {
        loadConfig();
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "baits.yml");
        if (!file.exists()) {
            plugin.saveResource("baits.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public boolean equipBait(UUID playerId, BaitType type, int quantity) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(playerId);
        if (data == null)
            return false;

        BaitData active = data.getActiveBait();
        if (active != null && active.getType() == type) {
            active.setQuantity(active.getQuantity() + quantity);
        } else {
            data.setActiveBait(new BaitData(type, quantity));
        }

        plugin.getPersistenceService().savePlayerData(data);
        return true;
    }

    @Override
    public boolean consumeBait(UUID playerId) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(playerId);
        if (data == null)
            return false;

        BaitData active = data.getActiveBait();
        if (active == null || active.isDepleted()) {
            data.setActiveBait(null);
            return false;
        }

        boolean consumed = active.consume();
        if (active.isDepleted()) {
            data.setActiveBait(null);
        }

        plugin.getPersistenceService().savePlayerData(data);
        return consumed;
    }

    @Override
    public BaitData getActiveBait(UUID playerId) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(playerId);
        return (data != null) ? data.getActiveBait() : null;
    }

    @Override
    public void removeBait(UUID playerId) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(playerId);
        if (data != null) {
            data.setActiveBait(null);
            plugin.getPersistenceService().savePlayerData(data);
        }
    }

    @Override
    public double getCommonBonus(UUID playerId) {
        BaitData bait = getActiveBait(playerId);
        return (bait != null) ? bait.getType().getCommonBonus() : 0;
    }

    @Override
    public double getUncommonBonus(UUID playerId) {
        BaitData bait = getActiveBait(playerId);
        return (bait != null) ? bait.getType().getUncommonBonus() : 0;
    }

    @Override
    public double getRareBonus(UUID playerId) {
        BaitData bait = getActiveBait(playerId);
        return (bait != null) ? bait.getType().getRareBonus() : 0;
    }

    @Override
    public double getLegendaryBonus(UUID playerId) {
        BaitData bait = getActiveBait(playerId);
        return (bait != null) ? bait.getType().getLegendaryBonus() : 0;
    }

    @Override
    public int getBaitCount(UUID playerId, BaitType type) {
        // This usually checks physical inventory for /bait equip command
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerId);
        if (player == null)
            return 0;

        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isBaitItem(item, type)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @Override
    public void giveBait(UUID playerId, BaitType type, int quantity) {
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerId);
        if (player == null)
            return;

        ItemStack item = createBaitItem(type);
        item.setAmount(quantity);
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItem(player.getLocation(), leftover));
    }

    public ItemStack createBaitItem(BaitType type) {
        ConfigurationSection section = config.getConfigurationSection("baits." + type.name());
        if (section == null)
            return new ItemStack(Material.PAPER);

        Material mat = (type == BaitType.ENCHANTED) ? Material.GLOWSTONE_DUST : Material.WHEAT_SEEDS;
        // In real implementation we'd probably have a specific field for Material in
        // config
        // but it's missing from current baits.yml. Let's use defaults.

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(section.getString("displayName")));
        meta.lore(ComponentUtil.parseList(section.getStringList("lore")));

        // Mark as bait in PDC
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "bait_type");
        meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, type.name());

        item.setItemMeta(meta);
        return item;
    }

    private boolean isBaitItem(ItemStack item, BaitType type) {
        if (item == null || !item.hasItemMeta())
            return false;
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "bait_type");
        String storedType = item.getItemMeta().getPersistentDataContainer().get(key,
                org.bukkit.persistence.PersistentDataType.STRING);
        return type.name().equalsIgnoreCase(storedType);
    }
}
