package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.gui.SkytreeGuide;
import com.wiredid.skytree.gui.StructureGUI;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class GuideListener implements Listener {

    private final SkytreePlugin plugin;
    private final SkytreeGuide guide;
    private final StructureGUI structureGUI;
    private final com.wiredid.skytree.gui.RecipeViewerGUI recipeViewerGUI;
    private final NamespacedKey guideCategoryKey;
    private final CatalogListener catalogListener;

    public GuideListener(SkytreePlugin plugin, ItemRegistry itemRegistry, CatalogListener catalogListener) {
        this.plugin = plugin;
        this.guide = new SkytreeGuide(plugin, itemRegistry);
        this.structureGUI = new StructureGUI(plugin);
        this.recipeViewerGUI = new com.wiredid.skytree.gui.RecipeViewerGUI(plugin, itemRegistry, plugin.getRecipeService());
        this.guideCategoryKey = new NamespacedKey(plugin, "guide_category");
        this.catalogListener = catalogListener;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String rawTitle = ComponentUtil.stripColor(event.getView().title());
        if (rawTitle == null) return;
        String title = rawTitle.toLowerCase();

        if (!title.contains("guide") && !title.contains("recipe") && !title.contains("quest")
                && !title.startsWith("structure:"))
            return;

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Don't cancel when clicking in player's own inventory
        if (event.getClickedInventory() == player.getInventory())
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        boolean isBackArrow = clicked.getType() == Material.ARROW && clicked.hasItemMeta()
                && clicked.getItemMeta().hasDisplayName()
                && ComponentUtil.toLegacy(clicked.getItemMeta().displayName())
                        .replaceAll("§[0-9a-fk-or]", "").contains("Back");

        // ── MAIN MENU ─────────────────────────────────────
        if (title.contains("skytree guide")) {
            if (isBackArrow) { guide.open(player); return; }
            var pdc = clicked.getItemMeta().getPersistentDataContainer();
            if (pdc.has(guideCategoryKey, PersistentDataType.STRING)) {
                switch (pdc.get(guideCategoryKey, PersistentDataType.STRING)) {
                    case "MENU_RECIPES" -> guide.openRecipeCategories(player);
                    case "MENU_MACHINES" -> guide.openMachineGuide(player);
                    case "MENU_QUESTS" -> guide.openQuestTutorial(player);
                }
            }
            return;
        }

        // ── RECIPE CATEGORIES ─────────────────────────────
        if (title.contains("skytree recipes")) {
            if (isBackArrow) { guide.open(player); return; }
            var pdc = clicked.getItemMeta().getPersistentDataContainer();
            if (pdc.has(guideCategoryKey, PersistentDataType.STRING)) {
                String catId = pdc.get(guideCategoryKey, PersistentDataType.STRING);
                if (!catId.startsWith("MENU_")) guide.openCategory(player, catId);
            }
            return;
        }

        // ── MACHINE GUIDE ─────────────────────────────────
        if (title.contains("machine building guide")) {
            if (isBackArrow) { guide.open(player); return; }
            if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                String dn = ComponentUtil.toLegacy(clicked.getItemMeta().displayName())
                        .replaceAll("§[0-9a-fk-or]", "").toLowerCase();
                switch (dn) {
                    case "sieve" -> structureGUI.open(player, "sieve");
                    case "barrel", "composting barrel", "wooden barrel" -> structureGUI.open(player, "barrel");
                    case "crucible", "porcelain crucible" -> structureGUI.open(player, "crucible");
                    case "compressor" -> structureGUI.open(player, "compressor");
                    case "pulverizer" -> structureGUI.open(player, "pulverizer");
                    case "advanced furnace", "electric furnace" -> structureGUI.open(player, "furnace_advanced");
                    case "cobblestone generator", "cobble generator" -> structureGUI.open(player, "cobble_gen");
                }
            }
            return;
        }

        // ── CATEGORY ITEMS PAGE ──────────────────────────
        if (title.contains("guide:")) {
            if (isBackArrow) { guide.openRecipeCategories(player); return; }

            String displayName = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                    ? ComponentUtil.toLegacy(clicked.getItemMeta().displayName()).replaceAll("§[0-9a-fk-or]", "")
                    : "";

            switch (displayName.toLowerCase()) {
                case "sieve" -> structureGUI.open(player, "sieve");
                case "barrel", "composting barrel", "wooden barrel" -> structureGUI.open(player, "barrel");
                case "crucible", "porcelain crucible" -> structureGUI.open(player, "crucible");
                case "compressor" -> structureGUI.open(player, "compressor");
                case "pulverizer" -> structureGUI.open(player, "pulverizer");
                case "advanced furnace" -> structureGUI.open(player, "advanced_furnace");
                default -> {
                    NamespacedKey itemKey = new NamespacedKey(plugin, "item_id");
                    var pdc = clicked.getItemMeta().getPersistentDataContainer();
                    if (pdc.has(itemKey, PersistentDataType.STRING)) {
                        recipeViewerGUI.open(player, pdc.get(itemKey, PersistentDataType.STRING));
                    } else {
                        player.sendMessage("§e[Guide] §7" + displayName);
                        player.sendMessage("§7Get from: §a/shop §7- Sieve §7- Machines");
                    }
                }
            }
            return;
        }

        // ── RECIPE VIEWER ────────────────────────────────
        if (title.contains("recipe:")) {
            if (isBackArrow) {
                if (catalogListener != null && catalogListener.isInCatalogRecipeView(player.getUniqueId())) {
                    catalogListener.removeCatalogRecipeView(player.getUniqueId());
                    player.performCommand("items");
                } else {
                    guide.open(player);
                }
            }
            return;
        }

        // ── STRUCTURE VIEW ──────────────────────────────
        if (title.startsWith("structure:")) {
            if (isBackArrow) { guide.open(player); }
        }
    }
}
