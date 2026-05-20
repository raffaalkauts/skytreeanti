package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.RecipeService;
import com.wiredid.skytree.machine.MachineProcessor;
import com.wiredid.skytree.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoCrafterGUI implements Listener {

    private final SkytreePlugin plugin;
    private final MachineProcessor machineProcessor;
    private final NamespacedKey locKey;
    private final NamespacedKey recipeKey;

    public AutoCrafterGUI(SkytreePlugin plugin, MachineProcessor machineProcessor) {
        this.plugin = plugin;
        this.machineProcessor = machineProcessor;
        this.locKey = new NamespacedKey(plugin, "machine_loc");
        this.recipeKey = new NamespacedKey(plugin, "recipe_id");
    }

    public void open(Player player, Location loc) {
        Inventory inv = Bukkit.createInventory(null, 54, ComponentUtil.parse("§6§lAuto-Crafter Recipes"));

        // Fill background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        gMeta.displayName(ComponentUtil.parse(" "));
        glass.setItemMeta(gMeta);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, glass);

        Map<String, RecipeService.AutoCrafterRecipe> recipes = plugin.getRecipeService().getAutoCrafterRecipes();
        int slot = 0;

        MachineProcessor.MachineData data = machineProcessor.getMachineData(loc);
        String currentRecipe = data != null ? data.getMeta() : null;

        for (Map.Entry<String, RecipeService.AutoCrafterRecipe> entry : recipes.entrySet()) {
            if (slot >= 45)
                break; // Slot limit for now

            RecipeService.AutoCrafterRecipe recipe = entry.getValue();
            ItemStack item = recipe.getOutput().clone();
            ItemMeta meta = item.getItemMeta();

            List<Component> lore = new ArrayList<>();
            lore.add(ComponentUtil.parse("§7Ingredients:"));
            for (ItemStack in : recipe.getInputs()) {
                lore.add(ComponentUtil.parse("§8- §f" + in.getType().name() + " x" + in.getAmount()));
            }
            lore.add(ComponentUtil.parse("§7Time: §e" + recipe.getProcessTime() + " ticks"));
            lore.add(ComponentUtil.parse(""));

            if (entry.getKey().equals(currentRecipe)) {
                lore.add(ComponentUtil.parse("§a§lACTIVE RECIPE"));
            } else {
                lore.add(ComponentUtil.parse("§eClick to select"));
            }

            meta.lore(lore);
            meta.getPersistentDataContainer().set(locKey, PersistentDataType.STRING, serializeLoc(loc));
            meta.getPersistentDataContainer().set(recipeKey, PersistentDataType.STRING, entry.getKey());

            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        // Status Item
        ItemStack status = new ItemStack(Material.BOOK);
        ItemMeta sMeta = status.getItemMeta();
        sMeta.displayName(
                ComponentUtil.parse("§eCurrent Recipe: §f" + (currentRecipe != null ? currentRecipe : "None")));
        status.setItemMeta(sMeta);
        inv.setItem(49, status);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(ComponentUtil.parse("§6§lAuto-Crafter Recipes")))
            return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        Player player = (Player) event.getWhoClicked();
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        if (meta.getPersistentDataContainer().has(recipeKey, PersistentDataType.STRING)) {
            String locStr = meta.getPersistentDataContainer().get(locKey, PersistentDataType.STRING);
            String rid = meta.getPersistentDataContainer().get(recipeKey, PersistentDataType.STRING);
            if (rid == null || rid.isBlank()) {
                return;
            }

            Location loc = deserializeLoc(locStr);
            if (loc == null)
                return;

            machineProcessor.updateMachineMeta(loc, rid);
            player.sendMessage("§a[Auto-Crafter] §fRecipe set to: " + rid);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();
        }
    }

    private String serializeLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location deserializeLoc(String str) {
        if (str == null)
            return null;
        String[] parts = str.split(",");
        return new Location(Bukkit.getWorld(parts[0]),
                Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
}
