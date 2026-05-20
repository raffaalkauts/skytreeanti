package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

/**
 * Listener for Shop GUI interactions with BUY and SELL functionality.
 * Refactored to be data-driven from shop.yml
 */
public class ShopGUIListener implements Listener {

    private final SkytreePlugin plugin;
    private final EconomyService economy;
    private final ItemRegistry itemRegistry;

    public ShopGUIListener(SkytreePlugin plugin, EconomyService economy, ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.economy = economy;
        this.itemRegistry = itemRegistry;
    }

    public void reload() {
        // ShopGUIListener now pulls from ShopService dynamically
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.stripColor(event.getView().title());
        if (!title.contains("Shop") && !title.contains("Categories") && !title.contains("Search")) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        // Don't cancel when clicking in player's own inventory
        if (event.getClickedInventory() == player.getInventory())
            return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        // Handle category selection or main menu actions
        if (title.contains("Shop Categories")) {
            // Check for Search Button
            if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(
                    new org.bukkit.NamespacedKey(plugin, "shop_action"),
                    org.bukkit.persistence.PersistentDataType.STRING)) {
                String action = clicked.getItemMeta().getPersistentDataContainer().get(
                        new org.bukkit.NamespacedKey(plugin, "shop_action"),
                        org.bukkit.persistence.PersistentDataType.STRING);
                if ("SEARCH".equals(action)) {
                    player.closeInventory();
                    player.sendMessage("§6§lShop §8» §eEnter search query in chat:");
                    player.setMetadata("shop_search_context",
                            new org.bukkit.metadata.FixedMetadataValue(plugin, "true"));
                    return;
                } else if ("SEARCH_TOGGLE".equals(action)) {
                    String query = clicked.getItemMeta().getPersistentDataContainer().get(
                            new org.bukkit.NamespacedKey(plugin, "search_query"),
                            org.bukkit.persistence.PersistentDataType.STRING);
                    String currentMode = clicked.getItemMeta().getPersistentDataContainer().get(
                            new org.bukkit.NamespacedKey(plugin, "search_mode"),
                            org.bukkit.persistence.PersistentDataType.STRING);

                    String newMode = "BUY".equals(currentMode) ? "SELL" : "BUY";
                    ((com.wiredid.skytree.impl.SkytreeShopService) plugin.getShopService()).openSearch(player, query,
                            newMode);
                    return;
                }
            }
            handleCategoryClick(player, clicked);
            return;
        }

        // Handle Search Results or general Shop views
        if (title.contains("Search") || title.contains("Custom Items") || title.contains("Shop")) {
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return;
            }
            if (clicked.getType() == Material.BLAZE_ROD && clicked.hasItemMeta()) {
                String dName = ComponentUtil.stripColor(clicked.getItemMeta().displayName());
                if (dName != null && dName.contains("Sell Wand")) {
                    double price = plugin.getConfig().getDouble("shop.sell_wand_price", 100000);
                    if (economy.getBalance(player.getUniqueId()) >= price) {
                        economy.removeBalance(player.getUniqueId(), price);
                        org.bukkit.inventory.ItemStack sellWandItem = plugin.getItemRegistry().getItem("sell_wand");
                        if (sellWandItem != null && plugin.getWorthService() != null) {
                            plugin.getWorthService().updateItemLore(sellWandItem);
                        }
                        player.getInventory().addItem(sellWandItem);
                        player.sendMessage("§a§l[Shop] §7Purchased §6Sell Wand §7for §e"
                                + NumberUtil.formatCurrency(price));
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                    } else {
                        player.sendMessage(
                                "§c§l[Shop] §7You need §e" + NumberUtil.formatCurrency(price) + " §7to buy this!");
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                    }
                    return;
                }
            }

            // Handle Sell All Button
            if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(
                    new org.bukkit.NamespacedKey(plugin, "shop_action"),
                    org.bukkit.persistence.PersistentDataType.STRING)) {
                String action = clicked.getItemMeta().getPersistentDataContainer().get(
                        new org.bukkit.NamespacedKey(plugin, "shop_action"),
                        org.bukkit.persistence.PersistentDataType.STRING);

                if ("SELL_ALL_CATEGORY".equals(action)) {
                    String catId = clicked.getItemMeta().getPersistentDataContainer().get(
                            new org.bukkit.NamespacedKey(plugin, "shop_cat_id"),
                            org.bukkit.persistence.PersistentDataType.STRING);
                    if (catId != null) {
                        handleSellAll(player, catId);
                    }
                    return;
                } else if ("SEARCH_TOGGLE".equals(action)) {
                    String query = clicked.getItemMeta().getPersistentDataContainer().get(
                            new org.bukkit.NamespacedKey(plugin, "search_query"),
                            org.bukkit.persistence.PersistentDataType.STRING);
                    String currentMode = clicked.getItemMeta().getPersistentDataContainer().get(
                            new org.bukkit.NamespacedKey(plugin, "search_mode"),
                            org.bukkit.persistence.PersistentDataType.STRING);

                    String newMode = "BUY".equals(currentMode) ? "SELL" : "BUY";
                    ((com.wiredid.skytree.impl.SkytreeShopService) plugin.getShopService()).openSearch(player, query,
                            newMode);
                    return;
                }
            }

            // Fallthrough to handle normal item transactions in any shop view
            handleItemTransaction(player, clicked, event);
        }
    }

    private void handleSellAll(Player player, String categoryId) {
        ConfigurationSection catSection = plugin.getShopService().getShopConfig()
                .getConfigurationSection("categories." + categoryId);
        String catName = categoryId;
        Material catIcon = Material.CHEST;

        if (catSection != null) {
            catName = catSection.getString("name", categoryId);
            String iconStr = catSection.getString("icon");
            if (iconStr != null) {
                try {
                    catIcon = Material.valueOf(iconStr.toUpperCase());
                } catch (Exception ignored) {
                }
            }
        }

        ItemStack infoIcon = new ItemStack(catIcon);
        ItemMeta meta = infoIcon.getItemMeta();
        meta.displayName(ComponentUtil.smartParse("§e§lSELL ALL: " + catName));
        infoIcon.setItemMeta(meta);

        new com.wiredid.skytree.gui.ConfirmationGUI(plugin).open(
                player,
                "§0Confirm Sell All",
                infoIcon,
                (confirmed) -> {
                    if (confirmed) {
                        executeSellAll(player, categoryId);
                    }
                },
                "§7Are you sure you want to sell §lALL §7items\n§7from this category in your inventory?");
    }

    private void executeSellAll(Player player, String categoryId) {
        ConfigurationSection catSection = plugin.getShopService().getShopConfig()
                .getConfigurationSection("categories." + categoryId);

        if (catSection == null)
            return;

        double totalValue = 0;
        int totalItems = 0;

        // Loop inventory
        com.wiredid.skytree.api.WorthService worth = plugin.getWorthService();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack is = player.getInventory().getItem(i);
            if (is == null || is.getType() == Material.AIR)
                continue;

            String matchKey = null;

            // 1. Check ALL keys in the category to find a match
            for (String key : catSection.getKeys(false)) {
                // Try as custom item id
                ItemStack template = plugin.getItemRegistry().getItem(key);
                if (template != null) {
                    if (worth.isSimilarIgnoringWorth(is, template)) {
                        matchKey = key;
                        break;
                    }
                } else {
                    // Try as material
                    try {
                        Material mat = Material.valueOf(key.toUpperCase());
                        if (is.getType() == mat) {
                            // Verify it's not a custom item if the shop expects a pure material
                            if (!plugin.getItemRegistry().isCustomItem(is)) {
                                matchKey = key;
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            if (matchKey != null) {
                double sellPrice = catSection.getDouble(matchKey + ".sell", -1);
                if (sellPrice > 0) {
                    double itemValue = sellPrice * is.getAmount();
                    totalValue += itemValue;
                    totalItems += is.getAmount();
                    player.getInventory().setItem(i, null); // Remove item
                }
            }
        }

        if (totalValue > 0) {
            economy.addBalance(player.getUniqueId(), totalValue);
            player.sendMessage(
                    "§a§l[Shop] §7Sold §e" + totalItems + " §7items for §a" + NumberUtil.formatCurrency(totalValue));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        } else {
            player.sendMessage("§c§l[Shop] §7No sellable items found for this category in your inventory.");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        }
    }

    private void handleCategoryClick(Player player, ItemStack clicked) {
        if (!clicked.hasItemMeta())
            return;

        // 1. Prioritize NBT detection (Reliable)
        String categoryId = clicked.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "shop_category"),
                org.bukkit.persistence.PersistentDataType.STRING);

        // 2. Fallback to name-based ONLY if NBT is missing (Legacy support)
        if (categoryId == null && clicked.getItemMeta().hasDisplayName()) {
            String name = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());
            if (name.contains("Blocks"))
                categoryId = "blocks_natural";
            else if (name.contains("Decoration"))
                categoryId = "decoration_misc";
            else if (name.contains("Farming"))
                categoryId = "farming";
            else if (name.contains("Mob Drops"))
                categoryId = "mob_drops";
            else if (name.contains("Minerals"))
                categoryId = "minerals";
            else if (name.contains("Custom Items"))
                categoryId = "custom_items";
            else if (name.contains("Machines"))
                categoryId = "machines";
            else if (name.contains("Spawners"))
                categoryId = "spawners";
            else if (name.contains("Equipment"))
                categoryId = "equipment";
            else if (name.contains("Food"))
                categoryId = "food";
            else if (name.contains("Redstone"))
                categoryId = "redstone";
            else if (name.contains("Transport"))
                categoryId = "transport";
            else if (name.contains("Brewing"))
                categoryId = "brewing";
            else if (name.contains("Dyes"))
                categoryId = "dyes";
            else if (name.contains("Misc"))
                categoryId = "misc";
        }

        if (categoryId != null) {
            plugin.getShopService().openCategory(player, categoryId);
        } else {
            player.sendMessage("§cCategory not found!");
        }
    }

    // Mode: "BUY" or "SELL". Default "BUY".
    // Stored in player's PDCs in specific scenarios? Or just passed in GUI
    // title/args.
    // Simplest approach: Pass mode in arguments.

    private void handleItemTransaction(Player player, ItemStack clicked, InventoryClickEvent event) {
        if (!clicked.hasItemMeta())
            return;

        // 1. Navigation / Back
        if (clicked.getType() == Material.ARROW) {
            String name = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());
            if (name.contains("Back")) {
                plugin.getShopService().openShop(player);
                return;
            }
            // Nav
            if (clicked.getItemMeta().getPersistentDataContainer().has(
                    new org.bukkit.NamespacedKey(plugin, "shop_page"),
                    org.bukkit.persistence.PersistentDataType.INTEGER)) {
                int page = clicked.getItemMeta().getPersistentDataContainer().get(
                        new org.bukkit.NamespacedKey(plugin, "shop_page"),
                        org.bukkit.persistence.PersistentDataType.INTEGER);
                String catId = clicked.getItemMeta().getPersistentDataContainer().get(
                        new org.bukkit.NamespacedKey(plugin, "shop_cat_id"),
                        org.bukkit.persistence.PersistentDataType.STRING);
                String mode = clicked.getItemMeta().getPersistentDataContainer().getOrDefault(
                        new org.bukkit.NamespacedKey(plugin, "shop_mode"),
                        org.bukkit.persistence.PersistentDataType.STRING, "BUY");
                plugin.getShopService().openCategory(player, catId, page, mode);
                return;
            }
        }

        // 2. Mode Toggle Button
        if (clicked.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "shop_toggle_mode"),
                org.bukkit.persistence.PersistentDataType.STRING)) {
            String targetMode = clicked.getItemMeta().getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey(plugin, "shop_toggle_mode"),
                    org.bukkit.persistence.PersistentDataType.STRING);
            String catId = clicked.getItemMeta().getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey(plugin, "shop_cat_id"),
                    org.bukkit.persistence.PersistentDataType.STRING);
            int page = clicked.getItemMeta().getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey(plugin, "shop_page"),
                    org.bukkit.persistence.PersistentDataType.INTEGER);

            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getShopService().openCategory(player, catId, page, targetMode);
            return;
        }

        // 3. Shop Items
        String shopItemId = null;
        if (clicked.getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "shop_item_id"),
                org.bukkit.persistence.PersistentDataType.STRING)) {
            shopItemId = clicked.getItemMeta().getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey(plugin, "shop_item_id"),
                    org.bukkit.persistence.PersistentDataType.STRING);
        }

        if (shopItemId == null)
            return; // Not a shop item

        org.bukkit.persistence.PersistentDataContainer data = clicked.getItemMeta().getPersistentDataContainer();
        double buyPrice = data.getOrDefault(new org.bukkit.NamespacedKey(plugin, "shop_buy_price"),
                org.bukkit.persistence.PersistentDataType.DOUBLE, -1.0);
        double sellPrice = data.getOrDefault(new org.bukkit.NamespacedKey(plugin, "shop_sell_price"),
                org.bukkit.persistence.PersistentDataType.DOUBLE, -1.0);

        List<String> lore = clicked.getItemMeta().lore() != null
                ? clicked.getItemMeta().lore().stream().map(ComponentUtil::toLegacy).toList()
                : new ArrayList<>();

        // Determine Mode from Title (fallback/safety) or Context
        // Actually, we don't strictly need to pass "Mode" to the item if we rely on the
        // GUI state,
        // BUT determining GUI state locally is hard.
        // Better: Check the item's LORE or NBT to see what action it proposes.
        // My createShopItem methods will put "Click to BUY" or "Click to SELL" in lore
        // based on mode.
        // We can just check that.

        boolean isBuyAction = false;
        boolean isSellAction = false;
        for (String line : lore) {
            if (line.contains("Click to BUY"))
                isBuyAction = true;
            if (line.contains("Click to SELL"))
                isSellAction = true;
        }

        if (isBuyAction) {
            if (buyPrice == -1)
                return;
            new com.wiredid.skytree.gui.QuantitySelectionGUI(plugin, economy, itemRegistry)
                    .open(player, clicked, buyPrice, com.wiredid.skytree.gui.QuantitySelectionGUI.Mode.BUY, shopItemId);
        } else if (isSellAction) {
            if (sellPrice <= 0) {
                player.sendMessage("§cThis item cannot be sold!");
                return;
            }
            new com.wiredid.skytree.gui.QuantitySelectionGUI(plugin, economy, itemRegistry)
                    .open(player, clicked, sellPrice, com.wiredid.skytree.gui.QuantitySelectionGUI.Mode.SELL,
                            shopItemId);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!player.hasMetadata("shop_search_context"))
            return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        player.removeMetadata("shop_search_context", plugin);

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage("§cSearch cancelled.");
            return;
        }

        // Open Search Results on main thread
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            ((com.wiredid.skytree.impl.SkytreeShopService) plugin.getShopService()).openSearch(player, message);
        });
    }
}
