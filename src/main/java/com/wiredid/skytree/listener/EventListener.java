package com.wiredid.skytree.listener;

import com.wiredid.skytree.system.EventManager;
import com.wiredid.skytree.system.FishingTournament;
import com.wiredid.skytree.system.MobHunt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

public class EventListener implements Listener {

    private final EventManager eventManager;

    public EventListener(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH)
            return;

        EventManager.SkytreeEvent active = eventManager.getActiveEvent();
        if (active instanceof FishingTournament tournament) {
            tournament.onCatch(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null)
            return;

        EventManager.SkytreeEvent active = eventManager.getActiveEvent();
        if (active instanceof MobHunt hunt) {
            hunt.onKill(killer.getUniqueId(), event.getEntityType());
        }
    }
}
