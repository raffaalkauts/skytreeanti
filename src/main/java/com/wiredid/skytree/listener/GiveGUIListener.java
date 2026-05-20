package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.impl.SkytreeItemRegistry;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles the Visual Give GUI
 */
public class GiveGUIListener implements Listener {

    private final SkytreePlugin plugin;
    private final ItemRegistry itemRegistry;
    private static final String GUI_PREFIX = "§8Item Browser";

    public GiveGUIListener(SkytreePlugin plugin, ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
    }

    public void openGUI(Player player, int page) {
        openGUI(player, page, false); // Default to Singe Mode
    }

    public void openGUI(Player player, int page, boolean stackMode) {
        String modeTitle = stackMode ? " (STACK)" : " (SINGLE)";
        Inventory gui = Bukkit.createInventory(null, 54,
                ComponentUtil.parse(GUI_PREFIX + modeTitle + " (Page " + (page + 1) + ")"));

        List<String> items = new ArrayList<>(((SkytreeItemRegistry) itemRegistry).getAllItemIds());
        Collections.sort(items);

        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            String itemId = items.get(i);
            ItemStack item = itemRegistry.getItem(itemId);

            // Add identifying NBT for the GUI click
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "give_item_id"), PersistentDataType.STRING,
                    itemId);
            // Add click instructions
            List<String> lore = meta.hasLore() ? meta.lore().stream().map(ComponentUtil::toLegacy).toList()
                    : new ArrayList<>();
            List<String> newLore = new ArrayList<>(lore);
            newLore.add("");
            if (stackMode) {
                newLore.add("§eClick: §fGet 64");
            } else {
                newLore.add("§eClick: §fGet 1");
            }
            newLore.add("§eShift-Click: §fGive to everyone (1)");
            // Store stack mode in item NBT for proper state tracking
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "give_item_stack_mode"),
                    PersistentDataType.INTEGER, stackMode ? 1 : 0);
            meta.lore(ComponentUtil.parseList(newLore));

            item.setItemMeta(meta);
            gui.setItem(slot++, item);
        }

        // Navigation
        if (page > 0) {
            gui.setItem(45, createNavButton("Previous Page", page - 1, stackMode));
        }
        if (endIndex < items.size()) {
            gui.setItem(53, createNavButton("Next Page", page + 1, stackMode));
        }

        // Stack Mode Toggle
        gui.setItem(51, createStackModeButton(stackMode, page));

        gui.setItem(49, createCloseButton());

        player.openInventory(gui);
    }

    private ItemStack createStackModeButton(boolean stackMode, int page) {
        Material mat = stackMode ? Material.BLAZE_ROD : Material.STICK;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(stackMode ? "§6§lMode: 64x (STACK)" : "§f§lMode: 1x (SINGLE)"));
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to toggle amount.");
        meta.lore(ComponentUtil.parseList(lore));

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "give_stack_mode"), PersistentDataType.INTEGER,
                stackMode ? 1 : 0);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "gui_page"), PersistentDataType.INTEGER, page);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavButton(String name, int targetPage, boolean stackMode) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§e" + name));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "gui_page"), PersistentDataType.INTEGER,
                targetPage);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "give_stack_mode_nav"),
                PersistentDataType.INTEGER, stackMode ? 1 : 0);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§cClose"));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        String title = ComponentUtil.toLegacy(event.getView().title());

        if (!title.startsWith(GUI_PREFIX))
            return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        // Toggle Stack Mode
        if (clicked.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "give_stack_mode"),
                PersistentDataType.INTEGER)) {
            int currentMode = clicked.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "give_stack_mode"), PersistentDataType.INTEGER);
            int page = clicked.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "gui_page"),
                    PersistentDataType.INTEGER);
            boolean newMode = currentMode == 0; // Toggle

            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            openGUI(player, page, newMode);
            return;
        }

        // Navigation
        if (clicked.getType() == Material.ARROW && clicked.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "gui_page"), PersistentDataType.INTEGER)) {
            int page = clicked.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "gui_page"),
                    PersistentDataType.INTEGER);
            int stackModeInt = clicked.getItemMeta().getPersistentDataContainer()
                    .getOrDefault(new NamespacedKey(plugin, "give_stack_mode_nav"), PersistentDataType.INTEGER, 0);
            openGUI(player, page, stackModeInt == 1);
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Give Item
        if (clicked.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "give_item_id"),
                PersistentDataType.STRING)) {
            String itemId = clicked.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "give_item_id"), PersistentDataType.STRING);
            ItemStack original = itemRegistry.getItem(itemId);

            if (original != null) {
                // Retrieve stack mode from item NBT (properly stored during GUI creation)
                boolean isStackMode = clicked.getItemMeta().getPersistentDataContainer()
                        .getOrDefault(new NamespacedKey(plugin, "give_item_stack_mode"),
                                PersistentDataType.INTEGER, 0) == 1;

                if (event.isShiftClick()) {
                    // Give to everyone (Always 1)
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        ItemStack itemToGive = original.clone();
                        plugin.getWorthService().updateItemLore(itemToGive);
                        java.util.Map<Integer, ItemStack> left = p.getInventory().addItem(itemToGive);
                        if (!left.isEmpty()) {
                            left.values().forEach(item -> p.getWorld().dropItemNaturally(p.getLocation(), item));
                        }
                        p.sendMessage("§a[Skytree] You received " + itemId + "!");
                    }
                    player.sendMessage("§aGave " + itemId + " to all players.");
                } else {
                    ItemStack stack = original.clone();
                    if (isStackMode) {
                        stack.setAmount(64);
                    } else {
                        stack.setAmount(1);
                    }
                    plugin.getWorthService().updateItemLore(stack);
                    java.util.Map<Integer, ItemStack> left = player.getInventory().addItem(stack);
                    if (!left.isEmpty()) {
                        left.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                        player.sendMessage("§cInventory full! Item dropped at your feet.");
                    }
                    player.sendMessage("§aReceived " + stack.getAmount() + "x " + itemId);
                }
            }
        }
    }
}
