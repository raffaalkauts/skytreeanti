package com.wiredid.skytree.system;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EventManager {

    private final SkytreePlugin plugin;
    private SkytreeEvent activeEvent;
    private long nextEventTime;

    public EventManager(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.nextEventTime = System.currentTimeMillis() + (1000 * 60 * 60); // 1 hour from now
        startEventTask();
    }

    private void startEventTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeEvent != null) {
                    if (activeEvent.isFinished()) {
                        activeEvent.end();
                        activeEvent = null;
                        nextEventTime = System.currentTimeMillis() + (1000 * 60 * 30); // 30 min break
                    } else {
                        activeEvent.tick();
                    }
                } else if (System.currentTimeMillis() >= nextEventTime) {
                    startRandomEvent();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startRandomEvent() {
        int r = new Random().nextInt(2);
        if (r == 0) {
            activeEvent = new FishingTournament(plugin);
        } else {
            activeEvent = new MobHunt(plugin);
        }
        activeEvent.start();
    }

    public SkytreeEvent getActiveEvent() {
        return activeEvent;
    }

    public interface SkytreeEvent {
        String getName();

        void start();

        void tick();

        void end();

        boolean isFinished();

        String getStatus();

        Map<UUID, Double> getLeaderboard();
    }

    public static abstract class BaseEvent implements SkytreeEvent {
        protected final SkytreePlugin plugin;
        protected final long endTime;
        protected final Map<UUID, Double> scores = new HashMap<>();

        public BaseEvent(SkytreePlugin plugin, int durationMinutes) {
            this.plugin = plugin;
            this.endTime = System.currentTimeMillis() + (1000L * 60 * durationMinutes);
        }

        @Override
        public boolean isFinished() {
            return System.currentTimeMillis() >= endTime;
        }

        @Override
        public Map<UUID, Double> getLeaderboard() {
            return scores;
        }

        protected void broadcast(String message) {
            Bukkit.broadcast(ComponentUtil.smartParse("§6§l[Event] §f" + message));
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
            }
        }
    }
}
