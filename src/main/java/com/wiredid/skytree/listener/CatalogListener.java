package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.api.WorthService;
import com.wiredid.skytree.gui.ItemCatalogGUI;
import com.wiredid.skytree.gui.RecipeViewerGUI;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CatalogListener implements Listener {

    private final SkytreePlugin plugin;
    private final ItemCatalogGUI catalogGUI;
    private final RecipeViewerGUI recipeViewerGUI;
    private final NamespacedKey catalogKey;

    // Track players who opened RecipeViewer from Catalog so Back → Catalog not Guide
    private final Set<UUID> catalogRecipeViewers = new HashSet<>();

    public CatalogListener(SkytreePlugin plugin, ItemRegistry itemRegistry, WorthService worthService) {
        this.plugin = plugin;
        this.catalogGUI = new ItemCatalogGUI(plugin, itemRegistry, worthService);
        this.recipeViewerGUI = new RecipeViewerGUI(plugin, itemRegistry, plugin.getRecipeService());
        this.catalogKey = catalogGUI.getCatalogKey();
    }

    public boolean isInCatalogRecipeView(UUID uuid) {
        return catalogRecipeViewers.contains(uuid);
    }

    public void removeCatalogRecipeView(UUID uuid) {
        catalogRecipeViewers.remove(uuid);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = ComponentUtil.stripColor(event.getView().title());
        if (title == null) return;

        // Match both main catalog menu and category pages
        boolean isCatalog = title.contains("Item Catalog") || title.contains("Page");
        if (!isCatalog) return;

        // Don't cancel when clicking in player's own inventory
        if (event.getClickedInventory() == player.getInventory())
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Skip glass panes (borders)
        if (clicked.getType().name().contains("STAINED_GLASS_PANE")) return;
        // Skip filler arrows that are not our navigation
        if (clicked.getType() == Material.ARROW && (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName())) return;

        if (!clicked.hasItemMeta()) return;
        var pdc = clicked.getItemMeta().getPersistentDataContainer();

        // Catalog navigation actions
        if (pdc.has(catalogKey, PersistentDataType.STRING)) {
            String action = pdc.get(catalogKey, PersistentDataType.STRING);
            if (action.startsWith("cat_")) {
                catalogGUI.openCategory(player, action.substring(4), 1);
            } else if (action.startsWith("prev_")) {
                String[] parts = action.split("_", 3);
                String catId = parts[1];
                int page = Integer.parseInt(parts[2]) - 1;
                catalogGUI.openCategory(player, catId, page);
            } else if (action.startsWith("next_")) {
                String[] parts = action.split("_", 3);
                String catId = parts[1];
                int page = Integer.parseInt(parts[2]) + 1;
                catalogGUI.openCategory(player, catId, page);
            } else if (action.equals("back")) {
                catalogGUI.open(player);
            }
            return;
        }

        // Item click → open RecipeViewer with catalog context
        NamespacedKey itemKey = new NamespacedKey(plugin, "item_id");
        if (pdc.has(itemKey, PersistentDataType.STRING)) {
            String itemId = pdc.get(itemKey, PersistentDataType.STRING);
            catalogRecipeViewers.add(player.getUniqueId());
            recipeViewerGUI.open(player, itemId);
        }
    }

    // Clean up tracking when inventory closes
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = ComponentUtil.stripColor(event.getView().title());
        if (title != null && title.contains("Recipe:")) {
            catalogRecipeViewers.remove(event.getPlayer().getUniqueId());
        }
    }
}
