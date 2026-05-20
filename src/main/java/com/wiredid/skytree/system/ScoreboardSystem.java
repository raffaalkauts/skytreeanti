package com.wiredid.skytree.system;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardSystem {

    private final SkytreePlugin plugin;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public ScoreboardSystem(SkytreePlugin plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    private void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 20L, 20L);
    }

    public void setupScoreboard(Player player) {
        if (!isEnabled(player)) {
            removeScoreboard(player);
            return;
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("skytree", Criteria.DUMMY,
                ComponentUtil.parse("§6§lSKYTREE §7v3.2.3"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        player.setScoreboard(scoreboard);
        scoreboards.put(player.getUniqueId(), scoreboard);
        updateScoreboard(player);
    }

    public void removeScoreboard(Player player) {
        scoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private void updateScoreboard(Player player) {
        if (!isEnabled(player)) {
            removeScoreboard(player);
            return;
        }

        Scoreboard scoreboard = scoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            setupScoreboard(player);
            return;
        }

        Objective obj = scoreboard.getObjective("skytree");
        if (obj == null)
            return;

        updateLine(scoreboard, obj, 15,
                "§7" + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(java.time.LocalDate.now()));
        updateLine(scoreboard, obj, 14, " ");
        updateLine(scoreboard, obj, 13, "§f§lPLAYER INFO");
        updateLine(scoreboard, obj, 12, " §8» §fBalance: §e"
                + NumberUtil.formatCurrency(plugin.getEconomyService().getBalance(player.getUniqueId())));
        updateLine(scoreboard, obj, 11, " §8» §fShards: §b" + plugin.getShardService().getShards(player.getUniqueId()));
        updateLine(scoreboard, obj, 10,
                " §8» §fPlaytime: §d" + plugin.getPlaytimeService().getFormattedPlaytime(player.getUniqueId()));
        updateLine(scoreboard, obj, 9, " §8» §fKills: §a" + player.getStatistic(org.bukkit.Statistic.PLAYER_KILLS));
        updateLine(scoreboard, obj, 8, " §8» §fDeaths: §c" + player.getStatistic(org.bukkit.Statistic.DEATHS));
        updateLine(scoreboard, obj, 7, "  ");

        updateLine(scoreboard, obj, 6, "§f§lACTIVE QUEST");
        QuestSystem.QuestData quest = plugin.getQuestSystem().getActiveQuest(player.getUniqueId());
        if (quest != null) {
            int progress = plugin.getQuestSystem().getProgress(player.getUniqueId(), quest.id);
            updateLine(scoreboard, obj, 5, " §8» §e" + quest.name);
            updateLine(scoreboard, obj, 4, "   §7[" + progress + "/" + quest.target + "]");
        } else {
            updateLine(scoreboard, obj, 5, " §8» §7None (/quest)");
            updateLine(scoreboard, obj, 4, "   ");
        }
        updateLine(scoreboard, obj, 3, "   ");

        updateLine(scoreboard, obj, 2, "§f§lISLAND");
        com.wiredid.skytree.model.Island island = plugin.getIslandService().getIsland(player.getUniqueId())
                .orElse(null);
        if (island != null) {
            updateLine(scoreboard, obj, 1, " §8» §fLevel: §a" + island.getLevel());
        } else {
            updateLine(scoreboard, obj, 1, " §8» §7No Island");
        }
        updateLine(scoreboard, obj, 0, " ");

        EventManager.SkytreeEvent activeEvent = plugin.getEventManager().getActiveEvent();
        if (activeEvent != null) {
            updateLine(scoreboard, obj, -1, "§f§lEVENT");
            updateLine(scoreboard, obj, -2, " §8» " + activeEvent.getStatus());
        } else {
            updateLine(scoreboard, obj, -1, "§f§lBOOSTERS");
            double multiplier = plugin.getMonetizationService().getGlobalMoneyMultiplier();
            updateLine(scoreboard, obj, -2, " §8» §eMoney: §ax" + multiplier);
        }
        updateLine(scoreboard, obj, -3, "      ");
    }

    private void updateLine(Scoreboard sb, Objective obj, int score, String text) {
        String teamName = "line_" + score;
        org.bukkit.scoreboard.Team team = sb.getTeam(teamName);
        if (team == null) {
            team = sb.registerNewTeam(teamName);
            String entry = getEntry(score);
            team.addEntry(entry);
            obj.getScore(entry).setScore(score);
        }
        team.prefix(ComponentUtil.parse(text));
    }

    private String getEntry(int score) {
        return "§" + Integer.toHexString(score) + "§r";
    }

    private boolean isEnabled(Player player) {
        return plugin.getPersistenceService().loadPlayerData(player.getUniqueId()).getSettings()
                .getOrDefault("scoreboard", true);
    }
}
