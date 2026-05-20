package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.AuctionOrder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FictitiousOrderService {

    private final SkytreePlugin plugin;
    private final Map<UUID, String> fakePlayers = new HashMap<>();
    private final List<String> potentialNames = Arrays.asList(
            "Sultan_Sky", "RichPlayer88", "Merchant_Joe", "BlockHoarder",
            "SkyTycoon", "MysteryBuyer", "TheCollector", "RareSeeker",
            "BTC_King", "GlobalTrader", "LegendaryMiner", "IslandMaster");

    public FictitiousOrderService(SkytreePlugin plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {
        long interval = plugin.getConfig().getLong("auction.fake_order_interval_ticks", 1200);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int minPlayers = plugin.getConfig().getInt("auction.fake_order_min_online_players", 10);
            if (Bukkit.getOnlinePlayers().size() < minPlayers) {
                generateFakeOrder();
            }
        }, interval, interval);
    }

    private void generateFakeOrder() {
        // 1. Pick a random fake player
        String name = potentialNames.get(ThreadLocalRandom.current().nextInt(potentialNames.size()));
        UUID fakeUuid = UUID.nameUUIDFromBytes(("FAKE:" + name).getBytes());
        fakePlayers.put(fakeUuid, name);

        // 2. Pick a random interesting item
        Material mat = getRandomMaterial();
        if (mat == null)
            return;

        // 3. Calculate "unreasonable" price (10x - 50x worth)
        double worth = plugin.getWorthService().getItemSellPrice(new ItemStack(mat));
        if (worth <= 0)
            worth = 10.0; // Floor for unknown items

        double multiplier = ThreadLocalRandom.current().nextDouble(10.0, 50.0);
        double price = worth * multiplier;
        int quantity = ThreadLocalRandom.current().nextInt(1, 65);

        // 4. Create the order via AuctionHouseService
        // Note: createOrder usually checks balance. We need a way to bypass for fake
        // players,
        // or just inject directly into the service.
        // I will modify SkytreeAuctionHouseService to have an injection method or use a
        // bypass.

        injectOrder(fakeUuid, new ItemStack(mat), price, quantity);

        plugin.getLogger().info("Generated fictitious order: " + name + " wants " + quantity + "x " + mat.name()
                + " for " + price + " each!");
    }

    private void injectOrder(UUID buyer, ItemStack item, double price, int quantity) {
        // This simulates createOrder but without the real economy check
        ItemStack normalized = item.clone();
        normalized.setAmount(1);

        long durationHours = plugin.getConfig().getLong("auction.listing_expiry_hours", 48);
        AuctionOrder order = new AuctionOrder(buyer, normalized, price, quantity, durationHours * 3600 * 1000L, false);
        plugin.getAuctionHouseService().injectFakeOrder(order);

        // Silent injection: No broadcast per user request
    }

    private Material getRandomMaterial() {
        Material[] mats = Material.values();
        for (int i = 0; i < 50; i++) { // Max attempts
            Material m = mats[ThreadLocalRandom.current().nextInt(mats.length)];
            if (m.isItem() && !m.isAir() && m != Material.BARRIER && m != Material.BEDROCK) {
                return m;
            }
        }
        return null;
    }

    public String getFakeName(UUID uuid) {
        return fakePlayers.get(uuid);
    }
}
