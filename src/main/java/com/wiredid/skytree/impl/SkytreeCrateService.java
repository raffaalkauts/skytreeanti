package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.CrateService;
import com.wiredid.skytree.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;

import java.io.File;
import java.util.*;

public class SkytreeCrateService implements CrateService {

    private final SkytreePlugin plugin;
    private final Map<String, CrateData> crates = new HashMap<>();
    private final NamespacedKey CRATE_KEY_KEY;

    public SkytreeCrateService(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.CRATE_KEY_KEY = new NamespacedKey(plugin, "crate_key");
        reload();
    }

    @Override
    public Map<String, CrateData> getCrates() {
        return crates;
    }

    @Override
    public CrateData getCrate(String id) {
        return crates.get(id);
    }

    @Override
    public void giveKey(Player player, String crateId, int amount) {
        CrateData crate = crates.get(crateId);
        if (crate == null)
            return;

        ItemStack key = crate.getKeyItem().clone();
        key.setAmount(amount);
        java.util.Collection<ItemStack> leftovers = player.getInventory().addItem(key).values();
        if (leftovers != null) {
            leftovers.forEach(item -> {
                if (item != null) player.getWorld().dropItemNaturally(player.getLocation(), item);
            });
        }
    }

    @Override
    public boolean hasKey(Player player, String crateId) {
        ItemStack[] contents = player.getInventory().getContents();
        if (contents == null) return false;
        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR)
                continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null)
                continue;

            PersistentDataContainer data = meta.getPersistentDataContainer();
            if (data.has(CRATE_KEY_KEY, PersistentDataType.STRING)) {
                String id = data.get(CRATE_KEY_KEY, PersistentDataType.STRING);
                if (crateId.equals(id))
                    return true;
            }
        }
        return false;
    }

    public void takeKey(Player player, String crateId, int amount) {
        int taken = 0;
        ItemStack[] contents = player.getInventory().getContents();
        if (contents == null) return;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR)
                continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null)
                continue;

            PersistentDataContainer data = meta.getPersistentDataContainer();
            if (data.has(CRATE_KEY_KEY, PersistentDataType.STRING)) {
                String id = data.get(CRATE_KEY_KEY, PersistentDataType.STRING);
                if (crateId.equals(id)) {
                    int amt = item.getAmount();
                    if (taken + amt <= amount) {
                        taken += amt;
                        player.getInventory().setItem(i, null);
                    } else {
                        int needed = amount - taken;
                        item.setAmount(amt - needed);
                        taken += needed;
                    }
                    if (taken >= amount)
                        break;
                }
            }
        }
    }

    @Override
    public CrateReward openCrate(Player player, String crateId) {
        CrateData crate = crates.get(crateId);
        if (crate == null)
            return null;

        // Consume key
        takeKey(player, crateId, 1);

        // Pick reward
        return selectRandomReward(crate.getRewards());
    }

    private CrateReward selectRandomReward(List<CrateReward> rewards) {
        double totalWeight = rewards.stream().mapToDouble(CrateReward::getChance).sum();
        double random = new Random().nextDouble() * totalWeight;

        double currentWeight = 0;
        for (CrateReward reward : rewards) {
            currentWeight += reward.getChance();
            if (random <= currentWeight) {
                return reward;
            }
        }
        return rewards.get(0);
    }

    @Override
    public final void reload() {
        crates.clear();
        File file = new File(plugin.getDataFolder(), "crates.yml");
        if (!file.exists()) {
            plugin.saveResource("crates.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("crates");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection cSec = section.getConfigurationSection(key);
                if (cSec != null) {
                    // Load Key
                    ConfigurationSection kSec = cSec.getConfigurationSection("key");
                    if (kSec == null) continue;
                    ItemStack keyStack = new ItemStack(Material.valueOf(kSec.getString("material", "TRIPWIRE_HOOK")));
                    ItemMeta kMeta = keyStack.getItemMeta();
                    kMeta.displayName(ComponentUtil.smartParse(kSec.getString("name", "Key")));
                    List<Component> kLore = new ArrayList<>();
                    for (String line : kSec.getStringList("lore")) {
                        kLore.add(ComponentUtil.smartParse(line));
                    }
                    kMeta.lore(kLore);
                    kMeta.getPersistentDataContainer().set(CRATE_KEY_KEY, PersistentDataType.STRING, key);
                    if (kSec.getBoolean("enchanted", false)) {
                        kMeta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                        kMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                    keyStack.setItemMeta(kMeta);

                    // Load Rewards
                    List<CrateReward> rewards = new ArrayList<>();
                    List<Map<?, ?>> rList = cSec.getMapList("rewards");
                    for (Map<?, ?> rMapRaw : rList) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> rMap = (Map<String, Object>) rMapRaw;

                        String display = (String) rMap.get("display");
                        double chance = ((Number) rMap.get("chance")).doubleValue();
                        String materialStr = (String) rMap.get("item");
                        ItemStack item = null;
                        if (materialStr != null) {
                            try {
                                item = new ItemStack(Material.valueOf(materialStr),
                                        ((Number) rMap.getOrDefault("amount", 1)).intValue());
                            } catch (IllegalArgumentException e) {
                                item = plugin.getItemRegistry().getItem(materialStr);
                                if (item != null) {
                                    item = item.clone();
                                    item.setAmount(((Number) rMap.getOrDefault("amount", 1)).intValue());
                                }
                            }
                            @SuppressWarnings("unchecked")
                            List<String> lore = (List<String>) rMap.get("lore");
                            if (lore != null) {
                                ItemMeta meta = item.getItemMeta();
                                List<Component> compLore = new ArrayList<>();
                                for (String l : lore)
                                    compLore.add(ComponentUtil.smartParse(l));
                                meta.lore(compLore);
                                item.setItemMeta(meta);
                            }

                            // Load Enchants
                            @SuppressWarnings("unchecked")
                            List<String> enchants = (List<String>) rMap.get("enchantments");
                            if (enchants != null && !enchants.isEmpty()) {
                                ItemMeta meta = item.getItemMeta();
                                for (String enchantLine : enchants) {
                                    String[] split = enchantLine.split(":");
                                    @SuppressWarnings("deprecation")
                                    Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(split[0].toLowerCase()));
                                    int level = split.length > 1 ? Integer.parseInt(split[1]) : 1;
                                    if (ench != null) {
                                        if (item.getType() == Material.ENCHANTED_BOOK) {
                                            ((org.bukkit.inventory.meta.EnchantmentStorageMeta) meta).addStoredEnchant(ench, level, true);
                                        } else {
                                            meta.addEnchant(ench, level, true);
                                        }
                                    }
                                }
                                item.setItemMeta(meta);
                            }
                        }
                        String cmd = (String) rMap.get("command");
                        double money = ((Number) rMap.getOrDefault("money", 0.0)).doubleValue();

                        rewards.add(new CrateReward(chance, display, item, cmd, money));
                    }

                    crates.put(key, new CrateData(key, cSec.getString("name"), rewards, keyStack));
                }
            }
        }
    }
}
