package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RTPCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public RTPCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        long cooldownMs = plugin.getConfig().getLong("rtp.cooldown_seconds", 30) * 1000;

        if (cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) + cooldownMs) - System.currentTimeMillis();
            if (timeLeft > 0) {
                player.sendMessage("§cYou must wait " + (timeLeft / 1000) + " seconds before using RTP again.");
                return true;
            }
        }

        player.sendMessage("§eSearching for a safe location...");
        Location loc = findSafeLocation(player.getWorld());

        if (loc != null) {
            player.teleport(loc);
            player.sendMessage("§aTeleported to random location: " + loc.getBlockX() + ", " + loc.getBlockY() + ", "
                    + loc.getBlockZ());
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        } else {
            player.sendMessage("§cCould not find a safe location. Please try again.");
        }

        return true;
    }

    private Location findSafeLocation(World world) {
        int radius = plugin.getConfig().getInt("rtp.radius", 5000);
        int attempts = plugin.getConfig().getInt("rtp.search_attempts", 10);

        for (int i = 0; i < attempts; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;
            int y = world.getHighestBlockYAt(x, z);

            Block block = world.getBlockAt(x, y, z); // Top block (usually air or grass)
            // highestBlockY returns the block index that is non-air? Or 1 above?
            // "getHighestBlockYAt" returns the Y of the highest non-air block.

            // Safety Check
            Material type = block.getType();
            if (isSafe(type)) {
                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }

    private boolean isSafe(Material type) {
        if (type.isSolid() && type != Material.MAGMA_BLOCK && type != Material.CACTUS && type != Material.LAVA) {
            return true;
        }
        // Check for water?
        if (type == Material.WATER)
            return false; // Don't drop in ocean if possible
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        return new ArrayList<>();
    }
}
