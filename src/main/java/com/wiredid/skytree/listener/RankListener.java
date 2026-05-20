package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Rank;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.player.PlayerJoinEvent;

public class RankListener implements Listener {

    private final SkytreePlugin plugin;

    public RankListener(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Rank rank = plugin.getRankService().getRank(player.getUniqueId());

        // Divine Glow
        if (rank == Rank.DIVINE) {
            player.setGlowing(true);
        } else {
            player.setGlowing(false);
        }
    }

}
