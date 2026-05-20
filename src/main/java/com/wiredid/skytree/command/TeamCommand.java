package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.IslandService;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.model.IslandMember;
import com.wiredid.skytree.model.IslandRole;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Team management subcommand for islands
 */
public class TeamCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final IslandService islandService;

    public TeamCommand(SkytreePlugin plugin, IslandService islandService) {
        this.plugin = plugin;
        this.islandService = islandService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        // Get player's island
        Island island = islandService.getIsland(player.getUniqueId()).orElse(null);
        if (island == null) {
            player.sendMessage("§c§l[Skytree] §cYou don't have an island!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "invite" -> handleInvite(player, island, args);
            case "join", "accept" -> handleJoin(player, args);
            case "leave" -> handleLeave(player, island);
            case "deny", "reject" -> handleDeny(player, args);
            case "kick" -> handleKick(player, island, args);
            case "ban" -> handleBan(player, island, args);
            case "unban" -> handleUnban(player, island, args);
            case "trust" -> handleTrust(player, island, args);
            case "untrust" -> handleUntrust(player, island, args);
            case "promote" -> handlePromote(player, island, args);
            case "demote" -> handleDemote(player, island, args);
            case "list" -> handleList(player, island);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleInvite(Player player, Island island, String[] args) {
        // Island checking logic is now inside service.inviteMember somewhat,
        // but we need to pass the target player.

        if (island == null) {
            player.sendMessage("§c§l[Skytree] §cYou need an island to invite people!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c§l[Skytree] §cUsage: /is team invite <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c§l[Skytree] §cPlayer not found!");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§c§l[Skytree] §cYou can't invite yourself!");
            return;
        }

        islandService.inviteMember(player, target);
    }

    private void handleJoin(Player player, String[] args) {
        islandService.acceptInvite(player);
    }

    private void handleDeny(Player player, String[] args) {
        islandService.denyInvite(player);
    }

    private void handleLeave(Player player, Island island) {
        if (island == null) {
            player.sendMessage("§c§l[Skytree] §cYou are not in a team!");
            return;
        }

        if (island.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(
                    "§c§l[Skytree] §cOwners cannot leave! Use §e/is delete §cor promote someone else first.");
            return;
        }

        island.removeMember(player.getUniqueId());
        plugin.getPersistenceService().saveIsland(island);

        // Refresh cache
        if (islandService instanceof com.wiredid.skytree.impl.SkytreeIslandService skytreeService) {
            skytreeService.cacheIsland(island);
        }

        player.sendMessage("§e§l[Skytree] §eYou have left the island.");
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
    }

    private void handleKick(Player player, Island island, String[] args) {
        if (!isOwnerOrCoOwner(player, island)) {
            player.sendMessage("§c§l[Skytree] §cOnly owner/co-owner can kick members!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c§l[Skytree] §cUsage: /is team kick <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID = target != null ? target.getUniqueId() : null;

        // Try to find offline player if target is null
        if (targetUUID == null) {
            // Basic lookup by name in members?
            for (IslandMember m : island.getMembers()) {
                if (Bukkit.getOfflinePlayer(m.getUuid()).getName().equalsIgnoreCase(args[1])) {
                    targetUUID = m.getUuid();
                    break;
                }
            }
        }

        if (targetUUID == null) {
            player.sendMessage("§c§l[Skytree] §cPlayer not found in team!");
            return;
        }

        // Can't kick owner
        if (targetUUID.equals(island.getOwnerUUID())) {
            player.sendMessage("§c§l[Skytree] §cYou can't kick the island owner!");
            return;
        }

        // Remove member
        UUID finalTargetUUID = targetUUID; // for lambda
        boolean removed = island.getMembers().removeIf(m -> m.getUuid().equals(finalTargetUUID));

        if (removed) {
            plugin.getPersistenceService().saveIsland(island);
            // Refresh cache
            if (islandService instanceof com.wiredid.skytree.impl.SkytreeIslandService skytreeService) {
                skytreeService.cacheIsland(island);
            }

            String name = target != null ? target.getName() : args[1];
            player.sendMessage("§a§l[Skytree] §aKicked " + name + " from the island!");
            if (target != null) {
                target.sendMessage("§c§l[Skytree] §cYou've been kicked from " + player.getName() + "'s island!");
            }
        } else {
            player.sendMessage("§c§l[Skytree] §c" + args[1] + " is not a member!");
        }
    }

    private void handleBan(Player player, Island island, String[] args) {
        if (!island.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("§c§l[Skytree] §cOnly the owner can ban players!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c§l[Skytree] §cUsage: /is team ban <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c§l[Skytree] §cPlayer not found!");
            return;
        }

        UUID targetUUID = target.getUniqueId();

        // Kick first if member
        island.getMembers().removeIf(m -> m.getUuid().equals(targetUUID));

        // Add to ban list (using island settings as simple storage)
        island.getSettings().put("banned_" + targetUUID.toString(), true);
        plugin.getPersistenceService().saveIsland(island);

        player.sendMessage("§a§l[Skytree] §aBanned " + target.getName() + " from the island!");
        target.sendMessage("§c§l[Skytree] §cYou've been banned from " + player.getName() + "'s island!");
    }

    private void handleUnban(Player player, Island island, String[] args) {
        if (!island.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("§c§l[Skytree] §cOnly the owner can unban players!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c§l[Skytree] §cUsage: /is team unban <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c§l[Skytree] §cPlayer not found!");
            return;
        }

        island.getSettings().remove("banned_" + target.getUniqueId().toString());
        plugin.getPersistenceService().saveIsland(island);

        player.sendMessage("§a§l[Skytree] §aUnbanned " + target.getName() + "!");
    }

    private void handleTrust(Player player, Island island, String[] args) {
        if (!isOwnerOrCoOwner(player, island)) {
            player.sendMessage("§c§l[Skytree] §cOnly owner/co-owner can trust players!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c§l[Skytree] §cUsage: /is team trust <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c§l[Skytree] §cPlayer not found!");
            return;
        }

        if (isMember(target.getUniqueId(), island)) {
            player.sendMessage("§c§l[Skytree] §c" + target.getName() + " is already a member!");
            return;
        }

        // Use new trust system (defaults to BUILDER)
        islandService.trustPlayer(island, target.getUniqueId(), com.wiredid.skytree.model.TrustLevel.BUILDER);

        player.sendMessage("§a§l[Skytree] §aAdded " + target.getName() + " as trusted player (BUILDER)!");
        target.sendMessage("§a§l[Skytree] §aYou now have trusted access to " + player.getName() + "'s island!");
    }

    private void handleUntrust(Player player, Island island, String[] args) {
        if (!isOwnerOrCoOwner(player, island)) {
            player.sendMessage("§c§l[Skytree] §cOnly owner/co-owner can untrust players!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c§l[Skytree] §cUsage: /is team untrust <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c§l[Skytree] §cPlayer not found!");
            return;
        }

        if (island.getTrustLevel(target.getUniqueId()) == com.wiredid.skytree.model.TrustLevel.VISITOR) {
            player.sendMessage("§c§l[Skytree] §c" + target.getName() + " is not trusted!");
            return;
        }

        islandService.untrustPlayer(island, target.getUniqueId());
        player.sendMessage("§a§l[Skytree] §aRemoved " + target.getName() + "'s trust access!");
        target.sendMessage("§c§l[Skytree] §cYour trust access to " + player.getName() + "'s island was removed!");
    }

    private void handlePromote(Player player, Island island, String[] args) {
        if (!island.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("§c§l[Skytree] §cOnly the owner can promote members!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c§l[Skytree] §cUsage: /is team promote <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c§l[Skytree] §cPlayer not found!");
            return;
        }

        IslandMember member = findMember(target.getUniqueId(), island);
        if (member == null) {
            player.sendMessage("§c§l[Skytree] §c" + target.getName() + " is not a member!");
            return;
        }

        if (member.getRole() == IslandRole.CO_OWNER) {
            player.sendMessage("§c§l[Skytree] §c" + target.getName() + " is already CO-OWNER!");
            return;
        }

        member.setRole(IslandRole.CO_OWNER);
        plugin.getPersistenceService().saveIsland(island);

        player.sendMessage("§a§l[Skytree] §aPromoted " + target.getName() + " to CO-OWNER!");
        target.sendMessage("§a§l[Skytree] §aYou've been promoted to CO-OWNER!");
    }

    private void handleDemote(Player player, Island island, String[] args) {
        if (!island.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage("§c§l[Skytree] §cOnly the owner can demote members!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c§l[Skytree] §cUsage: /is team demote <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c§l[Skytree] §cPlayer not found!");
            return;
        }

        IslandMember member = findMember(target.getUniqueId(), island);
        if (member == null) {
            player.sendMessage("§c§l[Skytree] §c" + target.getName() + " is not a member!");
            return;
        }

        if (member.getRole() == IslandRole.MEMBER || member.getRole() == IslandRole.COOP) {
            player.sendMessage("§c§l[Skytree] §c" + target.getName() + " is already MEMBER/COOP!");
            return;
        }

        member.setRole(IslandRole.MEMBER);
        plugin.getPersistenceService().saveIsland(island);

        player.sendMessage("§a§l[Skytree] §aDemoted " + target.getName() + " to MEMBER!");
        target.sendMessage("§c§l[Skytree] §cYou've been demoted to MEMBER!");
    }

    private void handleList(Player player, Island island) {
        player.sendMessage("§6§l=== Island Team ===");
        player.sendMessage("§eOwner: §f" + Bukkit.getOfflinePlayer(island.getOwnerUUID()).getName());

        if (island.getMembers().isEmpty()) {
            player.sendMessage("§7No other members");
            return;
        }

        player.sendMessage("§eMembers:");
        for (IslandMember member : island.getMembers()) {
            String name = Bukkit.getOfflinePlayer(member.getUuid()).getName();
            String role = member.getRole().name();
            player.sendMessage("  §f" + name + " §7- §e" + role);
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l=== Team Management ===");
        player.sendMessage("§e/is team invite <player> §7- Invite to island");
        player.sendMessage("§e/is team join §7- Accept invite");
        player.sendMessage("§e/is team deny §7- Reject invite");
        player.sendMessage("§e/is team leave §7- Leave current island");
        player.sendMessage("§e/is team kick <player> §7- Remove member");
        player.sendMessage("§e/is team ban <player> §7- Ban from island");
        player.sendMessage("§e/is team unban <player> §7- Unban player");
        player.sendMessage("§e/is team trust <player> §7- Give COOP access");
        player.sendMessage("§e/is team untrust <player> §7- Remove COOP");
        player.sendMessage("§e/is team promote <player> §7- Promote to CO-OWNER");
        player.sendMessage("§e/is team demote <player> §7- Demote to MEMBER");
        player.sendMessage("§e/is team list §7- View all members");
    }

    private boolean isOwnerOrCoOwner(Player player, Island island) {
        if (island.getOwnerUUID().equals(player.getUniqueId()))
            return true;

        IslandMember member = findMember(player.getUniqueId(), island);
        return member != null && member.getRole() == IslandRole.CO_OWNER;
    }

    private boolean isMember(UUID uuid, Island island) {
        if (island.getOwnerUUID().equals(uuid))
            return true;
        return findMember(uuid, island) != null;
    }

    private IslandMember findMember(UUID uuid, Island island) {
        return island.getMembers().stream()
                .filter(m -> m.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("invite", "join", "leave", "deny", "kick", "ban", "unban", "trust", "untrust",
                    "promote", "demote", "list");
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
