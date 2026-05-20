package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.*;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class SkytreeAuctionHouseService implements AuctionHouseService {

    private final SkytreePlugin plugin;
    private final EconomyService economyService;
    private final List<AuctionHouse> activeListings = new CopyOnWriteArrayList<>();
    private final List<AuctionOrder> activeOrders = new CopyOnWriteArrayList<>();

    private File listingsFile;
    private File ordersFile;
    private YamlConfiguration listingsConfig;
    private YamlConfiguration ordersConfig;

    public SkytreeAuctionHouseService(SkytreePlugin plugin, EconomyService economyService) {
        this.plugin = plugin;
        this.economyService = economyService;

        reload();

        // Start expiry task
        long expiryInterval = plugin.getConfig().getLong("auction.expiry_check_interval_ticks", 1200);
        Bukkit.getScheduler().runTaskTimer(plugin, this::processExpiry, expiryInterval, expiryInterval);
    }

    @Override
    public void reload() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists())
            dataFolder.mkdirs();

        listingsFile = new File(dataFolder, "listings.yml");
        ordersFile = new File(dataFolder, "orders.yml");

        if (!listingsFile.exists()) {
            try {
                listingsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!ordersFile.exists()) {
            try {
                ordersFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        listingsConfig = YamlConfiguration.loadConfiguration(listingsFile);
        ordersConfig = YamlConfiguration.loadConfiguration(ordersFile);

        loadListings();
        loadOrders();
    }

    private synchronized void loadListings() {
        activeListings.clear();
        ConfigurationSection sec = listingsConfig.getConfigurationSection("listings");
        if (sec == null)
            return;

        for (String idStr : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                UUID seller = UUID.fromString(sec.getString(idStr + ".seller"));
                ItemStack item = sec.getItemStack(idStr + ".item");
                double price = sec.getDouble(idStr + ".price");
                long listed = sec.getLong(idStr + ".listed");
                long expiry = sec.getLong(idStr + ".expiry");
                int qty = sec.getInt(idStr + ".quantity");
                boolean expired = sec.getBoolean(idStr + ".expired", false);

                AuctionHouse listing = new AuctionHouse(id, seller, item, price, listed, expiry, qty, expired);
                activeListings.add(listing);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load listing " + idStr);
            }
        }
    }

    private synchronized void loadOrders() {
        activeOrders.clear();
        ConfigurationSection sec = ordersConfig.getConfigurationSection("orders");
        if (sec == null)
            return;

        for (String idStr : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                UUID buyer = UUID.fromString(sec.getString(idStr + ".buyer"));
                ItemStack item = sec.getItemStack(idStr + ".item");
                double price = sec.getDouble(idStr + ".price");
                int total = sec.getInt(idStr + ".total");
                int filled = sec.getInt(idStr + ".filled");
                int claimed = sec.getInt(idStr + ".claimed", 0);
                long created = sec.getLong(idStr + ".created");
                long expiry = sec.getLong(idStr + ".expiry");

                boolean strict = sec.getBoolean(idStr + ".strict", false);

                AuctionOrder order = new AuctionOrder(id, buyer, item, price, total, filled, claimed, created, expiry,
                        strict);
                activeOrders.add(order);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load order " + idStr);
            }
        }
    }

    @Override
    public AuctionHouse createListing(Player player, ItemStack item, double pricePerItem, long durationMillis) {
        if (item == null || item.getAmount() <= 0)
            return null;

        double listingFee = plugin.getConfig().getDouble("auction.listing_fee", 10.0);
        if (plugin.getEconomyService().getBalance(player.getUniqueId()) < listingFee) {
            player.sendMessage("§cYou cannot afford the listing fee of §e\u20AE " + listingFee + "§c.");
            return null;
        }

        // CRITICAL: Verify items actually exist and can be removed
        // isSimilar check might fail due to worth lore, so we use manual removal with
        // ignore worth
        if (!removeItemsCorrectly(player, item, item.getAmount())) {
            player.sendMessage("§cCould not find the items in your inventory! (Meta mismatch)");
            return null;
        }

        plugin.getEconomyService().removeBalance(player.getUniqueId(), listingFee);

        if (plugin.getEconomyManager() != null) {
                plugin.getEconomyManager().addToReserve(listingFee);
        }

        AuctionHouse listing = new AuctionHouse(player.getUniqueId(), item, pricePerItem, durationMillis);
        activeListings.add(listing);

        saveListing(listing);
        player.sendMessage("§aItem listed on the market! Fee paid: §e\u20AE " + listingFee + "§a.");
        return listing;
    }

    /**
     * Helper to remove items from player inventory while ignoring worth-system
     * metadata.
     */
    private boolean removeItemsCorrectly(Player player, ItemStack template, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        WorthService worth = plugin.getWorthService();

        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is == null)
                continue;

            if (worth.isSimilarIgnoringWorth(is, template)) {
                if (is.getAmount() <= remaining) {
                    remaining -= is.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    is.setAmount(is.getAmount() - remaining);
                    remaining = 0;
                }
            }
            if (remaining <= 0)
                break;
        }
        return remaining <= 0;
    }

    @Override
    public boolean cancelListing(AuctionHouse listing) {
        if (!activeListings.contains(listing))
            return false;

        listing.setExpired(true);
        saveListing(listing);

        return true;
    }

    @Override
    public boolean claimListing(Player player, AuctionHouse listing) {
        if (!activeListings.contains(listing))
            return false;
        if (!listing.isExpired())
            return false;
        if (!listing.getSeller().equals(player.getUniqueId()))
            return false;

        ItemStack item = listing.getItem().clone();
        item.setAmount(listing.getQuantityRemaining());

        Map<Integer, ItemStack> left = player.getInventory().addItem(item);
        if (!left.isEmpty()) {
            player.sendMessage("§cInventory full! Could not claim items.");
            return false;
        }

        activeListings.remove(listing);
        listingsConfig.set("listings." + listing.getId().toString(), null);
        saveListingsConfig();
        player.sendMessage("§6§lMarket §8» §aSuccessfully claimed items!");
        return true;
    }

    @Override
    public synchronized boolean purchaseListing(Player buyer, AuctionHouse listing, int amount) {
        if (!activeListings.contains(listing))
            return false;
        if (listing.isExpired())
            return false;
        if (amount > listing.getQuantityRemaining())
            amount = listing.getQuantityRemaining();
        if (amount <= 0)
            return false;

        double cost = listing.getPricePerItem() * amount;

        if (economyService.getBalance(buyer.getUniqueId()) < cost) {
            buyer.sendMessage("§cInsufficient funds.");
            return false;
        }

        economyService.removeBalance(buyer.getUniqueId(), cost);

        double taxPercent = plugin.getConfig().getDouble("auction.tax_percentage", 5.0);
        double tax = cost * (taxPercent / 100.0);
        double netPayout = cost - tax;

        economyService.addBalance(listing.getSeller(), netPayout);

        if (tax > 0) {
            if (plugin.getEconomyManager() != null) {
                plugin.getEconomyManager().addToReserve(tax);
            }
            Player seller = Bukkit.getPlayer(listing.getSeller());
            if (seller != null) {
                seller.sendMessage("§7[§6Market§7] §aYou sold items for §e" + NumberUtil.formatCurrency(cost)
                        + " §a(Tax: §c" + NumberUtil.formatCurrency(tax) + "§a)");
            }
        }

        listing.setQuantityRemaining(listing.getQuantityRemaining() - amount);

        ItemStack purchased = listing.getItem().clone();
        purchased.setAmount(amount);

        // CRITICAL: Handle full inventory by dropping items naturally
        Map<Integer, ItemStack> leftovers = buyer.getInventory().addItem(purchased);
        if (!leftovers.isEmpty()) {
            buyer.sendMessage("§e§l[Market] §7Inventory full! Items dropped on the ground.");
            for (ItemStack leftover : leftovers.values()) {
                buyer.getWorld().dropItemNaturally(buyer.getLocation(), leftover);
            }
        }

        if (listing.getQuantityRemaining() <= 0) {
            activeListings.remove(listing);
            listingsConfig.set("listings." + listing.getId().toString(), null);
        } else {
            saveListing(listing);
        }
        saveListingsConfig();

        return true;
    }

    @Override
    public List<AuctionHouse> getActiveListings() {
        return activeListings.stream()
                .filter(l -> !l.isExpired() && l.getQuantityRemaining() > 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuctionHouse> getPlayerListings(UUID sellerId) {
        return activeListings.stream()
                .filter(l -> l.getSeller().equals(sellerId) && !l.isExpired())
                .collect(Collectors.toList());
    }

    @Override
    public List<AuctionHouse> getExpiredListings(UUID sellerId) {
        return activeListings.stream()
                .filter(l -> l.getSeller().equals(sellerId) && l.isExpired())
                .collect(Collectors.toList());
    }

    @Override
    public AuctionHouse getListing(UUID id) {
        return activeListings.stream().filter(l -> l.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public AuctionOrder createOrder(Player player, ItemStack item, double pricePerItem, int quantity,
            long durationMillis, boolean strictMatch) {
        double totalCost = pricePerItem * quantity;

        if (economyService.getBalance(player.getUniqueId()) < totalCost) {
            player.sendMessage("§cInsufficient funds to guarantee this order.");
            return null;
        }

        economyService.removeBalance(player.getUniqueId(), totalCost);

        ItemStack normalizedItem = item.clone();
        normalizedItem.setAmount(1);

        AuctionOrder order = new AuctionOrder(player.getUniqueId(), normalizedItem, pricePerItem, quantity,
                durationMillis, strictMatch);
        activeOrders.add(order);
        saveOrder(order);

        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? com.wiredid.skytree.util.ComponentUtil.toLegacy(item.getItemMeta().displayName())
                : item.getType().name().replace("_", " ");

        String broadcastMsg = "\n§6§lBounty §8» §e" + player.getName() + " §7is looking for §f" + quantity + "x "
                + itemName +
                "\n§7Reward: §a" + NumberUtil.formatCurrency(pricePerItem) + " each §8| §7Type: "
                + (strictMatch ? "§cStrict" : "§aAny") +
                "\n§e<click:run_command:/orders>[CLICK TO SELL ITEMS]</click>\n";

        Bukkit.broadcast(com.wiredid.skytree.util.ComponentUtil.smartParse(broadcastMsg));

        return order;
    }

    @Override
    public boolean cancelOrder(AuctionOrder order) {
        if (!activeOrders.contains(order))
            return false;

        int remaining = order.getRemainingQuantity();
        double refund = remaining * order.getPricePerItem();
        if (refund > 0) {
            economyService.addBalance(order.getBuyer(), refund);
            Player buyer = Bukkit.getPlayer(order.getBuyer());
            if (buyer != null) {
                buyer.sendMessage("§6§lMarket §8» §eOrder cancelled! Refunded §a" + NumberUtil.formatCurrency(refund)
                        + " §efor unfilled items.");
            }
        }

        if (order.getUnclaimedQuantity() > 0) {
            order.setExpiryTime(0);
            saveOrder(order);
        } else {
            activeOrders.remove(order);
            ordersConfig.set("orders." + order.getId().toString(), null);
            saveOrdersConfig();
        }

        return true;
    }

    @Override
    public synchronized boolean fulfillOrder(Player seller, AuctionOrder order, ItemStack items) {
        if (!activeOrders.contains(order))
            return false;
        if (order.isExpired())
            return false;

        WorthService worth = plugin.getWorthService();
        if (order.isStrictMatch()) {
            if (!worth.isSimilarIgnoringWorth(items, order.getRequestedItem()))
                return false;
        } else {
            if (items.getType() != order.getRequestedItem().getType())
                return false;
        }

        int fillAmount = Math.min(items.getAmount(), order.getRemainingQuantity());
        if (fillAmount <= 0)
            return false;

        // Perform inventory removal using worth-ignoring logic
        if (!removeItemsCorrectly(seller, items, fillAmount)) {
            return false;
        }

        // Logic now CENTRALIZED in Service (includes taxes)
        double payout = fillAmount * order.getPricePerItem();
        double taxPercent = plugin.getConfig().getDouble("auction.tax_percentage", 5.0);
        double tax = payout * (taxPercent / 100.0);
        double netPayout = payout - tax;

        economyService.addBalance(seller.getUniqueId(), netPayout);

        if (tax > 0) {
            if (plugin.getEconomyManager() != null) {
                plugin.getEconomyManager().addToReserve(tax);
            }
            seller.sendMessage("§7[§6Market§7] §aYou fulfilled an order for §e" + NumberUtil.formatCurrency(payout)
                    + " §a(Tax: §c" + NumberUtil.formatCurrency(tax) + "§a)");
        }

        order.addFilled(fillAmount);
        saveOrder(order);

        return true;
    }

    @Override
    public boolean claimOrder(Player player, AuctionOrder order) {
        if (!activeOrders.contains(order))
            return false;
        if (!order.getBuyer().equals(player.getUniqueId()))
            return false;

        int unclaimed = order.getUnclaimedQuantity();
        if (unclaimed <= 0)
            return false;

        ItemStack item = order.getRequestedItem().clone();
        item.setAmount(unclaimed);

        Map<Integer, ItemStack> left = player.getInventory().addItem(item);
        if (!left.isEmpty()) {
            int failed = left.values().stream().mapToInt(ItemStack::getAmount).sum();
            int claimed = unclaimed - failed;
            order.addClaimed(claimed);
            player.sendMessage("§cInventory full! Only claimed " + claimed + " items.");
        } else {
            order.addClaimed(unclaimed);
            player.sendMessage("§aSuccessfully claimed " + unclaimed + " items!");
        }

        if (order.getRemainingQuantity() <= 0 && order.getUnclaimedQuantity() <= 0) {
            activeOrders.remove(order);
            ordersConfig.set("orders." + order.getId().toString(), null);
            saveOrdersConfig();
        } else {
            saveOrder(order);
        }

        return true;
    }

    @Override
    public List<AuctionOrder> getActiveOrders() {
        return activeOrders.stream().filter(o -> !o.isExpired() && !o.isFilled()).collect(Collectors.toList());
    }

    @Override
    public List<AuctionOrder> getPlayerOrders(UUID buyerId) {
        return activeOrders.stream().filter(o -> o.getBuyer().equals(buyerId)).collect(Collectors.toList());
    }

    @Override
    public AuctionOrder getOrder(UUID id) {
        return activeOrders.stream().filter(o -> o.getId().equals(id)).findFirst().orElse(null);
    }

    private synchronized void saveListing(AuctionHouse listing) {
        String path = "listings." + listing.getId().toString();
        listingsConfig.set(path + ".seller", listing.getSeller().toString());
        listingsConfig.set(path + ".item", listing.getItem());
        listingsConfig.set(path + ".price", listing.getPricePerItem());
        listingsConfig.set(path + ".listed", listing.getListedTime());
        listingsConfig.set(path + ".expiry", listing.getExpiryTime());
        listingsConfig.set(path + ".quantity", listing.getQuantityRemaining());
        listingsConfig.set(path + ".expired", listing.isExpired());
        saveListingsConfig();
    }

    private synchronized void saveOrder(AuctionOrder order) {
        String path = "orders." + order.getId().toString();
        ordersConfig.set(path + ".buyer", order.getBuyer().toString());
        ordersConfig.set(path + ".item", order.getRequestedItem());
        ordersConfig.set(path + ".price", order.getPricePerItem());
        ordersConfig.set(path + ".total", order.getTotalQuantity());
        ordersConfig.set(path + ".filled", order.getFilledQuantity());
        ordersConfig.set(path + ".claimed", order.getClaimedQuantity());
        ordersConfig.set(path + ".created", order.getCreatedTime());
        ordersConfig.set(path + ".expiry", order.getExpiryTime());
        ordersConfig.set(path + ".strict", order.isStrictMatch());
        saveOrdersConfig();
    }

    private synchronized void saveListingsConfig() {
        try {
            listingsConfig.save(listingsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void saveOrdersConfig() {
        try {
            ordersConfig.save(ordersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save() {
        activeListings.forEach(this::saveListing);
        activeOrders.forEach(this::saveOrder);
    }

    @Override
    public synchronized void processExpiry() {
        for (AuctionHouse l : activeListings) {
            if (!l.isExpired() && System.currentTimeMillis() > l.getExpiryTime()) {
                l.setExpired(true);
                saveListing(l);
            }
        }

        for (AuctionOrder o : activeOrders) {
            if (!o.isExpired() && System.currentTimeMillis() > o.getExpiryTime()) {
                int remaining = o.getRemainingQuantity();
                double refund = remaining * o.getPricePerItem();
                if (refund > 0) {
                    economyService.addBalance(o.getBuyer(), refund);
                    Player buyer = Bukkit.getPlayer(o.getBuyer());
                    if (buyer != null) {
                        buyer.sendMessage("§6§lMarket §8» §eYour order for " + o.getRequestedItem().getType().name()
                                + " has expired. Refunded §a" + NumberUtil.formatCurrency(refund) + "§e.");
                    }
                }

                o.setExpiryTime(0);
                saveOrder(o);
            }
        }
    }

    public void injectFakeOrder(AuctionOrder order) {
        activeOrders.add(order);
        saveOrder(order);
    }
}
