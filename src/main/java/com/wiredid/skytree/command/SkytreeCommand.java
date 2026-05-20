package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.gui.*;

import com.wiredid.skytree.util.IslandBorderVisualizer;
import com.wiredid.skytree.util.IslandLevelCalculator;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command handler for /skytree (/is /island)
 */
public class SkytreeCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final BiomeGUI biomeGUI;
    private final UpgradeGUI upgradeGUI;
    private final PermissionsGUI permissionsGUI;
    private final LeaderboardGUI leaderboardGUI;
    private final TeamCommand teamCommand;
    private final IslandBorderVisualizer borderVisualizer;
    private final IslandQuestGUI questGUI;
    private final RollbackCommand rollbackCommand;

    public SkytreeCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.biomeGUI = new BiomeGUI(plugin);
        this.upgradeGUI = new UpgradeGUI(plugin);
        this.permissionsGUI = new PermissionsGUI(plugin);
        plugin.getServer().getPluginManager().registerEvents(permissionsGUI, plugin);
        this.leaderboardGUI = plugin.getLeaderboardGUI();
        this.teamCommand = new TeamCommand(plugin, plugin.getIslandService());
        this.borderVisualizer = new IslandBorderVisualizer(plugin);
        this.questGUI = new IslandQuestGUI(plugin);
        plugin.getServer().getPluginManager().registerEvents(questGUI, plugin);
        this.rollbackCommand = new RollbackCommand(plugin.getIslandService(), plugin.getActionLogger());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            if (!plugin.isIslandSystemEnabled()) {
                player.sendMessage("§c§l[Skytree] §cThe Island system is currently disabled by administrators.");
                return true;
            }
            // Open GUI instead of help
            plugin.getIslandMenuGUI().openMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create", "c" -> plugin.getIslandService().createIsland(player);
            case "home", "h" -> plugin.getIslandService().teleportHome(player);
            case "sethome" -> {
                plugin.getIslandService().getIsland(player.getUniqueId()).ifPresentOrElse(
                        island -> {
                            // Only owner/officer check? For now owner
                            if (!island.getOwnerUUID().equals(player.getUniqueId())) {
                                player.sendMessage("§cOnly the island owner can set the island home!");
                                return;
                            }
                            island.setSpawnLocation(player.getLocation());
                            plugin.getPersistenceService().saveIsland(island);
                            player.sendMessage("§a§l[Skytree] §aIsland spawn point updated!");
                        },
                        () -> player.sendMessage("§c§l[Skytree] §cYou don't have an island!"));
            }
            case "delete", "del" -> plugin.getIslandService().deleteIsland(player);
            case "stats", "s" -> showStats(player);
            case "level" -> calculateLevel(player);
            case "top" -> leaderboardGUI.openMain(player);
            case "biome" -> openBiomeGUI(player);
            case "settings" -> openSettingsGUI(player);
            case "upgrade" -> openUpgradeGUI(player);
            case "perms", "p" -> openPermsGUI(player);

            case "quests", "quest", "q" -> questGUI.open(player);
            case "team", "teams", "t" -> {
                if (args.length == 1) {
                    plugin.getIslandService().getIsland(player.getUniqueId()).ifPresentOrElse(
                            island -> plugin.getTeamGUI().open(player, island),
                            () -> player.sendMessage("§c§l[Skytree] §cYou don't have an island!"));
                    return true;
                }
                String[] teamArgs = Arrays.copyOfRange(args, 1, args.length);
                teamCommand.onCommand(sender, command, "team", teamArgs);
            }
            // Shortcuts for team commands
            case "invite", "join", "leave", "kick", "ban", "unban", "trust", "untrust", "promote", "demote" -> {
                teamCommand.onCommand(sender, command, "team", args);
            }
            case "visit", "v" -> {
                if (args.length < 2) {
                    player.sendMessage("§c§l[Skytree] §cUsage: /is visit <player>");
                } else {
                    visitIsland(player, args[1]);
                }
            }
            case "reload" -> {
                if (!player.hasPermission("skytree.admin")) {
                    player.sendMessage("§cNo permission.");
                    return true;
                }
                plugin.reloadConfig();
                plugin.getItemRegistry().reload();
                if (plugin.getMythicItemManager() != null)
                    plugin.getMythicItemManager().reload();
                plugin.getShopService().reload();
                plugin.getQuestSystem().reload();
                plugin.getDailyRewardsSystem().reload();
                plugin.getInvestmentService().reload();
                plugin.getGachaService().reload();
                if (plugin.getMachineProcessor() != null)
                    plugin.getMachineProcessor().reload();
                plugin.getBaitService().reload();

                player.sendMessage("§a§l[Skytree] §aAll systems reloaded successfully!");
            }
            case "admin" -> {
                if (!player.hasPermission("skytree.admin")) {
                    player.sendMessage("§cNo permission.");
                    return true;
                }
                handleAdminCommand(player, args);
            }
            case "border", "b" -> showBorder(player);
            case "rollback" -> {
                String[] rollbackArgs = Arrays.copyOfRange(args, 1, args.length);
                rollbackCommand.onCommand(sender, command, "rollback", rollbackArgs);
            }
            case "tags" -> plugin.getTagGUI().open(player);
            case "invest" -> plugin.getInvestmentGUI().open(player);
            case "history" -> plugin.getChatHistoryGUI().open(player);
            case "bank" -> plugin.getTeamBankGUI().open(player);
            case "help" -> sendHelp(player);
            default -> player.sendMessage("§c§l[Skytree] §cUnknown command. Type §e/is help");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "home", "delete", "stats", "level", "top",
                    "biome", "settings", "upgrade", "perms", "team", "visit", "border", "help", "admin", "quests",
                    "rollback", "tags", "invest", "history", "bank");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("visit")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return Arrays.asList("logs", "staff", "invsee", "ecsee");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("rollback")) {
            return rollbackCommand.onTabComplete(sender, command, alias, Arrays.copyOfRange(args, 1, args.length));
        }
        return new ArrayList<>();
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l=== Skytree Commands ===");
        player.sendMessage("§e/is create §7- Create your island");
        player.sendMessage("§e/is home §7- Teleport to your island");
        player.sendMessage("§e/is delete §7- Delete your island");
        player.sendMessage("§e/is stats §7- View island statistics");
        player.sendMessage("§e/is level §7- Calculate island level");
        player.sendMessage("§e/is top §7- View leaderboards");
        player.sendMessage("§e/is team §7- Manage team members");
        player.sendMessage("§e/is visit <player> §7- Visit another island");
        player.sendMessage("§e/is biome §7- Change island biome");
        player.sendMessage("§e/is settings §7- Island settings");
        player.sendMessage("§e/is upgrade §7- Upgrade your island");

        player.sendMessage("§e/is perms §7- Manage permissions");
        player.sendMessage("§e/is quests §7- Daily island quests");
        player.sendMessage("§e/is border §7- Show island border");
        player.sendMessage("§e/is rollback <count> §7- Rollback island actions");
        player.sendMessage("§e/is tags §7- Manage your player tags");
        player.sendMessage("§e/is invest §7- Trade stocks and bonds");
        if (player.hasPermission("skytree.admin")) {
            player.sendMessage("§c/is admin <logs|staff|invsee|ecsee> §7- Admin Tools");
        }
    }

    private void showStats(Player player) {
        plugin.getIslandService().getIsland(player.getUniqueId()).ifPresentOrElse(
                island -> {
                    int level = IslandLevelCalculator.calculateIslandLevel(island);
                    player.sendMessage("§6§l=== Island Stats ===");
                    player.sendMessage("§eLevel: §f" + level);
                    player.sendMessage("§eSize: §f" + island.getSize() + "x" + island.getSize());
                    player.sendMessage("§eMembers: §f" + (island.getMembers().size() + 1));
                    player.sendMessage("§eBiome: §f" + island.getBiome());
                    player.sendMessage("§eCreated: §f" + formatTime(island.getCreatedAt()));
                },
                () -> player.sendMessage("§c§l[Skytree] §cYou don't have an island!"));
    }

    private void calculateLevel(Player player) {
        plugin.getIslandService().getIsland(player.getUniqueId()).ifPresentOrElse(
                island -> {
                    player.sendMessage("§e§l[Skytree] §7Calculating island level...");
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                        int level = IslandLevelCalculator.calculateIslandLevel(island);
                        island.setLevel(level);
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            player.sendMessage("§a§l[Skytree] §aYour island level is: §e" + level);
                        });
                    });
                },
                () -> player.sendMessage("§c§l[Skytree] §cYou don't have an island!"));
    }

    private void visitIsland(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§c§l[Skytree] §cPlayer not found!");
            return;
        }

        plugin.getIslandService().getIsland(target.getUniqueId()).ifPresentOrElse(
                island -> {
                    // Check if visitor entry is allowed
                    if (island.getSettings().getOrDefault("visitor_entry", true)) {
                        player.teleport(island.getCenter());
                        player.sendMessage("§a§l[Skytree] §aTeleported to " + target.getName() + "'s island!");
                        target.sendMessage("§e§l[Skytree] §e" + player.getName() + " §7visited your island!");
                    } else {
                        player.sendMessage("§c§l[Skytree] §cThis island doesn't allow visitors!");
                    }
                },
                () -> player.sendMessage("§c§l[Skytree] §c" + targetName + " doesn't have an island!"));
    }

    private void showBorder(Player player) {
        plugin.getIslandService().getIsland(player.getUniqueId()).ifPresentOrElse(
                island -> {
                    borderVisualizer.showBorder(player, island);
                    borderVisualizer.showCorners(player, island);
                    player.sendMessage("§a§l[Skytree] §aShowing island border!");
                    player.sendMessage("§7Size: §e" + island.getSize() + "x" + island.getSize());
                },
                () -> player.sendMessage("§c§l[Skytree] §cYou don't have an island!"));
    }

    private void openBiomeGUI(Player player) {
        plugin.getIslandService().getIsland(player.getUniqueId()).ifPresentOrElse(
                island -> biomeGUI.open(player, island),
                () -> player.sendMessage("§c§l[Skytree] §cYou don't have an island!"));
    }

    private void openSettingsGUI(Player player) {
        plugin.getSettingsMainGUI().open(player);
    }

    private void openUpgradeGUI(Player player) {
        plugin.getIslandService().getIsland(player.getUniqueId()).ifPresentOrElse(
                island -> upgradeGUI.open(player, island),
                () -> player.sendMessage("§c§l[Skytree] §cYou don't have an island!"));
    }

    private void openPermsGUI(Player player) {
        plugin.getIslandService().getIsland(player.getUniqueId()).ifPresentOrElse(
                island -> permissionsGUI.open(player, island),
                () -> player.sendMessage("§c§l[Skytree] §cYou don't have an island!"));
    }

    private void handleAdminCommand(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getAdminDashboardGUI().open(player);
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "staff" -> plugin.getAdminService().toggleStaffMode(player);
            case "logs" -> plugin.getAdminLogsGUI().open(player);
            case "invsee" -> {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /is admin invsee <player>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage("§cPlayer not found.");
                    return;
                }
                plugin.getAdminService().openInventory(player, target);
            }
            case "ecsee" -> {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /is admin ecsee <player>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage("§cPlayer not found.");
                    return;
                }
                plugin.getAdminService().openEnderChest(player, target);
            }
            case "toggle" -> {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /is admin toggle <feature>");
                    player.sendMessage("§7Features: island, mythic");
                    return;
                }
                String feature = args[2].toLowerCase();
                if (!feature.equals("island") && !feature.equals("mythic")) {
                    player.sendMessage("§cUnknown feature. Valid: island, mythic");
                    return;
                }
                boolean current = plugin.getConfig().getBoolean("features." + feature, true);
                plugin.setFeatureEnabled(feature, !current);
                player.sendMessage(
                        "§a§l[Skytree] §aFeature '§e" + feature + "§a' is now " + (!current ? "ENABLED" : "DISABLED"));
            }
            default -> player.sendMessage("§cUnknown admin subcommand.");
        }
    }

    private String formatTime(long timestamp) {
        long days = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24);
        if (days == 0)
            return "Today";
        if (days == 1)
            return "Yesterday";
        return days + " days ago";
    }
}
