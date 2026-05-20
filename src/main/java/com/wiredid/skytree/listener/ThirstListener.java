package com.wiredid.skytree.listener;

import com.wiredid.skytree.api.ThirstService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ThirstListener implements Listener {

    private final ThirstService thirstService;

    public ThirstListener(ThirstService thirstService) {
        this.thirstService = thirstService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        thirstService.registerPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        thirstService.unregisterPlayer(event.getPlayer());
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        thirstService.handleConsumption(event.getPlayer(), event.getItem());
    }
}

