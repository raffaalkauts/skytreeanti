package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.economy.JobData;
import com.wiredid.skytree.economy.JobService;
import com.wiredid.skytree.model.Rank;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class JobAdminCommand implements CommandExecutor, TabCompleter {

    private final SkytreePlugin plugin;
    private final JobService jobService;

    public JobAdminCommand(SkytreePlugin plugin, JobService jobService) {
        this.plugin = plugin;
        this.jobService = jobService;
    }

    private boolean isAuthorized(CommandSender sender) {
        if (sender.isOp() || !(sender instanceof Player)) return true;
        Rank rank = plugin.getRankService().getRank(((Player) sender).getUniqueId());
        return rank.isAtLeast(Rank.ADMIN);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isAuthorized(sender)) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /jobadmin <info|setlevel|setxp|reset|top|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /jobadmin info <player>");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
                JobData data = jobService.getJobData(target.getUniqueId());
                sender.sendMessage("§6§l=== Jobs: " + target.getName() + " ===");
                for (String jobId : JobService.ALL_JOBS) {
                    JobData.JobProgress p = data.getOrCreate(jobId);
                    String dn = JobService.JOB_DISPLAY.getOrDefault(jobId, jobId);
                    sender.sendMessage("§e" + dn + " §7Lv.§f" + p.level
                            + " §7XP: §b" + String.format("%.0f", p.xp)
                            + " §7Earned: §a" + String.format("%.2f", p.totalEarned)
                            + " §7Actions: §e" + p.actions);
                }
            }
            case "setlevel" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /jobadmin setlevel <player> <job> <level>");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
                String jobId = args[2].toLowerCase();
                if (!JobService.ALL_JOBS.contains(jobId)) {
                    sender.sendMessage("§cUnknown job. Available: " + String.join(", ", JobService.ALL_JOBS));
                    return true;
                }
                try {
                    int level = Integer.parseInt(args[3]);
                    if (level < 0) { sender.sendMessage("§cLevel must be >= 0"); return true; }
                    jobService.setLevel(target.getUniqueId(), jobId, level);
                    sender.sendMessage("§aSet " + target.getName() + "'s " + jobId + " to level " + level);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid level: " + args[3]);
                }
            }
            case "setxp" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /jobadmin setxp <player> <job> <xp>");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
                String jobId = args[2].toLowerCase();
                if (!JobService.ALL_JOBS.contains(jobId)) {
                    sender.sendMessage("§cUnknown job. Available: " + String.join(", ", JobService.ALL_JOBS));
                    return true;
                }
                try {
                    double xp = Double.parseDouble(args[3]);
                    if (xp < 0) { sender.sendMessage("§cXP must be >= 0"); return true; }
                    jobService.setXp(target.getUniqueId(), jobId, xp);
                    sender.sendMessage("§aSet " + target.getName() + "'s " + jobId + " XP to " + xp);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid XP: " + args[3]);
                }
            }
            case "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /jobadmin reset <player> [job]");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
                if (args.length >= 3) {
                    String jobId = args[2].toLowerCase();
                    if (!JobService.ALL_JOBS.contains(jobId)) {
                        sender.sendMessage("§cUnknown job. Available: " + String.join(", ", JobService.ALL_JOBS));
                        return true;
                    }
                    jobService.resetPlayerJob(target.getUniqueId(), jobId);
                    sender.sendMessage("§aReset " + target.getName() + "'s " + jobId + " job.");
                } else {
                    jobService.resetPlayer(target.getUniqueId());
                    sender.sendMessage("§aReset all jobs for " + target.getName() + ".");
                }
            }
            case "top" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /jobadmin top <job>");
                    return true;
                }
                String jobId = args[1].toLowerCase();
                if (!JobService.ALL_JOBS.contains(jobId)) {
                    sender.sendMessage("§cUnknown job. Available: " + String.join(", ", JobService.ALL_JOBS));
                    return true;
                }
                String dn = JobService.JOB_DISPLAY.getOrDefault(jobId, jobId);
                sender.sendMessage("§6§l=== Top " + dn + " ===");
                Map<UUID, JobData> top = jobService.getTopPlayers(jobId, 10);
                int rank = 1;
                for (Map.Entry<UUID, JobData> entry : top.entrySet()) {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    int lv = entry.getValue().getLevel(jobId);
                    sender.sendMessage("§e#" + rank + " §f" + (name != null ? name : "Unknown") + " §7- §bLv." + lv);
                    rank++;
                }
                if (top.isEmpty()) {
                    sender.sendMessage("§7No players have leveled this job yet.");
                }
            }
            case "reload" -> {
                jobService.reload();
                sender.sendMessage("§aJob config reloaded.");
            }
            default -> sender.sendMessage("§cUnknown subcommand. Use: info, setlevel, setxp, reset, top, reload");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!isAuthorized(sender)) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            result.addAll(List.of("info", "setlevel", "setxp", "reset", "top", "reload"));
        } else if (args.length == 2 && List.of("info", "setlevel", "setxp", "reset").contains(args[0].toLowerCase())) {
            result.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        } else if (args.length == 3 && List.of("setlevel", "setxp", "reset").contains(args[0].toLowerCase())) {
            result.addAll(JobService.ALL_JOBS.stream().filter(j -> j.startsWith(args[2].toLowerCase())).toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            result.addAll(JobService.ALL_JOBS.stream().filter(j -> j.startsWith(args[1].toLowerCase())).toList());
        }
        return result.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }
}
