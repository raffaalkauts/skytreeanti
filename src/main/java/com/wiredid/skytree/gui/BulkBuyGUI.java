package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

public class BulkBuyGUI implements Listener {

    private final EconomyService economy;
    private final ItemRegistry itemRegistry;
    private final NamespacedKey priceKey;
    private final NamespacedKey itemKey;

    public BulkBuyGUI(SkytreePlugin plugin, EconomyService economy, ItemRegistry itemRegistry) {

        this.economy = economy;
        this.itemRegistry = itemRegistry;
        this.priceKey = new NamespacedKey(plugin, "shop_price");
        this.itemKey = new NamespacedKey(plugin, "shop_item");
    }

    public void open(Player player, ItemStack itemToBuy, double unitPrice, String shopItemId) {
        Inventory gui = Bukkit.createInventory(null, 27,
                ComponentUtil.parse("§e§lBulk Buy: " + getItemName(itemToBuy)));

        // Background
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, bg);
        }

        // Display Item
        gui.setItem(13, itemToBuy);

        // Buy Options
        gui.setItem(10, createOption(Material.LIME_DYE, 1, unitPrice, shopItemId));
        gui.setItem(11, createOption(Material.LIME_DYE, 16, unitPrice, shopItemId));
        gui.setItem(12, createOption(Material.LIME_DYE, 32, unitPrice, shopItemId));
        gui.setItem(14, createOption(Material.LIME_DYE, 64, unitPrice, shopItemId));
        gui.setItem(15, createOption(Material.EMERALD_BLOCK, 64 * 2, unitPrice, shopItemId)); // 2 Stacks
        gui.setItem(16, createOption(Material.EMERALD_BLOCK, 64 * 9, unitPrice, shopItemId)); // 9 Stacks

        gui.setItem(26, createItem(Material.ARROW, "§cCancel"));

        player.openInventory(gui);
    }

    private ItemStack createOption(Material mat, int amount, double unitPrice, String shopItemId) {
        ItemStack item = new ItemStack(mat, Math.min(amount, 64)); // Clamp visual stack size
        ItemMeta meta = item.getItemMeta();
        double totalPrice = unitPrice * amount;

        meta.displayName(ComponentUtil.parse("§aBuy " + amount + "x"));
        meta.lore(ComponentUtil.parseList(
                "§7Unit Price: §e" + NumberUtil.formatCurrency(unitPrice),
                "",
                "§7Total: §e" + NumberUtil.formatCurrency(totalPrice),
                "",
                "§eClick to Purchase"));

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(priceKey, PersistentDataType.DOUBLE, totalPrice);
        data.set(itemKey, PersistentDataType.STRING, shopItemId + ":" + amount);

        item.setItemMeta(meta);
        return item;
    }

    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return ComponentUtil.toLegacy(item.getItemMeta().displayName());
        }
        return item.getType().name().replace("_", " ");
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ComponentUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.startsWith("§e§lBulk Buy:")) {
            return;
        }

        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        if (clicked.getType() == Material.ARROW) {
            player.closeInventory();
            return; // Or open shop again?
        }

        PersistentDataContainer data = clicked.getItemMeta().getPersistentDataContainer();
        if (!data.has(priceKey, PersistentDataType.DOUBLE))
            return;

        double totalPrice = data.get(priceKey, PersistentDataType.DOUBLE);
        String itemData = data.get(itemKey, PersistentDataType.STRING); // "id:amount"
        String[] parts = itemData.split(":");
        String shopItemId = parts[0];
        int amount = Integer.parseInt(parts[1]);

        if (economy.getBalance(player.getUniqueId()) >= totalPrice) {
            economy.removeBalance(player.getUniqueId(), totalPrice);

            // Give items
            ItemStack itemToGive = resolveShopItem(shopItemId);
            if (itemToGive != null) {
                // Handle inventory full logic if needed, but for now simple addItem
                itemToGive.setAmount(amount);

                // If amount > 64, loop
                while (amount > 0) {
                    int stackSize = Math.min(amount, itemToGive.getMaxStackSize());
                    ItemStack stack = itemToGive.clone();
                    stack.setAmount(stackSize);
                    java.util.Map<Integer, ItemStack> left = player.getInventory().addItem(stack);
                    if (!left.isEmpty()) {
                        left.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                    }
                    amount -= stackSize;
                }

                player.sendMessage(
                        "§aBought " + parts[1] + "x items for §e" + NumberUtil.formatCurrency(totalPrice));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }
        } else {
            player.sendMessage("§cNot enough money!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
        }
    }

    private ItemStack resolveShopItem(String key) {
        // Same logic as ShopGUIListener to resolve items
        // This duplication isn't ideal but works for now
        if (key.startsWith("spawner_")) {
            try {

                ItemStack spawner = new ItemStack(Material.SPAWNER);
                // In a real plugin, we'd need BlockStateMeta to set spawner type,
                // but for simple item stack it might not persist w/o placement logic.
                // Assuming MechanicsListener handles generic spawner placement or we give
                // specific item.
                // For now just basic Spawner.
                return spawner;
            } catch (Exception e) {
                return null;
            }
        } else if (itemRegistry.getAllItemIds().contains(key)) {
            return itemRegistry.getItem(key);
        } else {
            try {
                return new ItemStack(Material.valueOf(key.toUpperCase()));
            } catch (Exception e) {
                return null;
            }
        }
    }
}
