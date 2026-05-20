package com.wiredid.skytree.fishing.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.fishing.FishStorage;
import com.wiredid.skytree.fishing.NbtUtils;
import com.wiredid.skytree.util.ComponentUtil;
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

import net.kyori.adventure.text.Component;
import java.util.ArrayList;
import java.util.List;

public class FishStorageGUI implements Listener {

    private final FishStorage storage;
    private final com.wiredid.skytree.api.EconomyService economy;

    private static final String TITLE = "§8Fish Storage";

    public FishStorageGUI(SkytreePlugin plugin, FishStorage storage, com.wiredid.skytree.api.EconomyService economy) {
        // Plugin used to be stored effectively ignored. Use directly if needed or
        // remove from constructor if unused.
        // Keeping constructor signature to avoid breaking callers, but not storing
        // plugin.
        this.storage = storage;
        this.economy = economy;
    }

    private enum GUIMode {
        NORMAL("&bNormal Mode", Material.FISHING_ROD, "&7Left: Retrieve", "&7Right: Sell", "&7Shift: Toggle Fav"),
        FAVORITE("&eFavorite Mode", Material.NETHER_STAR, "&7Click items to toggle", "&7their favorite status."),
        SELL("&cSell Mode", Material.GOLD_INGOT, "&7Click items to sell", "&7them immediately.");

        private final String name;
        private final Material icon;
        private final String[] description;

        GUIMode(String name, Material icon, String... description) {
            this.name = name;
            this.icon = icon;
            this.description = description;
        }
    }

    private final java.util.Map<java.util.UUID, GUIMode> playerModes = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Integer> playerSorts = new java.util.HashMap<>(); // 0: Rarity, 1:
                                                                                                  // Weight, 2: Fav

    public void open(Player player, int page) {
        List<ItemStack> fish = storage.getFish(player.getUniqueId());
        GUIMode mode = playerModes.getOrDefault(player.getUniqueId(), GUIMode.NORMAL);
        int sortType = playerSorts.getOrDefault(player.getUniqueId(), 0);

        Inventory inv = Bukkit.createInventory(null, 54,
                ComponentUtil.smartParse(TITLE + " &7(Page " + (page + 1) + ")"));

        // Pagination
        int slots = 45;
        int startIndex = page * slots;
        int endIndex = Math.min(startIndex + slots, fish.size());

        for (int i = startIndex; i < endIndex; i++) {
            ItemStack item = fish.get(i).clone();
            boolean isFav = NbtUtils.getBoolean(item, "IsFavorite");
            ItemMeta meta = item.getItemMeta();
            List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            if (lore == null)
                lore = new ArrayList<>();

            if (isFav) {
                lore.add(ComponentUtil.smartParse("&e⭐ FAVORITE"));
            }
            lore.add(Component.empty());

            for (String desc : mode.description) {
                lore.add(ComponentUtil.smartParse(desc));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(i - startIndex, item);
        }

        // Control Row
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Component.empty());
        filler.setItemMeta(fm);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, filler);

        // Navigation
        if (page > 0)
            inv.setItem(45, createButton(Material.ARROW, "&ePrevious Page"));
        if (endIndex < fish.size())
            inv.setItem(53, createButton(Material.ARROW, "&eNext Page"));

        // Mode Toggles
        inv.setItem(46,
                createButton(GUIMode.NORMAL.icon, GUIMode.NORMAL.name + (mode == GUIMode.NORMAL ? " &a[ACTIVE]" : ""),
                        "&7Switch to standard interaction"));
        inv.setItem(47,
                createButton(GUIMode.FAVORITE.icon,
                        GUIMode.FAVORITE.name + (mode == GUIMode.FAVORITE ? " &a[ACTIVE]" : ""),
                        "&7Click fish to favorite/unfavorite"));
        inv.setItem(48, createButton(GUIMode.SELL.icon, GUIMode.SELL.name + (mode == GUIMode.SELL ? " &a[ACTIVE]" : ""),
                "&7Click fish to sell instantly"));

        // Sort Cycle
        String sortName = switch (sortType) {
            case 0 -> "Rarity";
            case 1 -> "Weight";
            default -> "Favorite";
        };
        inv.setItem(49, createButton(Material.HOPPER, "&6Sort: &f" + sortName, "&7Click to cycle sorting methods"));

        // Mass Actions
        inv.setItem(50, createButton(Material.GOLD_BLOCK, "&c&lSELL ALL", "&7Sells all &non-favorite&7 fish."));

        player.openInventory(inv);
    }

    private ItemStack createButton(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.smartParse(name));
        List<Component> loreComps = new ArrayList<>();
        for (String line : lore)
            loreComps.add(ComponentUtil.smartParse(line));
        meta.lore(loreComps);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Fish Storage"))
            return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        int slot = event.getSlot();
        int page = 0;
        try {
            String p = title.split("Page ")[1].replace(")", "");
            page = Integer.parseInt(p) - 1;
        } catch (Exception ignored) {
        }

        GUIMode mode = playerModes.getOrDefault(player.getUniqueId(), GUIMode.NORMAL);

        if (slot < 45) {
            List<ItemStack> fishList = storage.getFish(player.getUniqueId());
            int index = (page * 45) + slot;
            if (index >= fishList.size())
                return;

            ItemStack fish = fishList.get(index);

            if (mode == GUIMode.FAVORITE) {
                toggleFavorite(fish);
                open(player, page);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
            } else if (mode == GUIMode.SELL) {
                sellFish(player, fish);
                open(player, page);
            } else {
                // Normal Mode
                if (event.isShiftClick()) {
                    toggleFavorite(fish);
                    open(player, page);
                } else if (event.isLeftClick()) {
                    retrieveFish(player, fish);
                    open(player, page);
                } else if (event.isRightClick()) {
                    sellFish(player, fish);
                    open(player, page);
                }
            }
        } else {
            // Controls
            if (slot == 45 && clicked.getType() == Material.ARROW)
                open(player, page - 1);
            else if (slot == 53 && clicked.getType() == Material.ARROW)
                open(player, page + 1);
            else if (slot == 46) {
                playerModes.put(player.getUniqueId(), GUIMode.NORMAL);
                open(player, page);
            } else if (slot == 47) {
                playerModes.put(player.getUniqueId(), GUIMode.FAVORITE);
                open(player, page);
            } else if (slot == 48) {
                playerModes.put(player.getUniqueId(), GUIMode.SELL);
                open(player, page);
            } else if (slot == 49) {
                int nextSort = (playerSorts.getOrDefault(player.getUniqueId(), 0) + 1) % 3;
                playerSorts.put(player.getUniqueId(), nextSort);
                storage.sort(player.getUniqueId(), switch (nextSort) {
                    case 0 -> FishStorage.SortType.RARITY;
                    case 1 -> FishStorage.SortType.WEIGHT;
                    default -> FishStorage.SortType.FAVORITE;
                });
                open(player, page);
            } else if (slot == 50) {
                sellAllNonFavorites(player);
                open(player, page);
            }
        }
    }

    private void toggleFavorite(ItemStack fish) {
        boolean current = NbtUtils.getBoolean(fish, "IsFavorite");
        NbtUtils.setBoolean(fish, "IsFavorite", !current);
        storage.save();
    }

    private void retrieveFish(Player player, ItemStack fish) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(fish);
            storage.removeFish(player.getUniqueId(), fish);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        } else {
            player.sendMessage("§cInventory full!");
        }
    }

    private void sellFish(Player player, ItemStack fish) {
        double price = NbtUtils.getDouble(fish, NbtUtils.KEY_FISH_PRICE);
        if (price > 0) {
            economy.addBalance(player.getUniqueId(), price);
            storage.removeFish(player.getUniqueId(), fish);
            player.sendMessage("§aSold for " + String.format("%.2f", price) + " USDT");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        } else {
            player.sendMessage("§cCannot sell this item.");
        }
    }

    private void sellAllNonFavorites(Player player) {
        List<ItemStack> list = storage.getFish(player.getUniqueId());
        double total = 0;
        int count = 0;

        java.util.Iterator<ItemStack> it = list.iterator();
        while (it.hasNext()) {
            ItemStack f = it.next();
            if (NbtUtils.getBoolean(f, "IsFavorite"))
                continue;
            double price = NbtUtils.getDouble(f, NbtUtils.KEY_FISH_PRICE);
            if (price > 0) {
                total += price;
                count++;
                it.remove();
            }
        }

        if (count > 0) {
            storage.save();
            economy.addBalance(player.getUniqueId(), total);
            player.sendMessage("§a§l[Shop] §aSold " + count + " fish for §e" + String.format("%.2f", total) + " USDT");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        } else {
            player.sendMessage("§cNo sellable non-favorite fish found.");
        }
    }
}
