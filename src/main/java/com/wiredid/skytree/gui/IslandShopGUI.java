package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.model.IslandShop;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class IslandShopGUI {

    private final EconomyService economyService;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey AMOUNT_KEY;
    private final NamespacedKey SHOP_LOC_KEY;

    public IslandShopGUI(SkytreePlugin plugin, EconomyService economyService) {
        this.economyService = economyService;
        this.ACTION_KEY = new NamespacedKey(plugin, "island_shop_action");
        this.AMOUNT_KEY = new NamespacedKey(plugin, "island_shop_amount");
        this.SHOP_LOC_KEY = new NamespacedKey(plugin, "island_shop_loc");
    }

    public void open(Player player, IslandShop shop) {
        Inventory inv = Bukkit.createInventory(null, 27,
                ComponentUtil.smartParse("§6Island Shop: " + shop.getMaterial().name()));
        GuiUtil.applyPremiumBorder(inv, Material.BROWN_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE);

        int stock = calculateStock(shop);
        shop.setStock(stock);

        // Display Item
        ItemStack display = new ItemStack(shop.getMaterial());
        ItemMeta meta = display.getItemMeta();
        meta.displayName(ComponentUtil.smartParse("§a§lSelling: " + shop.getMaterial().name()));
        List<Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.smartParse("§7Owner: §e" + Bukkit.getOfflinePlayer(shop.getOwnerUUID()).getName()));
        lore.add(ComponentUtil.smartParse("§7Stock: §f" + stock));
        lore.add(Component.empty());
        lore.add(ComponentUtil.smartParse("§7Buy Price: §a₮ " + economyService.format(shop.getBuyPrice())));
        lore.add(ComponentUtil.smartParse("§7Sell Price: §e₮ " + economyService.format(shop.getSellPrice())));
        meta.lore(lore);
        display.setItemMeta(meta);
        inv.setItem(13, display);

        // Buy Buttons
        if (shop.getBuyPrice() > 0) {
            inv.setItem(10, createButton(Material.LIME_DYE, "§a§lBuy 1", 1, "BUY", shop.getBuyPrice(), shop));
            inv.setItem(11,
                    createButton(Material.LIME_TERRACOTTA, "§a§lBuy 64", 64, "BUY", shop.getBuyPrice() * 64, shop));
        }

        // Sell Buttons
        if (shop.getSellPrice() > 0) {
            inv.setItem(15, createButton(Material.ORANGE_DYE, "§e§lSell 1", 1, "SELL", shop.getSellPrice(), shop));
            inv.setItem(16,
                    createButton(Material.ORANGE_TERRACOTTA, "§e§lSell 64", 64, "SELL", shop.getSellPrice() * 64,
                            shop));
        }

        player.openInventory(inv);
    }

    private ItemStack createButton(Material material, String name, int amount, String action, double price,
            IslandShop shop) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.smartParse(name));
        List<Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.smartParse("§7Price: §f₮ " + economyService.format(price)));
        lore.add(Component.empty());
        lore.add(ComponentUtil.smartParse("§eClick to " + (action.equals("BUY") ? "Buy" : "Sell")));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(AMOUNT_KEY, PersistentDataType.INTEGER, amount);

        // Store location: "world,x,y,z"
        org.bukkit.Location loc = shop.getLocation();
        String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + ","
                + loc.getBlockZ();
        meta.getPersistentDataContainer().set(SHOP_LOC_KEY, PersistentDataType.STRING, locStr);

        item.setItemMeta(meta);
        return item;
    }

    private int calculateStock(IslandShop shop) {
        Block block = shop.getLocation().getBlock();
        if (!(block.getState() instanceof Container))
            return 0;
        Container container = (Container) block.getState();
        int count = 0;
        for (ItemStack item : container.getInventory().getContents()) {
            if (item != null && item.getType() == shop.getMaterial()) {
                count += item.getAmount();
            }
        }
        return count;
    }
}
