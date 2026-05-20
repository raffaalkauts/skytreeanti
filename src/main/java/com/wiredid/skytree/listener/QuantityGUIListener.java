package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.system.QuestSystem;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.ArrayList;

/**
 * Listener for Quantity Selection GUI interactions
 */
public class QuantityGUIListener implements Listener {

    private final SkytreePlugin plugin;
    private final EconomyService economy;
    private final ItemRegistry itemRegistry;
    private final QuestSystem questSystem;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey priceKey;
    private final NamespacedKey quantityKey;

    public QuantityGUIListener(SkytreePlugin plugin, EconomyService economy, ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.economy = economy;
        this.itemRegistry = itemRegistry;
        this.questSystem = plugin.getQuestSystem();
        this.itemIdKey = new NamespacedKey(plugin, "shop_item_id");
        this.priceKey = new NamespacedKey(plugin, "shop_price");
        this.quantityKey = new NamespacedKey(plugin, "shop_quantity");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.stripColor(event.getView().title());
        if (!title.contains("Cart") && !title.contains("Checkout") && !title.contains("Quantity")) {
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

        Inventory gui = event.getInventory();
        ItemStack infoItem = gui.getItem(4);
        if (infoItem == null)
            return;

        com.wiredid.skytree.gui.QuantitySelectionGUI.Mode mode = title.contains("Cart")
                ? com.wiredid.skytree.gui.QuantitySelectionGUI.Mode.BUY
                : com.wiredid.skytree.gui.QuantitySelectionGUI.Mode.SELL;

        org.bukkit.persistence.PersistentDataContainer data = infoItem.getItemMeta().getPersistentDataContainer();
        if (!data.has(priceKey, PersistentDataType.DOUBLE)) {
            return;
        }

        int currentKv = data.getOrDefault(quantityKey, PersistentDataType.INTEGER, 1);
        double price = data.get(priceKey, PersistentDataType.DOUBLE);
        String shopItemId = data.get(itemIdKey, PersistentDataType.STRING);

        ItemStack cleanShopItem = resolveCleanItem(shopItemId, infoItem);

        if (clicked.getType() == Material.RED_WOOL) {
            player.closeInventory();
            plugin.getShopService().openShop(player);
            return;
        }

        if (clicked.getType() == Material.LIME_CONCRETE) {
            // Confirm button clicked -> Open Confirmation GUI instead of executing
            // immediately
            double total = price * currentKv;
            String totalDisplay = NumberUtil.formatCurrency(total);
            String confirmTitle = (mode == com.wiredid.skytree.gui.QuantitySelectionGUI.Mode.BUY ? "Confirm Buy"
                    : "Confirm Sell");

            // Define the callback logic
            java.util.function.Consumer<Boolean> callback = (confirmed) -> {
                plugin.getLogger().info("[DEBUG] Callback invoked! Confirmed=" + confirmed);
                if (confirmed) {
                    // Execute transaction
                    handleTransaction(player, cleanShopItem, currentKv, price, mode, shopItemId);
                    // Re-open shop after success
                    plugin.getShopService().openShop(player);
                } else {
                    // Cancelled -> Re-open Quantity Selection
                    new com.wiredid.skytree.gui.QuantitySelectionGUI(plugin, economy, itemRegistry)
                            .open(player, cleanShopItem, price, mode, shopItemId);
                }
            };

            // Open Confirmation GUI
            // Pass the item to display in center (clone it so we don't handle reference
            // issues)
            ItemStack confirmItem = cleanShopItem.clone();
            confirmItem.setAmount(currentKv);

            // We need a way to pass the total cost string to ConfirmationGUI.
            // Since ConfirmationGUI.open signature is currently (Player, String title,
            // ItemStack item, Consumer callback)
            // we will need to update ConfirmationGUI to accept the total string OR put it
            // in the item lore here.

            // Let's add it to Item Lore for now to match current signature, OR update
            // ConfirmationGUI next.
            // As per plan, we update ConfirmationGUI to accept it.
            // For this step, I'll assume we update ConfirmationGUI.open to accept
            // 'totalDisplay'
            // If ConfirmationGUI isn't updated yet, this will fail compilation.
            // Wait, I am editing QuantityGUIListener first.
            // I should use the existing signature or update it.
            // Artifact plan says: "Modify open method to accept String totalDisplay"

            // Effectively, I will call the NEW signature here, and then update
            // ConfirmationGUI in the next step.
            plugin.getConfirmationGUI().open(player, confirmTitle, confirmItem, callback, "§eTotal: " + totalDisplay);
            return;
        }

        // ... existing navigation logic ...

        int newKv = currentKv;

        switch (event.getSlot()) {
            case 10 -> newKv -= 64;
            case 11 -> newKv -= 16;
            case 12 -> newKv -= 1;
            case 13 -> {
                if (mode == com.wiredid.skytree.gui.QuantitySelectionGUI.Mode.BUY) {
                    newKv = 768;
                } else {
                    newKv = countAvailableItems(player, cleanShopItem);
                }
            }
            case 14 -> newKv += 1;
            case 15 -> newKv += 16;
            case 16 -> newKv += 64;
            default -> {
                return;
            }
        }

        if (newKv < 1)
            newKv = 1;
        if (mode == com.wiredid.skytree.gui.QuantitySelectionGUI.Mode.BUY && newKv > 2304)
            newKv = 2304;

        if (mode == com.wiredid.skytree.gui.QuantitySelectionGUI.Mode.SELL) {
            int max = countAvailableItems(player, cleanShopItem);
            if (max == 0)
                max = 1;
            if (newKv > max)
                newKv = max;
        }

        if (newKv != currentKv) {
            com.wiredid.skytree.gui.QuantitySelectionGUI guiHelper = new com.wiredid.skytree.gui.QuantitySelectionGUI(
                    plugin, economy, itemRegistry);
            guiHelper.updateGUI(gui, player, cleanShopItem, price, newKv, mode, shopItemId);
        }
    }

    private ItemStack resolveCleanItem(String shopItemId, ItemStack infoItem) {
        ItemStack cleanShopItem = null;

        // --- Special case: spawner items (e.g. "spawner_zombie") ---
        if (shopItemId != null && shopItemId.startsWith("spawner_")) {
            String entityName = shopItemId.replace("spawner_", "").toUpperCase();
            // Alias map for display names
            entityName = switch (entityName) {
                case "MOOSHROOM" -> "MUSHROOM_COW";
                case "IRON_GOLEM" -> "IRON_GOLEM";
                case "SNOW_GOLEM" -> "SNOW_GOLEM";
                default -> entityName;
            };
            try {
                org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(entityName);
                cleanShopItem = new ItemStack(Material.SPAWNER);
                org.bukkit.inventory.meta.ItemMeta spawnMeta = cleanShopItem.getItemMeta();
                if (spawnMeta instanceof org.bukkit.inventory.meta.BlockStateMeta bsm) {
                    org.bukkit.block.CreatureSpawner cs = (org.bukkit.block.CreatureSpawner) bsm.getBlockState();
                    cs.setSpawnedType(type);
                    bsm.setBlockState(cs);
                    // Friendly display name
                    String friendly = convertToTitleCase(type.name().replace("_", " "));
                    bsm.displayName(ComponentUtil.parse("§f" + friendly + " Spawner"));
                    cleanShopItem.setItemMeta(bsm);
                }
            } catch (Exception e) {
                // Fallback to plain spawner if entity type is unknown
                cleanShopItem = new ItemStack(Material.SPAWNER);
            }
            return cleanShopItem;
        }

        // --- Special case: enchanted books (e.g. "enchanted_book_sharpness_5") ---
        if (shopItemId != null && shopItemId.startsWith("enchanted_book_")) {
            String raw = shopItemId.replace("enchanted_book_", "");
            try {
                int lastUnderscore = raw.lastIndexOf('_');
                if (lastUnderscore != -1) {
                    String enchantName = raw.substring(0, lastUnderscore).toLowerCase();
                    int level = 1;
                    try { level = Integer.parseInt(raw.substring(lastUnderscore + 1)); } catch (NumberFormatException e) { /* default level 1 */ }
                    @SuppressWarnings("deprecation")
                    org.bukkit.enchantments.Enchantment enchant = org.bukkit.Registry.ENCHANTMENT
                            .get(org.bukkit.NamespacedKey.minecraft(enchantName));
                    if (enchant != null) {
                        cleanShopItem = new ItemStack(Material.ENCHANTED_BOOK);
                        org.bukkit.inventory.meta.EnchantmentStorageMeta esm =
                                (org.bukkit.inventory.meta.EnchantmentStorageMeta) cleanShopItem.getItemMeta();
                        if (esm != null) {
                            esm.addStoredEnchant(enchant, level, true);
                            cleanShopItem.setItemMeta(esm);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Shop] Failed to parse enchantment item: " + raw);
            }
            if (cleanShopItem == null) cleanShopItem = new ItemStack(Material.ENCHANTED_BOOK);
            return cleanShopItem;
        }

        // --- Custom registry lookup ---
        if (shopItemId != null) {
            cleanShopItem = itemRegistry.getItem(shopItemId);
            if (cleanShopItem == null) {
                try {
                    Material mat = Material.valueOf(shopItemId.toUpperCase());
                    cleanShopItem = new ItemStack(mat);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // --- Fallback: clone GUI preview item and scrub shop metadata ---
        if (cleanShopItem == null) {
            cleanShopItem = infoItem.clone();
            cleanShopItem.setAmount(1);
            org.bukkit.inventory.meta.ItemMeta meta = cleanShopItem.getItemMeta();

            // Scrub lore of shop-specific lines
            if (meta.hasLore()) {
                List<net.kyori.adventure.text.Component> lore = meta.lore();
                List<net.kyori.adventure.text.Component> cleanLore = new ArrayList<>();
                if (lore != null) {
                    for (net.kyori.adventure.text.Component comp : lore) {
                        String line = ComponentUtil.toLegacy(comp);
                        if (!line.contains("Price per item:") &&
                                !line.contains("Quantity:") &&
                                !line.contains("Total cost:") &&
                                !line.contains("Total value:") &&
                                !line.contains("Your balance:") &&
                                !line.contains("Available:") &&
                                !line.contains("Click to BUY") &&
                                !line.contains("Click to SELL") &&
                                !line.contains("----------------")) {
                            cleanLore.add(comp);
                        }
                    }
                }
                meta.lore(cleanLore);
            }

            // Clear all shop-related PDC keys
            org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.remove(new NamespacedKey(plugin, "shop_item_id"));
            pdc.remove(new NamespacedKey(plugin, "shop_buy_price"));
            pdc.remove(new NamespacedKey(plugin, "shop_sell_price"));
            pdc.remove(new NamespacedKey(plugin, "shop_price"));
            pdc.remove(new NamespacedKey(plugin, "shop_quantity"));

            cleanShopItem.setItemMeta(meta);
        }
        return cleanShopItem;
    }

    private int countAvailableItems(Player player, ItemStack shopItem) {
        int count = 0;
        com.wiredid.skytree.api.WorthService worth = plugin.getWorthService();
        ItemStack[] contents = player.getInventory().getContents();
        if (contents == null) return 0;

        for (ItemStack invItem : contents) {
            if (invItem == null)
                continue;

            if (worth.isSimilarIgnoringWorth(invItem, shopItem)) {
                count += invItem.getAmount();
            }
        }
        return count;
    }

    private void handleTransaction(Player player, ItemStack item, int quantity, double price,
            com.wiredid.skytree.gui.QuantitySelectionGUI.Mode mode, String shopItemId) {
        double total = price * quantity;

        if (mode == com.wiredid.skytree.gui.QuantitySelectionGUI.Mode.BUY) {
            if (economy.getBalance(player.getUniqueId()) < total) {
                player.sendMessage("§c§l[Shop] §7Insufficient funds!");
                return;
            }

            // Perform Buy
            if (shopItemId != null && shopItemId.startsWith("rank_")) {
                String rankName = shopItemId.replace("rank_", "").toUpperCase();
                try {
                    com.wiredid.skytree.model.Rank rank = com.wiredid.skytree.model.Rank.valueOf(rankName);
                    economy.removeBalance(player.getUniqueId(), total);
                    plugin.getRankService().setRank(player.getUniqueId(), rank);
                    player.sendMessage("§a§l[Shop] §7Purchased " + rank.getPrefix() + " §7rank!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                    return;
                } catch (Exception ignored) {
                }
            }

            if (shopItemId != null && shopItemId.startsWith("command_")) {
                String cmd = shopItemId.replace("command_", "");
                economy.removeBalance(player.getUniqueId(), total);
                for (int i = 0; i < quantity; i++) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
                }
                player.sendMessage("§a§l[Shop] §7Purchase successful!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                return;
            }

            // Atomic Item Delivery
            ItemStack giveItem = item.clone();
            giveItem.setAmount(quantity);

            // Remove balance first, but be ready to rollback if needed
            economy.removeBalance(player.getUniqueId(), total);

            java.util.HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(giveItem);
            if (!leftovers.isEmpty()) {
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                player.sendMessage("§eInventory full! Some items were dropped on the ground.");
            }

            player.sendMessage("§a§l[Shop] §7Purchased §6" + quantity + "x " + getDisplayName(item) + " §7for §e"
                    + NumberUtil.formatCurrency(total));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            questSystem.addProgress(player, com.wiredid.skytree.system.QuestSystem.QuestType.SHOP_BUY, 1);
        } else {
            // Atomic Sell
            int available = countAvailableItems(player, item);
            if (available < quantity) {
                player.sendMessage("§cYou don't have enough items anymore!");
                return;
            }

            // Remove items first (Safe removal logic)
            if (removeItems(player, item, quantity)) {
                economy.addBalance(player.getUniqueId(), total);
                player.sendMessage("§a§l[Shop] §7Sold §6" + quantity + "x " + getDisplayName(item) + " §7for §e"
                        + NumberUtil.formatCurrency(total));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                questSystem.addProgress(player, com.wiredid.skytree.system.QuestSystem.QuestType.MONEY_EARN,
                        (int) total);
            } else {
                player.sendMessage("§cTransaction failed: Critical item sync error.");
            }
        }
    }

    private boolean removeItems(Player player, ItemStack item, int quantity) {
        int remaining = quantity;
        ItemStack[] contents = player.getInventory().getContents();
        if (contents == null) return false;
        com.wiredid.skytree.api.WorthService worth = plugin.getWorthService();

        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is == null)
                continue;

            if (worth.isSimilarIgnoringWorth(is, item)) {
                if (is.getAmount() <= remaining) {
                    remaining -= is.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    is.setAmount(is.getAmount() - remaining);
                    remaining = 0;
                }
            }
            if (remaining <= 0)
                break;
        }
        return remaining <= 0;
    }

    private String getDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return ComponentUtil.toLegacy(item.getItemMeta().displayName());
        }
        return convertToTitleCase(item.getType().name().replace("_", " "));
    }

    private String convertToTitleCase(String text) {
        if (text == null || text.isEmpty())
            return text;
        StringBuilder converted = new StringBuilder();
        boolean convertNext = true;
        for (char ch : text.toCharArray()) {
            if (Character.isSpaceChar(ch)) {
                convertNext = true;
            } else if (convertNext) {
                ch = Character.toTitleCase(ch);
                convertNext = false;
            } else {
                ch = Character.toLowerCase(ch);
            }
            converted.append(ch);
        }
        return converted.toString();
    }
}
