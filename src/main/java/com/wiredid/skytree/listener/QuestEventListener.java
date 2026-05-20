package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.system.QuestSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class QuestEventListener implements Listener {

    private final QuestSystem questSystem;

    public QuestEventListener(SkytreePlugin plugin, QuestSystem questSystem) {

        this.questSystem = questSystem;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        questSystem.addProgress(event.getPlayer(), QuestSystem.QuestType.BLOCK_BREAK, 1);
    }
}
