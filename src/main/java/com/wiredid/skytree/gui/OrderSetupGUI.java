package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OrderSetupGUI implements Listener {

    private final SkytreePlugin plugin;
    private static final Map<UUID, OrderSession> sessions = new HashMap<>();

    public OrderSetupGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String itemId) {
        sessions.put(player.getUniqueId(), new OrderSession(itemId));
        openGUI(player);
    }

    private void openGUI(Player player) {
        OrderSession session = sessions.get(player.getUniqueId());
        if (session == null)
            return;

        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.smartParse("§b§lBounty §8» §fPost Order"));

        // Display Item
        ItemStack displayItem;
        if (plugin.getItemRegistry().getItem(session.itemId) != null) {
            displayItem = plugin.getItemRegistry().getItem(session.itemId).clone();
        } else {
            Material mat = Material.matchMaterial(session.itemId);
            displayItem = new ItemStack(mat != null ? mat : Material.BARRIER);
        }
        gui.setItem(4, displayItem);

        // Quantity Button
        ItemStack quantityBtn = new ItemStack(Material.PAPER);
        ItemMeta qMeta = quantityBtn.getItemMeta();
        qMeta.displayName(ComponentUtil.parse("§b§lSet Quantity"));
        qMeta.lore(Arrays.asList(
                ComponentUtil.parse("§7Current: §f" + session.quantity),
                ComponentUtil.parse(""),
                ComponentUtil.parse("§e▶ Click to type in chat")));
        quantityBtn.setItemMeta(qMeta);
        gui.setItem(11, quantityBtn);

        // Price Button
        ItemStack priceBtn = new ItemStack(Material.SUNFLOWER);
        ItemMeta pMeta = priceBtn.getItemMeta();
        pMeta.displayName(ComponentUtil.parse("§b§lSet Price Per Item"));
        pMeta.lore(Arrays.asList(
                ComponentUtil.parse("§7Current: §a" + NumberUtil.formatCurrency(session.price)),
                ComponentUtil.parse(""),
                ComponentUtil.parse("§e▶ Click to type in chat")));
        priceBtn.setItemMeta(pMeta);
        gui.setItem(15, priceBtn);

        // Strict Match Toggle
        ItemStack strictBtn = new ItemStack(session.strictMatch ? Material.ENCHANTED_BOOK : Material.BOOK);
        ItemMeta sMeta = strictBtn.getItemMeta();
        sMeta.displayName(ComponentUtil.parse("§b§lMatch Enchantments"));
        sMeta.lore(Arrays.asList(
                ComponentUtil.parse("§7Status: " + (session.strictMatch ? "§aSTRICT" : "§7ANY")),
                ComponentUtil.parse(""),
                ComponentUtil.parse("§7If §aSTRICT§7, the delivered item must"),
                ComponentUtil.parse("§7have the exact same enchantments."),
                ComponentUtil.parse(""),
                ComponentUtil.parse("§e▶ Click to toggle")));
        strictBtn.setItemMeta(sMeta);
        gui.setItem(13, strictBtn);

        // Confirm Button
        boolean canAfford = plugin.getEconomyService()
                .getBalance(player.getUniqueId()) >= (session.price * session.quantity);
        ItemStack confirmBtn = new ItemStack(canAfford ? Material.LIME_CONCRETE : Material.RED_CONCRETE);
        ItemMeta cMeta = confirmBtn.getItemMeta();
        cMeta.displayName(ComponentUtil.parse(canAfford ? "§a§l[CONFIRM BOUNTY]" : "§c§l[INSUFFICIENT FUNDS]"));
        cMeta.lore(Arrays.asList(
                ComponentUtil.parse("§7Total Cost: §a" + NumberUtil.formatCurrency(session.price * session.quantity)),
                ComponentUtil.parse(""),
                ComponentUtil.parse(canAfford ? "§eClick to create your bounty" : "§cYou need more funds")));
        confirmBtn.setItemMeta(cMeta);
        gui.setItem(22, confirmBtn);

        GuiUtil.applyPremiumBorder(gui, Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE);
        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        String title = ComponentUtil.toLegacy(event.getView().title());

        if (!title.contains("Bounty") || !title.contains("Post Order"))
            return;
        event.setCancelled(true);

        OrderSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null)
            return;

        if (clicked.getType() == Material.PAPER) {
            session.awaitingInput = "QUANTITY";
            player.closeInventory();
            player.sendMessage("§aType the quantity in chat:");
        } else if (clicked.getType() == Material.SUNFLOWER) {
            session.awaitingInput = "PRICE";
            player.closeInventory();
            player.sendMessage("§aType the price per item in chat:");
        } else if (clicked.getType() == Material.BOOK || clicked.getType() == Material.ENCHANTED_BOOK) {
            session.strictMatch = !session.strictMatch;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            openGUI(player);
        } else if (clicked.getType() == Material.LIME_CONCRETE) {

            // Create Order
            // Determine item stack to order
            ItemStack itemToOrder;
            if (plugin.getItemRegistry().getItem(session.itemId) != null) {
                itemToOrder = plugin.getItemRegistry().getItem(session.itemId).clone();
            } else {
                Material mat = Material.matchMaterial(session.itemId);
                itemToOrder = new ItemStack(mat != null ? mat : Material.STONE);
            }
            itemToOrder.setAmount(session.quantity); // Just for display/logic, createOrder fixes amount to 1 internally
                                                     // for request type

            // Default duration from config
            long durationHours = plugin.getConfig().getLong("auction.listing_expiry_hours", 48);
            long duration = durationHours * 3600 * 1000L;

            if (plugin.getAuctionHouseService().createOrder(player, itemToOrder, session.price, session.quantity,
                    duration, session.strictMatch) != null) {
                player.sendMessage("§aOrder created successfully for " + session.quantity + "x " + session.itemId);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                sessions.remove(player.getUniqueId());
                player.closeInventory();
            } else {
                // Error handled in service usually, but fallback here
                player.sendMessage("§cFailed to create order. Check funds or invalid item.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        } else if (clicked.getType() == Material.RED_CONCRETE) {
            player.sendMessage("§c§lInsufficient Funds! §7You need more money to create this order.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        } else if (event.getSlot() == 4) {
            player.sendMessage("§eTarget Item: §f" + session.itemId);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!sessions.containsKey(player.getUniqueId()))
            return;

        OrderSession session = sessions.get(player.getUniqueId());
        if (session.awaitingInput == null)
            return;

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message());

        try {
            if (session.awaitingInput.equals("QUANTITY")) {
                int qty = (int) NumberUtil.parseSmartNumber(input.trim());
                if (qty <= 0)
                    throw new NumberFormatException();
                session.quantity = qty;
            } else if (session.awaitingInput.equals("PRICE")) {
                double price = NumberUtil.parseSmartNumber(input.trim());
                if (price <= 0)
                    throw new NumberFormatException();
                session.price = price;
            }
            session.awaitingInput = null;

            // Re-open GUI on main thread
            Bukkit.getScheduler().runTask(plugin, () -> openGUI(player));

        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number. Please try again.");
        }
    }

    private static class OrderSession {
        String itemId;
        int quantity = 1;
        double price = 100.0;
        boolean strictMatch = false;
        String awaitingInput = null;

        OrderSession(String itemId) {
            this.itemId = itemId;
        }
    }
}
