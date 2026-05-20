package com.wiredid.skytree.minion;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.impl.SkytreeMinionService;
import com.wiredid.skytree.model.MinionData;
import org.bukkit.scheduler.BukkitRunnable;

public class MinionTask extends BukkitRunnable {

    private final SkytreeMinionService minionService;

    public MinionTask(SkytreePlugin plugin, SkytreeMinionService minionService) {
        this.minionService = minionService;
    }

    @Override
    public void run() {
        for (MinionData data : minionService.getPlayerMinions(null)) {
            if (data.isActive()) {
                minionService.executeMinionAI(data.getMinionId());
            }
        }
    }
}
