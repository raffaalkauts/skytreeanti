package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.AuctionHouseService;
import com.wiredid.skytree.api.AuctionOrder;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.SearchUtil;
import com.wiredid.skytree.util.GuiUtil;
import com.wiredid.skytree.util.NumberUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Refactored OrdersGUI using a state-based InventoryHolder architecture.
 * Provides improved responsiveness and premium visual design.
 */
public class OrdersGUI implements Listener {

    private final SkytreePlugin plugin;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey ORDER_ID_KEY;

    private final Map<UUID, SessionState> sessions = new HashMap<>();

    public enum ViewMode {
        ALL, MY_ORDERS
    }

    public enum SortMode {
        NEWEST, OLDEST, PRICE_HIGH, PRICE_LOW, DEMAND_HIGH
    }

    public enum FilterMode {
        ALL, BLOCKS, TOOLS, COMBAT, FOOD, MISC
    }

    public static class SessionState {
        public ViewMode viewMode = ViewMode.ALL;
        public SortMode sortMode = SortMode.NEWEST;
        public FilterMode filterMode = FilterMode.ALL;
        public String searchQuery = "";
        public int page = 0;
    }

    /**
     * Internal holder for the GUI state.
     */
    public static class OrdersView implements InventoryHolder {
        private final SkytreePlugin plugin;
        private final Player player;
        private final SessionState state;
        private final Inventory inventory;
        private final NamespacedKey actionKey;
        private final NamespacedKey orderIdKey;

        public OrdersView(SkytreePlugin plugin, Player player, SessionState state, String title,
                NamespacedKey actionKey,
                NamespacedKey orderIdKey) {
            this.plugin = plugin;
            this.player = player;
            this.state = state;
            this.actionKey = actionKey;
            this.orderIdKey = orderIdKey;
            this.inventory = Bukkit.createInventory(this, 54, ComponentUtil.smartParse(title));
        }

        public void render() {
            inventory.clear();
            GuiUtil.applyPremiumBorder(inventory, Material.GRAY_STAINED_GLASS_PANE,
                    Material.LIGHT_BLUE_STAINED_GLASS_PANE);

            // 1. Data Fetching
            AuctionHouseService service = plugin.getAuctionHouseService();
            List<AuctionOrder> orders = new ArrayList<>(state.viewMode == ViewMode.ALL
                    ? service.getActiveOrders()
                    : service.getPlayerOrders(player.getUniqueId()));

            // Filters
            if (!state.searchQuery.isEmpty()) {
                orders.removeIf(o -> !SearchUtil.matches(o.getRequestedItem().getType().name(), state.searchQuery));
            }
            if (state.filterMode != FilterMode.ALL) {
                orders.removeIf(o -> !matchesCategory(o.getRequestedItem().getType(), state.filterMode));
            }

            // Sorting
            switch (state.sortMode) {
                case NEWEST -> orders.sort((a, b) -> Long.compare(b.getCreatedTime(), a.getCreatedTime()));
                case OLDEST -> orders.sort(Comparator.comparingLong(AuctionOrder::getCreatedTime));
                case PRICE_HIGH -> orders.sort((a, b) -> Double.compare(b.getPricePerItem(), a.getPricePerItem()));
                case PRICE_LOW -> orders.sort(Comparator.comparingDouble(AuctionOrder::getPricePerItem));
                case DEMAND_HIGH ->
                    orders.sort((a, b) -> Integer.compare(b.getRemainingQuantity(), a.getRemainingQuantity()));
            }

            // 2. Pagination (Internal slots 10-16, 19-25, 28-34, 37-43 = 28 slots)
            int[] dataSlots = {
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34,
                    37, 38, 39, 40, 41, 42, 43
            };
            int itemsPerPage = dataSlots.length;
            int start = state.page * itemsPerPage;
            int end = Math.min(start + itemsPerPage, orders.size());

            for (int i = start; i < end; i++) {
                inventory.setItem(dataSlots[i - start], createOrderItem(orders.get(i), player, plugin, orderIdKey));
            }

            // 3. Controls (Row 6: 45-53)
            if (state.page > 0)
                inventory.setItem(45, createButton(Material.ARROW, "§e§l« Previous Page", "PREV", actionKey));

            inventory.setItem(47,
                    createButton(Material.HOPPER, "§b§lFilter » §f" + state.filterMode.name(), "FILTER", actionKey,
                            "§7Current: §e" + state.filterMode.name(), "§8Click to cycle categories"));

            inventory.setItem(48,
                    createButton(Material.BOOK, "§b§lSort » §f" + state.sortMode.name(), "SORT", actionKey,
                            "§7Current: §e" + state.sortMode.name(), "§8Click to cycle sorting"));

            inventory.setItem(49, createButton(Material.OAK_SIGN, "§b§lSearch", "SEARCH", actionKey,
                    "§7Current: §e" + (state.searchQuery.isEmpty() ? "None" : state.searchQuery),
                    "§8Left-Click to type query",
                    "§8Right-Click to RESET search"));

            Material viewMat = state.viewMode == ViewMode.ALL ? Material.ENDER_CHEST : Material.CHEST;
            String viewName = state.viewMode == ViewMode.ALL ? "§e§lView Mode » §fGlobal"
                    : "§e§lView Mode » §bPersonal";
            inventory.setItem(50, createButton(viewMat, viewName, "TOGGLE_VIEW", actionKey, "§8Click to toggle views"));

            // 51: Refresh Button
            inventory.setItem(51, createButton(Material.SUNFLOWER, "§a§lRefresh", "REFRESH", actionKey,
                    "§8Click to refresh content"));

            inventory.setItem(52, createButton(Material.EMERALD, "§a§l[NEW BOUNTY]", "CREATE", actionKey,
                    "§7Submit a purchase request"));

            if (end < orders.size())
                inventory.setItem(53, createButton(Material.ARROW, "§e§lNext Page »", "NEXT", actionKey));
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public SessionState getState() {
            return state;
        }
    }

    public OrdersGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.ACTION_KEY = new NamespacedKey(plugin, "orders_action");
        this.ORDER_ID_KEY = new NamespacedKey(plugin, "order_id");
    }

    private SessionState getSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), k -> new SessionState());
    }

    public void open(Player player) {
        SessionState state = getSession(player);
        long start = System.currentTimeMillis();

        String title = state.viewMode == ViewMode.ALL ? "§8Orders Manager (§9" + (state.page + 1) + "§8)"
                : "§8Your Active Bounties";
        OrdersView view = new OrdersView(plugin, player, state, title, ACTION_KEY, ORDER_ID_KEY);
        view.render();

        player.openInventory(view.getInventory());
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);

        long end = System.currentTimeMillis();
        if (end - start > 50) {
            plugin.getLogger().warning(
                    "Performance Warning: OrdersGUI took " + (end - start) + "ms to open for " + player.getName());
        }
    }

    /**
     * Compatibility method for search integration.
     */
    public void openWithSearch(Player player, String query) {
        SessionState state = getSession(player);
        state.searchQuery = query;
        state.page = 0;
        open(player);
    }

    /**
     * Convenience method to open specific view mode.
     */
    public void open(Player player, ViewMode mode) {
        SessionState state = getSession(player);
        state.viewMode = mode;
        state.page = 0;
        open(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof OrdersView view))
            return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        SessionState state = view.getState();
        String action = clicked.getItemMeta().getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);

        if (action != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            switch (action) {
                case "PREV" -> {
                    state.page--;
                    open(player);
                }
                case "NEXT" -> {
                    state.page++;
                    open(player);
                }
                case "FILTER" -> {
                    state.filterMode = FilterMode.values()[(state.filterMode.ordinal() + 1)
                            % FilterMode.values().length];
                    state.page = 0;
                    view.render();
                }
                case "SORT" -> {
                    state.sortMode = SortMode.values()[(state.sortMode.ordinal() + 1) % SortMode.values().length];
                    view.render();
                }
                case "SEARCH" -> {
                    if (event.isRightClick()) {
                        state.searchQuery = "";
                        state.page = 0;
                        player.sendMessage("§a§lMarket §8» §7Search filter cleared.");
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
                        view.render();
                        return;
                    }
                    player.closeInventory();
                    player.sendMessage("§6§lMarket §8» §eEnter search query in chat (or 'cancel'):");
                    plugin.getOrdersChatListener().startSearch(player);
                }
                case "TOGGLE_VIEW" -> {
                    state.viewMode = state.viewMode == ViewMode.ALL ? ViewMode.MY_ORDERS : ViewMode.ALL;
                    state.page = 0;
                    open(player); // Title change requires reopen
                }
                case "REFRESH" -> {
                    view.render();
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                }
                case "CREATE" -> {
                    player.closeInventory();
                    plugin.startBountyCreation(player);
                }
            }
            return;
        }

        String orderId = clicked.getItemMeta().getPersistentDataContainer().get(ORDER_ID_KEY,
                PersistentDataType.STRING);
        if (orderId != null) {
            AuctionOrder order = plugin.getAuctionHouseService().getOrder(UUID.fromString(orderId));
            if (order == null) {
                player.sendMessage("§cThis bounty has expired or was already fulfilled.");
                view.render();
                return;
            }

            if (order.getBuyer().equals(player.getUniqueId())) {
                if (order.getUnclaimedQuantity() > 0) {
                    plugin.getConfirmationGUI().open(player, "Claim Bounty Items?", order.getRequestedItem(),
                            confirmed -> {
                                if (confirmed) {
                                    if (plugin.getAuctionHouseService().claimOrder(player, order)) {
                                        open(player);
                                    }
                                } else {
                                    open(player);
                                }
                            });
                } else {
                    plugin.getConfirmationGUI().open(player, "Cancel Bounty?", order.getRequestedItem(), confirmed -> {
                        if (confirmed) {
                            plugin.getAuctionHouseService().cancelOrder(order);
                            player.sendMessage("§cBounty cancelled. Funds returned to balance.");
                            open(player);
                        } else {
                            open(player);
                        }
                    });
                }
            } else {
                plugin.getOrderFulfillmentGUI().open(player, order);
            }
        }
    }

    private static ItemStack createOrderItem(AuctionOrder order, Player viewer, SkytreePlugin plugin,
            NamespacedKey idKey) {
        ItemStack item = order.getRequestedItem().clone();
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();

        lore.add(ComponentUtil.parse(" "));
        lore.add(ComponentUtil.parse("§b§lBounty Details"));
        lore.add(ComponentUtil
                .parse(" §8» §7Reward: §6" + NumberUtil.formatCurrency(order.getPricePerItem()) + " §7per unit"));
        lore.add(ComponentUtil.parse(" §8» §7Quantity: §e" + order.getRemainingQuantity() + " §7requested"));

        String buyerName = Bukkit.getOfflinePlayer(order.getBuyer()).getName();
        if (buyerName == null || buyerName.isEmpty()) {
            if (plugin.getFictitiousOrderService() != null) {
                String fakeName = plugin.getFictitiousOrderService().getFakeName(order.getBuyer());
                if (fakeName != null)
                    buyerName = fakeName + " §8(NPC)";
            }
        }
        lore.add(ComponentUtil.parse(" §8» §7Requester: §f" + (buyerName != null ? buyerName : "Unknown")));

        lore.add(ComponentUtil.parse(" "));
        if (order.getBuyer().equals(viewer.getUniqueId())) {
            if (order.getUnclaimedQuantity() > 0) {
                lore.add(ComponentUtil.parse("§a§l[!] §aClick to CLAIM " + order.getUnclaimedQuantity() + " items"));
            } else {
                lore.add(ComponentUtil.parse("§e§l[!] §eClick to MANAGE/CANCEL"));
            }
        } else {
            lore.add(ComponentUtil.parse("§6§l[!] §6Click to FULFILL BOUNTY"));
        }

        meta.lore(lore);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, order.getId().toString());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createButton(Material mat, String name, String action, NamespacedKey key, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        if (lore.length > 0) {
            List<Component> l = new ArrayList<>();
            for (String s : lore)
                l.add(ComponentUtil.parse(s));
            meta.lore(l);
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private static boolean matchesCategory(Material mat, FilterMode mode) {
        String name = mat.name();
        switch (mode) {
            case BLOCKS -> {
                return mat.isBlock();
            }
            case TOOLS -> {
                return name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL")
                        || name.endsWith("_HOE");
            }
            case COMBAT -> {
                return name.endsWith("_SWORD") || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                        || name.endsWith("_SHIELD");
            }
            case FOOD -> {
                return mat.isEdible();
            }
            case MISC -> {
                return !mat.isBlock() && !mat.isEdible();
            }
            default -> {
                return true;
            }
        }
    }
}
