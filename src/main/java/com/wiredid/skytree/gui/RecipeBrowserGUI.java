package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
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

public class RecipeBrowserGUI implements Listener {

    public RecipeBrowserGUI(SkytreePlugin plugin) {
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ComponentUtil.parse("§2§lRecipe Browser"));

        // Add some example recipes (visual representation)
        addRecipeLine(inv, 10, Material.OAK_SAPLING, Material.WOODEN_HOE, Material.STRING, "Infest Leaves (Crook)");
        addRecipeLine(inv, 19, Material.COBBLESTONE, Material.WOODEN_PICKAXE, Material.GRAVEL,
                "Crush to Gravel (Hammer)");
        addRecipeLine(inv, 28, Material.GRAVEL, Material.OAK_FENCE, Material.DIAMOND, "Sift Gravel (Sieve)");

        // Fill background
        ItemStack glass = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(ComponentUtil.parse(" "));
        glass.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, glass);
        }

        player.openInventory(inv);
    }

    private void addRecipeLine(Inventory inv, int startSlot, Material input, Material tool, Material output,
            String title) {
        inv.setItem(startSlot, createIcon(input, "§aInput: " + input.name()));
        inv.setItem(startSlot + 1, createIcon(Material.ARROW, "§7+"));
        inv.setItem(startSlot + 2, createIcon(tool, "§eTool: " + tool.name()));
        inv.setItem(startSlot + 3, createIcon(Material.ARROW, "§7="));
        inv.setItem(startSlot + 4, createIcon(output, "§bOutput: " + output.name()));
        inv.setItem(startSlot + 6, createIcon(Material.BOOK, "§d§l" + title));
    }

    private ItemStack createIcon(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onRecipeClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Recipe Browser"))
            return;
        event.setCancelled(true);
    }
}
