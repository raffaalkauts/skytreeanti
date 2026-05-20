package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.util.ComponentUtil;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import org.bukkit.inventory.ItemStack;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Simple placeholder listener for a shop GUI.
 * It expects items in the inventory to have two PersistentDataContainer keys:
 * - "shop_price" (double) – total price for the transaction
 * - "shop_item" (String) – the item identifier in the ItemRegistry
 *
 * The listener validates the player's balance, deducts the price, gives the
 * item,
 * and provides basic feedback. This implementation is intentionally minimal and
 * can be expanded with pagination, categories, etc.
 */
public class ShopGUIListener implements Listener {

    private final SkytreePlugin plugin;
    private final EconomyService economy;
    private final ItemRegistry itemRegistry;
    private final com.wiredid.skytree.impl.SkytreeShopService shopService;
    private final NamespacedKey priceKey;
    private final NamespacedKey sellKey;
    private final NamespacedKey itemKey;
    private final NamespacedKey categoryKey;

    public ShopGUIListener(SkytreePlugin plugin, EconomyService economy, ItemRegistry itemRegistry,
            com.wiredid.skytree.api.ShopService shopService) {
        this.plugin = plugin;
        this.economy = economy;
        this.itemRegistry = itemRegistry;
        if (shopService instanceof com.wiredid.skytree.impl.SkytreeShopService) {
            this.shopService = (com.wiredid.skytree.impl.SkytreeShopService) shopService;
        } else {
            this.shopService = null;
        }

        this.priceKey = new NamespacedKey(plugin, "shop_price");
        this.sellKey = new NamespacedKey(plugin, "shop_sell");
        this.itemKey = new NamespacedKey(plugin, "shop_item");
        this.categoryKey = new NamespacedKey(plugin, "shop_category");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        String title = ComponentUtil.toLegacy(event.getView().title());

        // Check if it's a shop (Main menu "Shop Categories" or Submenu "Shop - ...")
        if (!title.contains("Shop"))
            return;

        event.setCancelled(true); // Prevent moving items

        if (event.getClickedInventory() == player.getInventory())
            return; // Allow clicking own inv (but canceled above? actually strict cancel usually
                    // better)

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        PersistentDataContainer data = clicked.getItemMeta().getPersistentDataContainer();

        // 1. Category Navigation
        if (data.has(categoryKey, PersistentDataType.STRING)) {
            String cat = data.get(categoryKey, PersistentDataType.STRING);
            if (shopService != null) {
                shopService.openCategory(player, cat);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        // 2. Back Button or Close
        // Usually handled by generic checks or specific item names.
        // SkytreeShopService adds items named "Back to Menu" or "Close"
        String itemName = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());
        if (itemName.contains("Back to Menu")) {
            if (shopService != null)
                shopService.openShop(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }
        if (itemName.contains("Close")) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        // 3. Buy/Sell Interaction
        if (!data.has(itemKey, PersistentDataType.STRING))
            return; // Not a shop item

        String itemId = data.get(itemKey, PersistentDataType.STRING);

        // Right Click = SELL
        if (event.isRightClick()) {
            if (!data.has(sellKey, PersistentDataType.DOUBLE))
                return;
            double sellPrice = data.get(sellKey, PersistentDataType.DOUBLE);

            if (sellPrice <= 0) {
                player.sendMessage(ComponentUtil.smartParse("§cThis item cannot be sold."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Check if player has the item
            ItemStack toSell = resolveItem(itemId);
            if (toSell == null)
                return;

            // Check inventory for ONE of this item
            if (!player.getInventory().containsAtLeast(toSell, 1)) {
                player.sendMessage(ComponentUtil.smartParse("§cYou don't have any matching items to sell!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Sell Logic (1 at a time or all? Shift click?)
            // Simple: Sell 1 per click for now, or 64 if shift?
            // User didn't specify, sticking to simple.
            // "Right Click to Sell" usually implies 1 unless Shift used.
            int amount = 1;
            if (event.isShiftClick())
                amount = 64; // Try removing max stack

            // Count actual amount player has
            int playerHas = 0;
            for (ItemStack i : player.getInventory().getContents()) {
                if (i != null && i.isSimilar(toSell)) {
                    playerHas += i.getAmount();
                }
            }

            int sellAmount = Math.min(amount, playerHas);
            if (sellAmount == 0)
                return; // Should be caught by containsAtLeast but safe check

            // Remove items
            ItemStack removal = toSell.clone();
            removal.setAmount(sellAmount);
            player.getInventory().removeItem(removal);

            double totalEarn = sellPrice * sellAmount;
            economy.addBalance(player.getUniqueId(), totalEarn);

            player.sendMessage(ComponentUtil
                    .smartParse("§aSold " + sellAmount + "x " + itemId + " for §e" + economy.format(totalEarn)));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        } else {
            // Left Click = BUY
            if (!data.has(priceKey, PersistentDataType.DOUBLE))
                return;
            double buyPrice = data.get(priceKey, PersistentDataType.DOUBLE);

            if (buyPrice <= 0) {
                player.sendMessage(ComponentUtil.smartParse("§cThis item cannot be purchased."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (economy.getBalance(player.getUniqueId()) < buyPrice) {
                player.sendMessage(ComponentUtil.smartParse("§cInsufficient funds! Cost: " + economy.format(buyPrice)));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Check space
            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage(ComponentUtil.smartParse("§cYour inventory is full!"));
                return;
            }

            economy.removeBalance(player.getUniqueId(), buyPrice);
            ItemStack toGive = resolveItem(itemId);
            if (toGive != null) {
                if (plugin.getWorthService() != null) {
                    plugin.getWorthService().updateItemLore(toGive);
                }
                player.getInventory().addItem(toGive);
                player.sendMessage(
                        ComponentUtil.smartParse("§aPurchased 1x " + itemId + " for §e" + economy.format(buyPrice)));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            }
        }
    }

    private ItemStack resolveItem(String id) {
        // 1. Try item registry
        if (itemRegistry.getItem(id) != null) {
            return itemRegistry.getItem(id).clone();
        }

        // 2. Handle Spawners
        if (id.startsWith("spawner_")) {
            String mobName = id.replace("spawner_", "").toUpperCase();
            try {
                org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(mobName);
                ItemStack item = new ItemStack(Material.SPAWNER);
                org.bukkit.inventory.meta.BlockStateMeta bsm = (org.bukkit.inventory.meta.BlockStateMeta) item
                        .getItemMeta();
                org.bukkit.block.CreatureSpawner cs = (org.bukkit.block.CreatureSpawner) bsm.getBlockState();
                cs.setSpawnedType(type);
                bsm.setBlockState(cs);

                String displayName = mobName.charAt(0) + mobName.substring(1).toLowerCase().replace("_", " ");
                bsm.displayName(ComponentUtil.smartParse("§e" + displayName + " Spawner"));
                item.setItemMeta(bsm);
                return item;
            } catch (Exception e) {
            }
        }

        // 3. Material
        Material mat = Material.matchMaterial(id);
        if (mat != null)
            return new ItemStack(mat);
        return null;
    }
}
