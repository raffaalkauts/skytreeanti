package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.IslandShopService;
import com.wiredid.skytree.model.IslandShop;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SkytreeIslandShopService implements IslandShopService {

    private final SkytreePlugin plugin;
    private final File shopsFile;
    private YamlConfiguration shopsConfig;
    private final Map<Location, IslandShop> shops = new HashMap<>();

    public SkytreeIslandShopService(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.shopsFile = new File(plugin.getDataFolder(), "island_shops.yml");
        loadShops();
    }

    @Override
    public void createShop(Player owner, Block chest, double buyPrice, double sellPrice) {
        if (chest.getType() != Material.CHEST && chest.getType() != Material.TRAPPED_CHEST
                && chest.getType() != Material.BARREL) {
            return;
        }

        // Determine material from first item in chest if possible, or wait for GUI
        // interaction later?
        // For simplicity now, let's assume we take the item in hand or first item in
        // chest.
        ItemStack hand = owner.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            owner.sendMessage("§cYou must hold the item you want to sell.");
            return;
        }

        IslandShop shop = new IslandShop(owner.getUniqueId(), chest.getLocation(), hand.getType(), buyPrice, sellPrice);
        shops.put(chest.getLocation(), shop);
        saveShops();
        owner.sendMessage("§aIsland Shop created for " + hand.getType().name() + "!");
    }

    @Override
    public void removeShop(Block chest) {
        if (shops.remove(chest.getLocation()) != null) {
            saveShops();
        }
    }

    @Override
    public Optional<IslandShop> getShop(Block block) {
        return Optional.ofNullable(shops.get(block.getLocation()));
    }

    @Override
    public Collection<IslandShop> getAllShops() {
        return shops.values();
    }

    @Override
    public void saveShops() {
        shopsConfig = new YamlConfiguration();
        int i = 0;
        for (IslandShop shop : shops.values()) {
            String path = "shops." + i;
            shopsConfig.set(path + ".owner", shop.getOwnerUUID().toString());
            shopsConfig.set(path + ".loc", serializeLoc(shop.getLocation()));
            shopsConfig.set(path + ".material", shop.getMaterial().name());
            shopsConfig.set(path + ".buy", shop.getBuyPrice());
            shopsConfig.set(path + ".sell", shop.getSellPrice());
            shopsConfig.set(path + ".stock", shop.getStock());
            i++;
        }
        try {
            shopsConfig.save(shopsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save island_shops.yml!");
        }
    }

    @Override
    public void loadShops() {
        if (!shopsFile.exists()) {
            shopsConfig = new YamlConfiguration();
            return;
        }
        shopsConfig = YamlConfiguration.loadConfiguration(shopsFile);
        shops.clear();
        ConfigurationSection section = shopsConfig.getConfigurationSection("shops");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(section.getString(key + ".owner"));
                Location loc = deserializeLoc(section.getString(key + ".loc"));
                Material mat = Material.valueOf(section.getString(key + ".material"));
                double buy = section.getDouble(key + ".buy");
                double sell = section.getDouble(key + ".sell");
                int stock = section.getInt(key + ".stock");

                IslandShop shop = new IslandShop(owner, loc, mat, buy, sell);
                shop.setStock(stock);
                shops.put(loc, shop);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load island shop at " + key);
            }
        }
    }

    private String serializeLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location deserializeLoc(String s) {
        String[] parts = s.split(",");
        return new Location(plugin.getServer().getWorld(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]));
    }
}
