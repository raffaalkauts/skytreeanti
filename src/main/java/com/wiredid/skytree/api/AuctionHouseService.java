package com.wiredid.skytree.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing the Auction House and Buy Orders.
 */
public interface AuctionHouseService {

    // --- Listings (Sells) ---

    /**
     * Creates a new sell listing.
     * 
     * @param player         The seller
     * @param item           The item to sell
     * @param price          The TOTAL price for the item stack (or per item? User
     *                       said per item in GUI)
     *                       GUI spec said: "$Price each". Let's standardise on
     *                       Price Per Item.
     * @param durationMillis Duration in milliseconds
     * @return The created listing or null if failed
     */
    AuctionHouse createListing(Player player, ItemStack item, double pricePerItem, long durationMillis);

    /**
     * Cancels a listing and returns item to seller (or seller's collection bin).
     * 
     * @param listing The listing to cancel
     * @return true if successful
     */
    boolean cancelListing(AuctionHouse listing);

    boolean claimListing(Player player, AuctionHouse listing);

    /**
     * Purchases an item from a listing.
     * 
     * @param buyer   The buyer
     * @param listing The listing
     * @param amount  Quantity to buy
     * @return true if successful
     */
    boolean purchaseListing(Player buyer, AuctionHouse listing, int amount);

    List<AuctionHouse> getActiveListings();

    List<AuctionHouse> getPlayerListings(UUID sellerId);

    List<AuctionHouse> getExpiredListings(UUID sellerId);

    AuctionHouse getListing(UUID id);

    // --- Orders (Requests) ---

    /**
     * Creates a new buy order (request).
     * 
     * @param player         The buyer
     * @param item           The item requested
     * @param pricePerItem   Price offering per item
     * @param quantity       Total quantity wanted
     * @param durationMillis Duration
     * @return The created order
     */
    AuctionOrder createOrder(Player player, ItemStack item, double pricePerItem, int quantity, long durationMillis,
            boolean strictMatch);

    /**
     * Cancels a buy order and refunds remaining money.
     * 
     * @param order The order to cancel
     * @return true if successful
     */
    boolean cancelOrder(AuctionOrder order);

    /**
     * Fulfills a buy order (sells items to the requester).
     * 
     * @param seller The player selling items to the order
     * @param order  The order being filled
     * @param items  The items to deliver
     * @return true if successful
     */
    boolean fulfillOrder(Player seller, AuctionOrder order, ItemStack items);

    /**
     * Claims items from a fulfilled buy order.
     * 
     * @param player The buyer claiming the items
     * @param order  The order to claim from
     * @return true if successful
     */
    boolean claimOrder(Player player, AuctionOrder order);

    List<AuctionOrder> getActiveOrders();

    List<AuctionOrder> getPlayerOrders(UUID buyerId);

    AuctionOrder getOrder(UUID id);

    // --- General ---

    /**
     * Reloads data from storage.
     */
    void reload();

    /**
     * Saves data to storage.
     */
    void save();

    /**
     * Processes expired listings and orders.
     */
    void processExpiry();

    /**
     * Injects a raw order (bypasses economy checks).
     * Used for system-generated NPC orders.
     */
    void injectFakeOrder(AuctionOrder order);
}
