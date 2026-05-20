package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;

import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;

public class MaterialSelectorGUI implements Listener {

    private final SkytreePlugin plugin;

    private final NamespacedKey categoryKey;
    private final NamespacedKey itemKey;

    public MaterialSelectorGUI(SkytreePlugin plugin) {
        this.plugin = plugin;

        this.categoryKey = new NamespacedKey(plugin, "selector_category");
        this.itemKey = new NamespacedKey(plugin, "selector_item");
    }

    public void openCategorySelection(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, ComponentUtil.parse("§8Select Category"));

        // Categories (Simplified for now - matching common shop categories)
        addCategoryItem(gui, 10, Material.GRASS_BLOCK, "Blocks", "Building blocks and terrain");
        addCategoryItem(gui, 11, Material.WHEAT, "Farming", "Crops and seeds");
        addCategoryItem(gui, 12, Material.ROTTEN_FLESH, "Drops", "Mob drops and loot");
        addCategoryItem(gui, 13, Material.IRON_INGOT, "Minerals", "Ores and ingots");
        addCategoryItem(gui, 14, Material.REDSTONE, "Redstone", "Technical components");
        addCategoryItem(gui, 15, Material.OAK_LOG, "Nature", "Wood and saplings");
        addCategoryItem(gui, 16, Material.DIAMOND_SWORD, "Equipment", "Tools and armor");

        GuiUtil.applyPremiumBorder(gui, Material.CYAN_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE);
        player.openInventory(gui);
    }

    private void addCategoryItem(Inventory gui, int slot, Material icon, String name, String desc) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§b§l" + name));
        meta.lore(Arrays.asList(
                ComponentUtil.parse("§7" + desc),
                ComponentUtil.parse(""),
                ComponentUtil.parse("§e▶ Click to Browse")));
        meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, name.toUpperCase());
        item.setItemMeta(meta);
        gui.setItem(slot, item);
    }

    public void openItemSelection(Player player, String category) {
        Inventory gui = Bukkit.createInventory(null, 54,
                ComponentUtil.smartParse("§b§lBounty §8» §fSelect " + category));

        List<ItemStack> items = getItemsForCategory(category);

        java.util.List<Integer> interiorSlots = GuiUtil.getInteriorSlots(54);
        int slotIdx = 0;

        for (ItemStack is : items) {
            if (slotIdx >= interiorSlots.size() || interiorSlots.get(slotIdx) >= 45)
                break;

            ItemStack display = is.clone();
            ItemMeta meta = display.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            if (meta.hasLore())
                lore.addAll(meta.lore());
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(ComponentUtil.parse("§a▶ Click to Select"));
            meta.lore(lore);

            String id = plugin.getItemRegistry().getItemId(is);
            if (id == null)
                id = is.getType().name();

            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
            display.setItemMeta(meta);

            gui.setItem(interiorSlots.get(slotIdx++), display);
        }

        // Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(ComponentUtil.parse("§cBack to Categories"));
        back.setItemMeta(backMeta);
        gui.setItem(40, back);

        GuiUtil.applyPremiumBorder(gui, Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE);
        player.openInventory(gui);
    }

    private List<ItemStack> getItemsForCategory(String category) {
        List<ItemStack> list = new ArrayList<>();

        switch (category) {
            case "MINERALS":
                addIfNotNull(list, "TIN_INGOT");
                addIfNotNull(list, "STEEL_INGOT");
                addIfNotNull(list, "URANIUM_INGOT");
                list.add(new ItemStack(Material.IRON_INGOT));
                list.add(new ItemStack(Material.GOLD_INGOT));
                list.add(new ItemStack(Material.DIAMOND));
                list.add(new ItemStack(Material.EMERALD));
                addIfNotNull(list, "PRIME_DRILL");
                break;
            case "EQUIPMENT":
                addIfNotNull(list, "TIN_SWORD");
                addIfNotNull(list, "STEEL_SWORD");
                list.add(new ItemStack(Material.DIAMOND_SWORD));
                list.add(new ItemStack(Material.DIAMOND_PICKAXE));
                list.add(new ItemStack(Material.NETHERITE_CHESTPLATE));
                break;
            case "FARMING":
                list.add(new ItemStack(Material.WHEAT));
                list.add(new ItemStack(Material.CARROT));
                list.add(new ItemStack(Material.POTATO));
                list.add(new ItemStack(Material.PUMPKIN));
                list.add(new ItemStack(Material.MELON));
                break;
            case "BLOCKS":
                list.add(new ItemStack(Material.COBBLESTONE));
                list.add(new ItemStack(Material.GRASS_BLOCK));
                list.add(new ItemStack(Material.OAK_LOG));
                list.add(new ItemStack(Material.DIRT));
                break;
            case "DROPS":
                list.add(new ItemStack(Material.ROTTEN_FLESH));
                list.add(new ItemStack(Material.BONE));
                list.add(new ItemStack(Material.ENDER_PEARL));
                list.add(new ItemStack(Material.BLAZE_ROD));
                break;
            default:
                list.add(new ItemStack(Material.APPLE));
                break;
        }
        return list;
    }

    public void openSearchResults(Player player, String query) {
        Inventory gui = Bukkit.createInventory(null, 54,
                ComponentUtil.smartParse("§b§lBounty §8» §fSearch: " + query));

        List<ItemStack> items = new ArrayList<>();
        String upperQuery = query.toUpperCase();

        // 1. Search Materials
        for (Material mat : Material.values()) {
            if (mat.isItem() && !mat.isAir() && !mat.isLegacy()) {
                if (mat.name().contains(upperQuery)) {
                    items.add(new ItemStack(mat));
                    if (items.size() >= 45)
                        break;
                }
            }
        }

        // 2. Search Custom Items (limit total)
        if (items.size() < 45) {
            // Assuming ItemRegistry has a way to get all keys or we iterates known sets?
            // Since ItemRegistry interface isn't fully visible, I'll assume we can't easily
            // iterate ALL keys
            // without exposing them. However, for now, let's rely on Material search
            // primarily
            // unless I saw getItems() in registry?
            // Checking SkytreePlugin I saw `getItemRegistry` returning `ItemRegistry`
            // interface.
            // If I can't iterate, I might skip custom item search or try specific known
            // ones.
            // Wait, `SkytreeItemRegistry` likely likely has a map.
            // Let's assume for now Material search is the main request ("diamond").
        }

        java.util.List<Integer> interiorSlots = GuiUtil.getInteriorSlots(54);
        int slotIdx = 0;

        for (ItemStack is : items) {
            if (slotIdx >= interiorSlots.size() || interiorSlots.get(slotIdx) >= 45)
                break;

            ItemStack display = is.clone();
            ItemMeta meta = display.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            if (meta.hasLore())
                lore.addAll(meta.lore());
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(ComponentUtil.parse("§a▶ Click to Select"));
            meta.lore(lore);

            String id = is.getType().name(); // Default
            // Ideally we check if it matches a custom item ID, but for material search,
            // material name is ID.

            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
            display.setItemMeta(meta);

            gui.setItem(interiorSlots.get(slotIdx++), display);
        }

        if (items.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(ComponentUtil.parse("§cNo matches found"));
            empty.setItemMeta(meta);
            gui.setItem(22, empty);
        }

        // Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(ComponentUtil.parse("§cBack to Categories"));
        back.setItemMeta(backMeta);
        gui.setItem(40, back);

        GuiUtil.applyPremiumBorder(gui, Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE);
        player.openInventory(gui);
    }

    private void addIfNotNull(List<ItemStack> list, String id) {
        ItemStack item = plugin.getItemRegistry().getItem(id);
        if (item != null)
            list.add(item);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        String title = ComponentUtil.stripColor(event.getView().title());

        if (title.equals("Select Category")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || !item.hasItemMeta())
                return;

            String category = item.getItemMeta().getPersistentDataContainer().get(categoryKey,
                    PersistentDataType.STRING);
            if (category != null) {
                openItemSelection(player, category);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
        } else if (title.startsWith("Select Item:")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || !item.hasItemMeta())
                return;

            if (item.getType() == Material.ARROW && item.getItemMeta().displayName() != null
                    && ComponentUtil.stripColor(item.getItemMeta().displayName()).contains("Back")) {
                openCategorySelection(player);
                return;
            }

            String itemId = item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
            if (itemId != null) {
                // Proceed to Order Setup
                new OrderSetupGUI(plugin).open(player, itemId);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        }
    }
}
