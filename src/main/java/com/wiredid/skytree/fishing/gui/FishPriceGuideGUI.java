package com.wiredid.skytree.fishing.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.fishing.FishingModels.Fish;
import com.wiredid.skytree.fishing.FishingModels.Rarity;
import com.wiredid.skytree.fishing.FishingService;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class FishPriceGuideGUI implements Listener {

    private final FishingService fishingService;

    public FishPriceGuideGUI(SkytreePlugin plugin, FishingService fishingService) {
        this.fishingService = fishingService;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§2§lFish Price Guide"));

        // Fill background
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        // Add Rarity Icons
        inv.setItem(10, createRarityIcon(Rarity.COMMON));
        inv.setItem(11, createRarityIcon(Rarity.UNCOMMON));
        inv.setItem(12, createRarityIcon(Rarity.RARE));
        inv.setItem(13, createRarityIcon(Rarity.LEGEND));
        inv.setItem(14, createRarityIcon(Rarity.MYTHIC));
        inv.setItem(15, createRarityIcon(Rarity.LIMITED));

        player.openInventory(inv);
    }

    private ItemStack createRarityIcon(Rarity rarity) {
        Material mat;
        switch (rarity) {
            case COMMON -> mat = Material.WHITE_STAINED_GLASS_PANE;
            case UNCOMMON -> mat = Material.LIME_STAINED_GLASS_PANE;
            case RARE -> mat = Material.BLUE_STAINED_GLASS_PANE;
            case LEGEND -> mat = Material.ORANGE_STAINED_GLASS_PANE;
            case MYTHIC -> mat = Material.PURPLE_STAINED_GLASS_PANE;
            case LIMITED -> mat = Material.BLACK_STAINED_GLASS_PANE;
            default -> mat = Material.PAPER;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Component nameComp = ComponentUtil.smartParse(rarity.getColor())
                    .append(Component.text(" "))
                    .append(ComponentUtil.parse("§l" + rarity.getDisplayName() + " FISH"));
            meta.displayName(nameComp);
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to view prices");
            meta.lore(ComponentUtil.parseList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().title().equals(Component.text("§2§lFish Price Guide"))) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR || !item.hasItemMeta())
                return;

            String name = ComponentUtil.toLegacy(item.getItemMeta().displayName());
            if (name.contains("COMMON"))
                openRarityDetails((Player) event.getWhoClicked(), Rarity.COMMON);
            else if (name.contains("UNCOMMON"))
                openRarityDetails((Player) event.getWhoClicked(), Rarity.UNCOMMON);
            else if (name.contains("RARE"))
                openRarityDetails((Player) event.getWhoClicked(), Rarity.RARE);
            else if (name.contains("LEGEND"))
                openRarityDetails((Player) event.getWhoClicked(), Rarity.LEGEND);
            else if (name.contains("MYTHIC"))
                openRarityDetails((Player) event.getWhoClicked(), Rarity.MYTHIC);
            else if (name.contains("LIMITED"))
                openRarityDetails((Player) event.getWhoClicked(), Rarity.LIMITED);
        } else if (ComponentUtil.toLegacy(event.getView().title()).contains("Prices")) {
            event.setCancelled(true);
            if (event.getRawSlot() == 45) { // Back button
                open((Player) event.getWhoClicked());
            }
        }
    }

    private void openRarityDetails(Player player, Rarity rarity) {
        List<Fish> fishList = fishingService.getFishByRarity(rarity);
        int size = ((fishList.size() / 9) + 2) * 9;
        if (size > 54)
            size = 54;
        if (size < 9)
            size = 9;

        Inventory inv = Bukkit.createInventory(null, size, Component.text(rarity.getDisplayName() + " Fish Prices"));

        for (int i = 0; i < Math.min(fishList.size(), 45); i++) {
            Fish f = fishList.get(i);
            ItemStack item = new ItemStack(f.material());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(ComponentUtil.smartParse(rarity.getColor() + f.id().replace("_", " ").toUpperCase()));
                List<String> lore = new ArrayList<>();
                lore.add("§7Base Price: §e" + String.format("%.2f", f.basePrice()) + " USDT");
                lore.add("§fPrice: §e" + NumberUtil.formatCurrency(f.basePrice()) + " USDT (Base)");
                lore.add("§7Weight: §f" + f.minWeight() + "kg - " + f.maxWeight() + "kg");
                meta.lore(ComponentUtil.parseList(lore));
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        // Back button
        ItemStack back = createItem(Material.ARROW, "§c§lBACK");
        inv.setItem(size - 9, back);

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ComponentUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
