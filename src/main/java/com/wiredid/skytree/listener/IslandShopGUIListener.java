package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.api.IslandShopService;
import com.wiredid.skytree.model.IslandShop;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class IslandShopGUIListener implements Listener {

    private final IslandShopService islandShopService;
    private final EconomyService economyService;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey AMOUNT_KEY;
    private final NamespacedKey SHOP_LOC_KEY;

    public IslandShopGUIListener(SkytreePlugin plugin, IslandShopService islandShopService,
            EconomyService economyService) {
        this.islandShopService = islandShopService;
        this.economyService = economyService;
        this.ACTION_KEY = new NamespacedKey(plugin, "island_shop_action");
        this.AMOUNT_KEY = new NamespacedKey(plugin, "island_shop_amount");
        this.SHOP_LOC_KEY = new NamespacedKey(plugin, "island_shop_loc");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Island Shop:"))
            return;

        // Don't cancel when clicking in player's own inventory
        if (event.getClickedInventory() == player.getInventory())
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        if (!clicked.getPersistentDataContainer().has(ACTION_KEY, PersistentDataType.STRING))
            return;

        String action = clicked.getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
        int amount = clicked.getPersistentDataContainer().get(AMOUNT_KEY, PersistentDataType.INTEGER);
        String locStr = clicked.getPersistentDataContainer().get(SHOP_LOC_KEY, PersistentDataType.STRING);

        if (locStr == null)
            return;

        // Parse location: "world,x,y,z"
        String[] parts = locStr.split(",");
        if (parts.length != 4)
            return;

        org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
        if (world == null)
            return;

        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        org.bukkit.Location loc = new org.bukkit.Location(world, x, y, z);

        Optional<IslandShop> shopOpt = islandShopService.getShop(loc.getBlock());
        if (shopOpt.isEmpty()) {
            player.sendMessage("§cError: Could not find shop data.");
            player.closeInventory();
            return;
        }

        IslandShop shop = shopOpt.get();
        if (action.equalsIgnoreCase("BUY")) {
            handleBuy(player, shop, amount);
        } else if (action.equalsIgnoreCase("SELL")) {
            handleSell(player, shop, amount);
        }
    }

    private void handleBuy(Player player, IslandShop shop, int amount) {
        double totalPrice = shop.getBuyPrice() * amount;
        if (economyService.getBalance(player.getUniqueId()) < totalPrice) {
            player.sendMessage("§cYou don't have enough money!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Check stock in chest
        Block block = shop.getLocation().getBlock();
        if (!(block.getState() instanceof Container container))
            return;
        Inventory shopInv = container.getInventory();

        if (!hasItems(shopInv, shop.getMaterial(), amount)) {
            player.sendMessage("§cThis shop is out of stock!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Check player inventory space
        if (player.getInventory().firstEmpty() == -1 && !canStack(player.getInventory(), shop.getMaterial(), amount)) {
            player.sendMessage("§cYour inventory is full!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // All good, process transaction
        if (economyService.removeBalance(player.getUniqueId(), totalPrice)) {
            economyService.addBalance(shop.getOwnerUUID(), totalPrice);
            removeItems(shopInv, shop.getMaterial(), amount);
            player.getInventory().addItem(new ItemStack(shop.getMaterial(), amount));

            player.sendMessage("§aYou bought " + amount + "x " + shop.getMaterial().name() + " for ₮ "
                    + economyService.format(totalPrice));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);

            // Notify shop owner
            org.bukkit.entity.Player owner = org.bukkit.Bukkit.getPlayer(shop.getOwnerUUID());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage("§a§l[Shop] §f" + player.getName() + " §abought §f" + amount + "x "
                        + shop.getMaterial().name() + " §afor ₮" + economyService.format(totalPrice));
                owner.playSound(owner.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.5f);
            }
        }
    }

    private void handleSell(Player player, IslandShop shop, int amount) {
        double totalGain = shop.getSellPrice() * amount;

        // Check owner has enough money
        if (economyService.getBalance(shop.getOwnerUUID()) < totalGain) {
            player.sendMessage("§cThe shop owner cannot afford this!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Check player has items
        if (!hasItems(player.getInventory(), shop.getMaterial(), amount)) {
            player.sendMessage("§cYou don't have enough items to sell!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Check chest space
        Block block = shop.getLocation().getBlock();
        if (!(block.getState() instanceof Container container))
            return;
        Inventory shopInv = container.getInventory();
        if (shopInv.firstEmpty() == -1 && !canStack(shopInv, shop.getMaterial(), amount)) {
            player.sendMessage("§cThe shop chest is full!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Process transaction
        if (economyService.removeBalance(shop.getOwnerUUID(), totalGain)) {
            economyService.addBalance(player.getUniqueId(), totalGain);
            removeItems(player.getInventory(), shop.getMaterial(), amount);
            shopInv.addItem(new ItemStack(shop.getMaterial(), amount));

            player.sendMessage("§eYou sold " + amount + "x " + shop.getMaterial().name() + " for ₮ "
                    + economyService.format(totalGain));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);

            // Notify shop owner
            org.bukkit.entity.Player owner = org.bukkit.Bukkit.getPlayer(shop.getOwnerUUID());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage("§e§l[Shop] §f" + player.getName() + " §esold §f" + amount + "x "
                        + shop.getMaterial().name() + " §efor ₮" + economyService.format(totalGain));
                owner.playSound(owner.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.5f);
            }
        }
    }

    private boolean hasItems(Inventory inv, Material mat, int amount) {
        int count = 0;
        for (ItemStack is : inv.getContents()) {
            if (is != null && is.getType() == mat) {
                count += is.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeItems(Inventory inv, Material mat, int amount) {
        int remaining = amount;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is != null && is.getType() == mat) {
                if (is.getAmount() <= remaining) {
                    remaining -= is.getAmount();
                    inv.setItem(i, null);
                } else {
                    is.setAmount(is.getAmount() - remaining);
                    remaining = 0;
                }
            }
            if (remaining == 0)
                break;
        }
    }

    private boolean canStack(Inventory inv, Material mat, int amount) {
        int space = 0;
        for (ItemStack is : inv.getStorageContents()) {
            if (is == null || is.getType() == Material.AIR) {
                space += mat.getMaxStackSize();
            } else if (is.getType() == mat) {
                space += (mat.getMaxStackSize() - is.getAmount());
            }
        }
        return space >= amount;
    }
}
