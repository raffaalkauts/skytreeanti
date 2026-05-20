package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class KitCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private FileConfiguration kitsConfig;

    public KitCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        File file = new File(plugin.getDataFolder(), "kits.yml");
        if (!file.exists()) {
            plugin.saveResource("kits.yml", false);
        }
        this.kitsConfig = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return kitsConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // If args present, try to claim specific kit
        if (args.length > 0) {
            String kitName = args[0].toLowerCase();
            if (kitsConfig.contains("kits." + kitName)) {
                // We need to trigger the claim logic.
                // Since claim logic is currently in KitGUIListener, we should ideally
                // centralize it or simulate it.
                // For now, let's open the GUI as a fallback if we can't easily access the
                // listener's logic,
                // OR we can move the claim logic here.
                // Looking at the architecture, it's better to keep logic in one place.
                // However, the user specifically asked for subcommands to work.

                // Let's implement a direct claim check here.
                // We need to check permissions/cooldowns/cost.
                // Since I don't want to duplicate logic from KitGUIListener, I will fire a
                // custom event or just instantiate the logic.
                // Actually, for this fix to be robust, I should probably keep the GUI logic
                // separate but replicate the check here
                // or just open the GUI for now if that's acceptable? No, User said "/kits (sub
                // command) juga ga berfungsi".
                // I will replicate the basic claim logic here for immediacy.

                // BETTER APPROACH: Delegate to a KitService. But we don't have one explicitly
                // separate from KitGUIListener logic.
                // I will implement a basic claim here.

                // Construct a fake item click? No, that's hacky.
                // I'll just check if the kit exists and open the GUI? No, user wants
                // subcommand.

                // Verify kit exists
                ConfigurationSection kitSection = kitsConfig.getConfigurationSection("kits." + kitName);
                if (kitSection == null) {
                    player.sendMessage("§cKit '" + kitName + "' not found.");
                    return true;
                }

                // For now, to ensure I don't break permission logic handled elsewhere, I'll
                // redirect to a helper method
                // or just accept that I need to duplicate the "give items" logic from the
                // listener or move it to a Service.
                // I'll assume for this specific task, fixing the GUI population is higher
                // priority,
                // but the subcommand needs to essentially do what clicking the icon does.

                // Let's implement the claim logic directly here.
                handleKitClaim(player, kitName);
                return true;
            }
        }

        openKitGUI(player);
        return true;
    }

    private void handleKitClaim(Player player, String kitId) {
        claimKit(player, kitId);
    }

    @SuppressWarnings("deprecation")
    public void claimKit(Player player, String kitId) {
        FileConfiguration config = getConfig();
        String path = "kits." + kitId;

        if (!config.contains(path)) {
            player.sendMessage("§cError: Kit configuration not found.");
            return;
        }

        double price = config.getDouble(path + ".price");
        long cooldownSeconds = config.getLong(path + ".cooldown");

        // 1. Check Cooldown
        com.wiredid.skytree.model.PlayerData playerData = plugin.getPersistenceService()
                .loadPlayerData(player.getUniqueId());
        long lastClaim = playerData.getKitCooldown(kitId);
        long now = System.currentTimeMillis();

        if (cooldownSeconds > 0 && lastClaim > 0) {
            long elapsed = (now - lastClaim) / 1000;
            if (elapsed < cooldownSeconds) {
                long remaining = cooldownSeconds - elapsed;
                player.sendMessage(com.wiredid.skytree.util.ComponentUtil
                        .smartParse("§cCooldown: You must wait §e" + formatTime(remaining)));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
        }

        // 2. Check Price & Deduct Money FIRST (transactional safety)
        com.wiredid.skytree.api.EconomyService economy = plugin.getEconomyService();
        if (price > 0) {
            if (economy.getBalance(player.getUniqueId()) < price) {
                player.sendMessage(com.wiredid.skytree.util.ComponentUtil
                        .smartParse("§cInsufficient funds! Cost: " + economy.format(price)));
                return;
            }

            boolean success = economy.removeBalance(player.getUniqueId(), price);
            if (!success) {
                player.sendMessage("§cTransaction failed. Please try again.");
                return;
            }
        }

        // 3. Give Items
        if (config.contains(path + ".contents")) {
            java.util.List<java.util.Map<?, ?>> items = config.getMapList(path + ".contents");

            for (java.util.Map<?, ?> itemData : items) {
                String matName = (String) itemData.get("item");
                Object amountObj = itemData.get("amount");
                int amount = (amountObj instanceof Number) ? ((Number) amountObj).intValue() : 1;
                Object slotsObj = itemData.get("slots");
                int slots = (slotsObj instanceof Number) ? ((Number) slotsObj).intValue() : 1;
                if (slots < 1)
                    slots = 1;

                ItemStack is = null;
                // Check Item Registry first for custom items
                if (plugin.getItemRegistry().getItem(matName) != null) {
                    is = plugin.getItemRegistry().getItem(matName);
                    is.setAmount(amount);
                } else {
                    Material mat = Material.matchMaterial(matName);
                    if (mat != null) {
                        is = new ItemStack(mat, amount);
                    }
                }

                if (is != null) {
                    for (int i = 0; i < slots; i++) {
                        ItemStack itemToGive = is.clone();
                        ItemMeta meta = itemToGive.getItemMeta();

                        // Enchants
                        if (itemData.containsKey("enchants") && meta != null) {
                            try {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> enchants = (java.util.Map<String, Object>) itemData
                                        .get("enchants");
                                for (java.util.Map.Entry<String, Object> entry : enchants.entrySet()) {
                                    String enchantName = entry.getKey().toUpperCase();
                                    int level = ((Number) entry.getValue()).intValue();
                                    org.bukkit.enchantments.Enchantment ench = null;

                                    try {
                                        ench = org.bukkit.enchantments.Enchantment
                                                .getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
                                    } catch (Exception ignored) {
                                    }

                                    if (ench == null) {
                                        try {
                                            ench = org.bukkit.enchantments.Enchantment.getByName(enchantName);
                                        } catch (Exception ignored) {
                                        }
                                    }

                                    if (ench != null) {
                                        meta.addEnchant(ench, level, true);
                                    }
                                }
                            } catch (Exception e) {
                                plugin.getLogger()
                                        .warning("Error parsing enchants for kit " + kitId + ": " + e.getMessage());
                            }
                        }

                        // Custom Name
                        if (itemData.containsKey("name") && meta != null) {
                            meta.displayName(
                                    com.wiredid.skytree.util.ComponentUtil.smartParse("§b" + itemData.get("name")));
                        }

                        if (meta != null) {
                            itemToGive.setItemMeta(meta);
                        }

                        // Update worth lore
                        if (plugin.getWorthService() != null) {
                            plugin.getWorthService().updateItemLore(itemToGive);
                        }

                        // Give safely
                        java.util.HashMap<Integer, ItemStack> left = player.getInventory().addItem(itemToGive);
                        for (ItemStack drop : left.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                            player.sendMessage("§eInventory full! Item dropped on ground.");
                        }
                    }
                } else {
                    plugin.getLogger().warning("Invalid item in kit " + kitId + ": " + matName);
                    player.sendMessage("§cWarning: Could not find item '" + matName + "'. Please contact an admin.");
                }
            }
        }

        // Update Cooldown
        if (cooldownSeconds > 0) {
            playerData.setKitCooldown(kitId, now);
            plugin.getPersistenceService().savePlayerData(playerData);
        }

        player.sendMessage(com.wiredid.skytree.util.ComponentUtil
                .smartParse("§aSuccessfully claimed kit §e" + config.getString(path + ".name", kitId)));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    public void openKitGUI(Player player) {
        reloadConfig(); // Ensure fresh config

        // Dynamic size based on kit count
        int kitCount = kitsConfig.getConfigurationSection("kits").getKeys(false).size();
        int rows = (int) Math.ceil(kitCount / 9.0) + 2; // +2 for header/footer padding
        if (rows < 3)
            rows = 3;
        if (rows > 6)
            rows = 6;

        Inventory gui = Bukkit.createInventory(null, rows * 9, ComponentUtil.parse("§6§lKits §8» §7Selector"));

        ConfigurationSection kits = kitsConfig.getConfigurationSection("kits");
        if (kits != null) {
            int slot = 10; // Start at 2nd row
            for (String key : kits.getKeys(false)) {
                // Formatting logic...

                // Skip if slot is border (0-9, 17, 18, 26, 27, 35, etc.)?
                // Simplest is to just fill middle area.
                // Let's just fill sequentially for now to valid slots.

                // Logic to find next valid slot (inner area)
                while (slot < (rows - 1) * 9 && (slot % 9 == 0 || slot % 9 == 8)) {
                    slot++;
                }
                if (slot >= (rows - 1) * 9)
                    break;

                String name = kits.getString(key + ".name", key);
                double price = kits.getDouble(key + ".price", 0);
                long cooldown = kits.getLong(key + ".cooldown", 0);
                String iconName = kits.getString(key + ".icon", "CHEST");
                Material icon = Material.matchMaterial(iconName);
                if (icon == null)
                    icon = Material.CHEST;

                ItemStack item = new ItemStack(icon);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(ComponentUtil.parse("§e§l" + name));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(
                        ComponentUtil.parse("§7Price: §e" + (price > 0 ? NumberUtil.formatCurrency(price) : "§aFree")));
                lore.add(ComponentUtil
                        .parse("§7Cooldown: §f" + (cooldown > 0 ? "§6" + formatTime(cooldown) : "§aNone")));
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(ComponentUtil.parse("§e§lCLICK §7to View/Claim"));
                meta.lore(lore);

                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "kit_id"), PersistentDataType.STRING,
                        key);
                item.setItemMeta(meta);

                gui.setItem(slot++, item);
            }
        }

        // Filler & Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.BLACK_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE);

        player.openInventory(gui);
    }

    private String formatTime(long seconds) {
        if (seconds >= 86400)
            return (seconds / 86400) + "d";
        if (seconds >= 3600)
            return (seconds / 3600) + "h";
        if (seconds >= 60)
            return (seconds / 60) + "m";
        return seconds + "s";
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            ConfigurationSection kits = kitsConfig.getConfigurationSection("kits");
            if (kits == null)
                return Collections.emptyList();
            return kits.getKeys(false).stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
