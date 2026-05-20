package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.AuctionHouse;
import com.wiredid.skytree.api.AuctionHouseService;
import com.wiredid.skytree.api.AuctionOrder;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.SearchUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;

public class AuctionHouseGUI implements Listener {

    private final SkytreePlugin plugin;
    private final AuctionHouseService service;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey ID_KEY;
    private final NamespacedKey TYPE_KEY; // "LISTING" or "ORDER"

    // Sort Options
    public enum SortType {
        NEWEST, OLDEST, LOWEST_PRICE, HIGHEST_PRICE
    }

    // View Type
    public enum ViewType {
        LISTINGS, ORDERS, MY_LISTINGS, MY_ORDERS, COLLECTION
    }

    public AuctionHouseGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.service = plugin.getAuctionHouseService();
        this.ACTION_KEY = new NamespacedKey(plugin, "ah_action");
        this.ID_KEY = new NamespacedKey(plugin, "ah_id");
        this.TYPE_KEY = new NamespacedKey(plugin, "ah_type");
    }

    public void open(Player player, ViewType view, int page, SortType sort) {
        open(player, view, page, sort, null);
    }

    public void open(Player player, ViewType view, int page, SortType sort, String search) {
        String title;
        List<?> itemsRaw;

        switch (view) {
            case ORDERS:
                title = "§6§lMarket §8» §7Orders §8(" + (page + 1) + ")";
                itemsRaw = getSortedOrders(service.getActiveOrders(), sort);
                break;
            case MY_LISTINGS:
                title = "§6§lMarket §8» §dYour Listings §8(" + (page + 1) + ")";
                itemsRaw = service.getPlayerListings(player.getUniqueId());
                break;
            case MY_ORDERS:
                title = "§6§lMarket §8» §bYour Orders §8(" + (page + 1) + ")";
                itemsRaw = service.getPlayerOrders(player.getUniqueId());
                break;
            case COLLECTION:
                title = "§6§lMarket §8» §aCollection Bin §8(" + (page + 1) + ")";
                itemsRaw = service.getExpiredListings(player.getUniqueId());
                break;
            default: // LISTINGS
                title = "§6§lMarket §8» §eListings §8(" + (page + 1) + ")";
                itemsRaw = getSortedListings(service.getActiveListings(), sort);
                break;
        }

        // Apply Filter
        List<Object> items = new ArrayList<>();
        if (search != null && !search.isEmpty()) {
            for (Object obj : itemsRaw) {
                String name = "";
                if (obj instanceof AuctionHouse) {
                    name = ComponentUtil.toLegacy(((AuctionHouse) obj).getItem().displayName());
                } else if (obj instanceof AuctionOrder) {
                    name = ComponentUtil.toLegacy(((AuctionOrder) obj).getRequestedItem().displayName());
                }

                if (SearchUtil.matches(name, search)) {
                    items.add(obj);
                }
            }
        } else {
            items.addAll((Collection<? extends Object>) itemsRaw);
        }

        if (search != null && !search.isEmpty()) {
            title += " §8[§6" + search + "§8]";
        }

        Inventory inv = Bukkit.createInventory(null, 54, ComponentUtil.smartParse(title));

        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(inv, Material.BLACK_STAINED_GLASS_PANE,
                Material.ORANGE_STAINED_GLASS_PANE);

        // Populate Items (Interior rows 2, 3, 4: 19-25, 28-34, 37-43)
        int[] availableSlots = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43 };
        int itemsPerPage = availableSlots.length; // 21
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());

        for (int i = start; i < end; i++) {
            Object obj = items.get(i);
            int slot = availableSlots[i - start];
            if (obj instanceof AuctionHouse) {
                inv.setItem(slot, createListingItem((AuctionHouse) obj, player, view == ViewType.COLLECTION));
            } else if (obj instanceof AuctionOrder) {
                inv.setItem(slot, createOrderItem((AuctionOrder) obj, player));
            }
        }

        // Control Bar (Row 1 Interior: 10-16)
        setupControlBar(inv, player, view, page, sort, items.size() > end, search);

        player.openInventory(inv);
    }

    private void setupControlBar(Inventory inv, Player player, ViewType view, int page, SortType sort,
            boolean hasNext, String search) {
        // 10: Previous
        if (page > 0) {
            inv.setItem(10, createButton(Material.ARROW, "§ePrevious Page", "PREV", view, page, sort));
        }

        // 11: Search & Sort Row
        inv.setItem(11, createButton(Material.OAK_SIGN, "§eSearch: §6" + (search == null ? "None" : search), "SEARCH",
                view, page, sort));
        inv.setItem(12, createButton(Material.COMPARATOR, "§eSort: §f" + formatSort(sort), "SORT", view, page, sort));

        // 13: Main Views Multi-toggle (Simplified for interior row)
        Material viewMat = switch (view) {
            case LISTINGS -> Material.CHEST;
            case MY_LISTINGS -> Material.PLAYER_HEAD;
            case ORDERS -> Material.WRITABLE_BOOK;
            case COLLECTION -> Material.HOPPER;
            default -> Material.CHEST; // Fallback
        };
        inv.setItem(13, createButton(viewMat, "§6§lSwitch View §7(Click)", "SWITCH_VIEW", view, page, sort));

        // 14: Collection Bin
        inv.setItem(14, createButton(Material.HOPPER, "§aCollection Bin", "VIEW_COLLECTION", view, page, sort));

        // 15: Conditional Action
        if (view == ViewType.ORDERS) {
            inv.setItem(15, createButton(Material.EMERALD, "§a§l+ Create Order", "CREATE_ORDER", view, page, sort));
        } else {
            inv.setItem(15,
                    createButton(Material.PLAYER_HEAD, "§dYour Listings", "VIEW_MY_LISTINGS", view, page, sort));
        }

        // 16: Next
        if (hasNext) {
            inv.setItem(16, createButton(Material.ARROW, "§eNext Page", "NEXT", view, page, sort));
        }

        // 17: Refresh
        inv.setItem(17, createButton(Material.SUNFLOWER, "§a§lRefresh", "REFRESH", view, page, sort));

        // Backfill search queries to all buttons in row 1
        for (int i = 10; i < 18; i++) {
            ItemStack btn = inv.getItem(i);
            if (btn != null && btn.hasItemMeta() && search != null) {
                ItemMeta meta = btn.getItemMeta();
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "search_query"),
                        PersistentDataType.STRING, search);
                btn.setItemMeta(meta);
            }
        }
    }

    private ItemStack createListingItem(AuctionHouse listing, Player viewer, boolean isCollection) {
        ItemStack item = listing.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();

        lore.add(ComponentUtil.parse(" "));
        lore.add(ComponentUtil.parse("§8§m----------------"));
        lore.add(ComponentUtil.parse("§7Seller: §f" + resolveName(listing.getSeller())));
        lore.add(ComponentUtil
                .parse("§7Price: §e" + NumberUtil.formatCurrency(listing.getPricePerItem()) + " §7each"));
        lore.add(ComponentUtil.parse("§7Stock: §a" + listing.getQuantityRemaining()));
        lore.add(ComponentUtil.parse(" "));

        if (isCollection) {
            lore.add(ComponentUtil.parse("§a§lClick to CLAIM items"));
        } else if (listing.getSeller().equals(viewer.getUniqueId())) {
            lore.add(ComponentUtil.parse("§cClick to CANCEL listing"));
        } else {
            if (listing.getPricePerItem() * listing.getQuantityRemaining() <= plugin.getEconomyService()
                    .getBalance(viewer.getUniqueId())) {
                lore.add(ComponentUtil.parse("§eClick to BUY"));
                lore.add(ComponentUtil.parse("§7Shift-Click to Buy All"));
            } else {
                lore.add(ComponentUtil.parse("§cInsufficient Funds"));
            }
        }
        lore.add(ComponentUtil.parse("§8§m----------------"));
        meta.lore(lore);

        // Clean PDC
        // We use PDC to store Listing ID
        meta.getPersistentDataContainer().set(ID_KEY, PersistentDataType.STRING, listing.getId().toString());
        meta.getPersistentDataContainer().set(TYPE_KEY, PersistentDataType.STRING, "LISTING");

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOrderItem(AuctionOrder order, Player viewer) {
        ItemStack item = order.getRequestedItem();
        ItemMeta meta = item.getItemMeta();
        List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();

        lore.add(ComponentUtil.parse(" "));
        lore.add(ComponentUtil.parse("§8§m----------------"));
        lore.add(ComponentUtil.parse("§7Buyer: §f" + resolveName(order.getBuyer())));
        lore.add(ComponentUtil
                .parse("§7Offering: §a" + NumberUtil.formatCurrency(order.getPricePerItem()) + " §7each"));
        lore.add(ComponentUtil.parse("§7Progress: §e" + order.getFilledQuantity() + "/" + order.getTotalQuantity()));
        lore.add(ComponentUtil.parse(" "));

        if (order.getBuyer().equals(viewer.getUniqueId())) {
            if (order.getUnclaimedQuantity() > 0) {
                lore.add(ComponentUtil.parse("§a§lClick to CLAIM " + order.getUnclaimedQuantity() + " items"));
            } else {
                lore.add(ComponentUtil.parse("§cClick to CANCEL order"));
            }
        } else {
            lore.add(ComponentUtil.parse("§eClick to FULFILL order"));
        }
        lore.add(ComponentUtil.parse("§8§m----------------"));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(ID_KEY, PersistentDataType.STRING, order.getId().toString());
        meta.getPersistentDataContainer().set(TYPE_KEY, PersistentDataType.STRING, "ORDER");

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButton(Material mat, String name, String action, ViewType view, int page, SortType sort) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        // Store State
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "view"), PersistentDataType.STRING,
                view.name());
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "page"), PersistentDataType.INTEGER, page);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "sort"), PersistentDataType.STRING,
                sort.name());
        item.setItemMeta(meta);
        return item;
    }

    private String resolveName(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    private String formatSort(SortType sort) {
        switch (sort) {
            case LOWEST_PRICE:
                return "Lowest Price";
            case HIGHEST_PRICE:
                return "Highest Price";
            case NEWEST:
                return "Newest";
            case OLDEST:
                return "Oldest";
            default:
                return sort.name();
        }
    }

    private List<AuctionHouse> getSortedListings(List<AuctionHouse> list, SortType sort) {
        // Clone to sort
        List<AuctionHouse> sorted = new ArrayList<>(list);
        switch (sort) {
            case LOWEST_PRICE:
                sorted.sort(Comparator.comparingDouble(AuctionHouse::getPricePerItem));
                break;
            case HIGHEST_PRICE:
                sorted.sort((a, b) -> Double.compare(b.getPricePerItem(), a.getPricePerItem()));
                break;
            case NEWEST:
                sorted.sort((a, b) -> Long.compare(b.getListedTime(), a.getListedTime()));
                break;
            case OLDEST:
                sorted.sort(Comparator.comparingLong(AuctionHouse::getListedTime));
                break;
        }
        return sorted;
    }

    private List<AuctionOrder> getSortedOrders(List<AuctionOrder> list, SortType sort) {
        List<AuctionOrder> sorted = new ArrayList<>(list);
        // Orders sorting same as listings for now
        switch (sort) {
            case LOWEST_PRICE:
                sorted.sort(Comparator.comparingDouble(AuctionOrder::getPricePerItem));
                break;
            case HIGHEST_PRICE:
                sorted.sort((a, b) -> Double.compare(b.getPricePerItem(), a.getPricePerItem()));
                break;
            case NEWEST:
                sorted.sort((a, b) -> Long.compare(b.getCreatedTime(), a.getCreatedTime()));
                break;
            case OLDEST:
                sorted.sort(Comparator.comparingLong(AuctionOrder::getCreatedTime));
                break;
        }
        return sorted;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.stripColor(event.getView().title());
        if (!title.contains("Market"))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta())
            return;

        // Handle Control Buttons
        String action = clicked.getItemMeta().getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
        if (action != null) {
            ViewType currentView = ViewType.valueOf(clicked.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "view"), PersistentDataType.STRING));
            int currentPage = clicked.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "page"),
                    PersistentDataType.INTEGER);
            SortType currentSort = SortType.valueOf(clicked.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "sort"), PersistentDataType.STRING));
            String currentSearch = clicked.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "search_query"), PersistentDataType.STRING);

            switch (action) {
                case "SWITCH_VIEW":
                    int nextViewOrdinal = (currentView.ordinal() + 1) % ViewType.values().length;
                    open(player, ViewType.values()[nextViewOrdinal], 0, currentSort, currentSearch);
                    break;
                case "REFRESH":
                    open(player, currentView, currentPage, currentSort, currentSearch);
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
                    break;
                case "NEXT":
                    open(player, currentView, currentPage + 1, currentSort, currentSearch);
                    break;
                case "PREV":
                    open(player, currentView, currentPage - 1, currentSort, currentSearch);
                    break;
                case "VIEW_LISTINGS":
                    open(player, ViewType.LISTINGS, 0, currentSort, currentSearch);
                    break;
                case "VIEW_MY_LISTINGS":
                    open(player, ViewType.MY_LISTINGS, 0, currentSort, currentSearch);
                    break;
                case "VIEW_MY_ORDERS":
                    open(player, ViewType.MY_ORDERS, 0, currentSort, currentSearch);
                    break;
                case "VIEW_COLLECTION":
                    open(player, ViewType.COLLECTION, 0, currentSort, currentSearch);
                    break;
                case "SORT":
                    // Cycle Sort
                    int ordinal = (currentSort.ordinal() + 1) % SortType.values().length;
                    open(player, currentView, 0, SortType.values()[ordinal], currentSearch);
                    break;
                case "SEARCH":
                    player.closeInventory();
                    player.sendMessage("§6§lMarket §8» §eEnter search query in chat (or 'cancel'):");
                    player.setMetadata("ah_search_context", new org.bukkit.metadata.FixedMetadataValue(plugin,
                            currentView.name() + ":" + currentSort.name()));
                    break;
                case "CLEAR_SEARCH":
                    open(player, currentView, 0, currentSort, null);
                    break;
                case "CREATE_ORDER":
                    // Open Material Selector
                    new MaterialSelectorGUI(plugin).openCategorySelection(player);
                    break;
            }
            return;
        }

        // Handle Item Click (Listing or Order)
        String idStr = clicked.getItemMeta().getPersistentDataContainer().get(ID_KEY, PersistentDataType.STRING);
        String type = clicked.getItemMeta().getPersistentDataContainer().get(TYPE_KEY, PersistentDataType.STRING);

        if (idStr != null && type != null) {
            UUID id = UUID.fromString(idStr);
            if (type.equals("LISTING")) {
                AuctionHouse listing = service.getListing(id);
                if (listing == null) {
                    player.sendMessage("§cItem no longer available.");
                    open(player, ViewType.LISTINGS, 0, SortType.NEWEST, null);
                    return;
                }

                if (title.contains("Collection Bin") || title.contains("Collection")) {
                    service.claimListing(player, listing);
                    open(player, ViewType.COLLECTION, 0, SortType.NEWEST, null);
                } else if (listing.getSeller().equals(player.getUniqueId())) {
                    // Cancel
                    service.cancelListing(listing);
                    player.sendMessage("§6§lMarket §8» §eListing cancelled! Claim your items in the Collection Bin.");
                    open(player, ViewType.MY_LISTINGS, 0, SortType.NEWEST, null); // Refresh
                } else {
                    // Partial Buy GUI
                    plugin.getAuctionPurchaseGUI().open(player, listing, 1);
                }
            } else if (type.equals("ORDER")) {
                AuctionOrder order = service.getOrder(id);
                if (order == null)
                    return;

                if (order.getBuyer().equals(player.getUniqueId())) {
                    // Check Claim or Cancel
                    if (order.getUnclaimedQuantity() > 0) {
                        // Claim logic (Give items)
                        ItemStack requested = order.getRequestedItem();
                        requested.setAmount(order.getUnclaimedQuantity());
                        // Add to inventory check full
                        // Simplify: Give all unclaimed.
                        Map<Integer, ItemStack> left = player.getInventory().addItem(requested);
                        int given = order.getUnclaimedQuantity() - (left.isEmpty() ? 0 : left.get(0).getAmount());
                        order.addClaimed(given);
                        player.sendMessage("§aClaimed " + given + " items.");
                        // Save order state? Service doesn't explicitly expose saveSingleOrder but
                        // fulfill does save.
                        // I need a service method updateOrder? Or cast to Impl.
                        // For now let's hope persist works or I add updateOrder.
                        // Re-open to refresh view.
                        open(player, ViewType.MY_ORDERS, 0, SortType.NEWEST, null);
                    } else {
                        service.cancelOrder(order);
                        player.sendMessage("§cOrder cancelled and funds refunded.");
                        open(player, ViewType.MY_ORDERS, 0, SortType.NEWEST, null);
                    }
                } else {
                    // Fulfill
                    plugin.getOrderFulfillmentGUI().open(player, order);
                }
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!player.hasMetadata("ah_search_context"))
            return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        String context = player.getMetadata("ah_search_context").get(0).asString();
        player.removeMetadata("ah_search_context", plugin);

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(ComponentUtil.parse("§cSearch cancelled."));
            return;
        }

        String[] parts = context.split(":");
        ViewType view = ViewType.valueOf(parts[0]);
        SortType sort = SortType.valueOf(parts[1]);

        Bukkit.getScheduler().runTask(plugin, () -> {
            open(player, view, 0, sort, message);
        });
    }
}
