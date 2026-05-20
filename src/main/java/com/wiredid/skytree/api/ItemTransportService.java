package com.wiredid.skytree.api;

import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing item transport links between containers and machines
 */
public interface ItemTransportService {

    /**
     * Create a link between source and destination
     * 
     * @param source      Source location (chest/machine)
     * @param destination Destination location (chest/machine)
     * @param playerId    Player creating the link
     * @return true if link created
     */
    boolean createLink(Location source, Location destination, UUID playerId);

    /**
     * Remove a link from source
     * 
     * @param source   Source location
     * @param playerId Player removing (for permission check)
     * @return true if removed
     */
    boolean removeLink(Location source, UUID playerId);

    /**
     * Remove all links from a location
     * 
     * @param location Location
     * @param playerId Player UUID
     */
    void removeAllLinks(Location location, UUID playerId);

    /**
     * Get all links for an island
     * 
     * @param islandId Island UUID
     * @return Map of source location to list of destination locations
     */
    Map<Location, List<Location>> getLinks(UUID islandId);

    /**
     * Get destinations for a source
     * 
     * @param source Source location
     * @return List of destination locations
     */
    List<Location> getDestinations(Location source);

    /**
     * Check if location is linked (as source)
     * 
     * @param location Location to check
     * @return true if has links
     */
    boolean isLinked(Location location);

    /**
     * Transfer items from source to all destinations
     * Called by scheduled task
     * 
     * @param source Source location
     */
    void transferItems(Location source);

    /**
     * Transfer all items for all islands
     * Called by scheduled task every 5 seconds
     */
    void transferAllItems();

    /**
     * Get link count for a container
     * 
     * @param source Source location
     * @return Number of links
     */
    int getLinkCount(Location source);

    /**
     * Check if max links reached
     * 
     * @param source Source location
     * @return true if at max (5 links)
     */
    boolean isMaxLinksReached(Location source);

    /**
     * Spawn visual particle line between linked containers
     * 
     * @param source      Source location
     * @param destination Destination location
     */
    void spawnLinkParticle(Location source, Location destination);
}
