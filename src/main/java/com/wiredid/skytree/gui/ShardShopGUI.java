package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ShardService;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShardShopGUI implements Listener {

    private final SkytreePlugin plugin;
    private final ShardService shardService;
    private final Map<Integer, ShopItem> shopItems = new HashMap<>();

    public ShardShopGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.shardService = plugin.getShardService();
        loadConfig();
    }

    public void reload() {
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "shard_shop.yml");
        if (!file.exists()) {
            plugin.saveResource("shard_shop.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        shopItems.clear();

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section != null) {
                int price = section.getInt("price");
                int slot = section.getInt("slot");
                shopItems.put(slot, new ShopItem(key, price));
            }
        }
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ComponentUtil.smartParse("§6§lShard Shop §8» §7Market"));

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(inv, Material.BLACK_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE);

        // Shop Items
        for (Map.Entry<Integer, ShopItem> entry : shopItems.entrySet()) {
            int slot = entry.getKey();
            ShopItem shopItem = entry.getValue();

            ItemStack item = plugin.getItemRegistry().getItem(shopItem.id);
            if (item == null)
                continue;

            item = item.clone();
            ItemMeta meta = item.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = meta.lore();
            if (lore == null)
                lore = new ArrayList<>();
            else
                lore = new ArrayList<>(lore);

            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(ComponentUtil.smartParse("§7Price: §3" + shopItem.price + " Shards"));
            lore.add(ComponentUtil.smartParse("§eClick to purchase!"));

            meta.lore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot, item);
        }

        // Balance Info
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(ComponentUtil.smartParse("§b§lYour Balance"));
        List<net.kyori.adventure.text.Component> infoLore = new ArrayList<>();
        infoLore.add(ComponentUtil.smartParse("§fCurrent Shards: §3" + shardService.getShards(player.getUniqueId())));
        infoMeta.lore(infoLore);
        info.setItemMeta(infoMeta);
        inv.setItem(40, info);

        player.openInventory(inv);
    }

    private final Map<java.util.UUID, Long> lastPurchase = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Shard Shop"))
            return;

        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player))
            return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        int slot = event.getSlot();
        if (shopItems.containsKey(slot)) {
            // Cooldown check (500ms)
            long last = lastPurchase.getOrDefault(player.getUniqueId(), 0L);
            if (System.currentTimeMillis() - last < 500) {
                return;
            }
            lastPurchase.put(player.getUniqueId(), System.currentTimeMillis());

            ShopItem shopItem = shopItems.get(slot);
            if (shardService.hasShards(player.getUniqueId(), shopItem.price)) {
                shardService.removeShards(player.getUniqueId(), shopItem.price);

                ItemStack purchase = plugin.getItemRegistry().getItem(shopItem.id);
                if (purchase == null && shopItem.id.equals("earth_destroyer")) {
                    // Create Earth Destroyer on the fly if not in registry
                    purchase = new ItemStack(Material.MACE);
                    ItemMeta meta = purchase.getItemMeta();
                    meta.displayName(ComponentUtil.smartParse("§4§lEarth Destroyer"));
                    List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                    lore.add(ComponentUtil.smartParse("§7An ancient weapon that can shatter the world."));
                    lore.add(ComponentUtil.smartParse("§c§lOP MACE - 1.21+"));
                    meta.lore(lore);
                    
                    // Correct Mace Enchantments
                    try {
                        // Density V - Increases smash damage
                        org.bukkit.enchantments.Enchantment density = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("density"));
                        if (density != null) meta.addEnchant(density, 10, true); // Over-leveled for OP effect
                        
                        // Breach IV - Pierces armor
                        org.bukkit.enchantments.Enchantment breach = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("breach"));
                        if (breach != null) meta.addEnchant(breach, 10, true);
                        
                        // Wind Burst III - Bounce back on hit
                        org.bukkit.enchantments.Enchantment windBurst = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("wind_burst"));
                        if (windBurst != null) meta.addEnchant(windBurst, 10, true);
                    } catch (Exception e) {
                        plugin.getLogger().warning("[ShardShop] Failed to apply mace enchantments");
                    }

                    // Compatible General Enchantments
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 10, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 10, true);
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
                    
                    // Unbreakable tag for OP item
                    meta.setUnbreakable(true);
                    
                    purchase.setItemMeta(meta);
                }

                if (purchase != null && purchase.getItemMeta() != null) {
                    player.getInventory().addItem(purchase.clone()).values().forEach(
                            leftover -> {
                                if (leftover != null) player.getWorld().dropItem(player.getLocation(), leftover);
                            });
                    player.sendMessage(ComponentUtil.smartParse(
                            "§aSuccessfully purchased §e" + shopItem.id + " §afor §3" + shopItem.price + " §ashards!"));
                    // Schedule GUI refresh on next tick to prevent double-click issues
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> open(player));
                } else {
                    player.sendMessage(ComponentUtil.smartParse("§cError: Item not found in registry."));
                }
            } else {
                player.sendMessage(ComponentUtil.smartParse("§cYou don't have enough shards!"));
            }
        }
    }

    private static class ShopItem {
        String id;
        int price;

        ShopItem(String id, int price) {
            this.id = id;
            this.price = price;
        }
    }
}
