package com.wiredid.skytree.api;

import com.wiredid.skytree.model.Island;
import org.bukkit.entity.Player;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.Collection;

/**
 * Service for managing islands
 */
public interface IslandService {

    /**
     * Creates a new island for the player
     * 
     * @param player The player to create an island for
     */
    void createIsland(Player player);

    /**
     * Gets the next available grid ID for island placement
     * 
     * @return The next grid ID
     */
    int getNextIslandId();

    /**
     * Gets cached island level
     */
    int getIslandLevel(UUID ownerId);

    /**
     * Gets all cached island levels.
     */
    Map<UUID, Integer> getAllIslandLevels();

    /**
     * Teleports player to their island home
     * 
     * @param player The player to teleport
     */
    void teleportHome(Player player);

    /**
     * Deletes the player's island
     * 
     * @param player The player whose island to delete
     */
    void deleteIsland(Player player);

    /**
     * Gets an island by owner UUID
     * 
     * @param ownerId The owner's UUID
     * @return Optional containing the island if found
     */
    Optional<Island> getIsland(UUID ownerId);

    /**
     * Gets an island by island UUID
     * 
     * @param islandId The island's UUID
     * @return Optional containing the island if found
     */
    Optional<Island> getIslandById(UUID islandId);

    /**
     * Checks if a player has an island
     * 
     * @param uuid The player's UUID
     * @return true if player has an island
     */
    boolean hasIsland(UUID uuid);

    /**
     * Gets the island at a specific location
     * 
     * @param location The location to check
     * @return Optional containing the island if at that location
     */
    Optional<Island> getIslandAtLocation(org.bukkit.Location location);

    /**
     * Invites a player to join an island
     */
    void inviteMember(Player inviter, Player target);

    /**
     * Accepts a pending invitation
     */
    void acceptInvite(Player player);

    /**
     * Denies a pending invitation
     */
    void denyInvite(Player player);

    /**
     * Gets all currently loaded islands in memory.
     */
    Collection<Island> getLoadedIslands();

    /**
     * Saves an island to persistence.
     */
    void saveIsland(Island island);

    /**
     * Trusts a player on an island
     */
    void trustPlayer(Island island, UUID playerUUID, com.wiredid.skytree.model.TrustLevel level);

    /**
     * Untrusts a player on an island
     */
    void untrustPlayer(Island island, UUID playerUUID);

    /**
     * Gets the trust level of a player on an island
     */
    com.wiredid.skytree.model.TrustLevel getTrustLevel(Island island, UUID playerUUID);

    /**
     * Checks if a player can modify a block at a specific location
     * 
     * @param player   The player to check
     * @param location The location to check
     * @return true if player can modify
     */
    boolean canModify(Player player, org.bukkit.Location location);
}
