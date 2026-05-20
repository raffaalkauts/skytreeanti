package com.wiredid.skytree.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import java.util.List;

public class CommandUtil {

    /**
     * Resolves a list of OfflinePlayers from a string argument.
     * Supports:
     * - Selectors (@a, @p, @r, etc.) if key starts with '@'
     * - Specific player names (Online or Offline)
     * 
     * @param sender The command sender (needed for selectors)
     * @param arg    The argument string (e.g. "@a", "Steve", "Notch")
     * @return A list of matching OfflinePlayers. Empty if none found.
     */
    public static List<OfflinePlayer> resolveTargets(CommandSender sender, String arg) {
        List<OfflinePlayer> targets = new ArrayList<>();

        // 1. Selector Support (@a, @p, @r, @e[type=player])
        if (arg.startsWith("@")) {
            try {
                List<Entity> entities = Bukkit.selectEntities(sender, arg);
                for (Entity entity : entities) {
                    if (entity instanceof Player) {
                        targets.add((Player) entity);
                    }
                }
            } catch (IllegalArgumentException e) {
                // Invalid selector syntax - suppress error or let caller handle empty list
            }
            return targets;
        }

        // 2. Specific Player (Online or Offline)
        // Try getting online player first for exact match with proper casing
        Player onlinePlayer = Bukkit.getPlayerExact(arg);
        if (onlinePlayer != null) {
            targets.add(onlinePlayer);
        } else {
            // Fallback to offline player lookup
            // Note: This creates a profile if it doesn't exist on some server versions,
            // but for admin commands this is usually acceptable.
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(arg);
            // Verify if they have played before if we want to be strict,
            // but for "giving" stuff, sometimes we want to prep an account.
            // User requested offline support, so we return it.
            if (offlinePlayer != null) {
                targets.add(offlinePlayer);
            }
        }

        return targets;
    }
}
