package com.wiredid.skytree.minion.handler;

import com.wiredid.skytree.model.MinionData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;

public class SlayerHandler {

    public SlayerHandler() {
    }

    public void handle(MinionData data) {
        long cooldown = (long) (5000 / data.getEffectiveSpeed());
        if (System.currentTimeMillis() - data.getLastAction() < cooldown) {
            return;
        }

        int radius = data.getEffectiveRange();
        Collection<Entity> entities = data.getLocation().getWorld().getNearbyEntities(data.getLocation(), radius, radius, radius);

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player) && !entity.isDead()) {
                // Kill entity
                living.setHealth(0);
                // Loot will drop naturally, or we could handle it.
                // For Slayer, we usually want drops to go to storage.
                // We'll rely on the EntityDeathEvent to pick up drops if we implement a Collector,
                // or just manually kill and handle drops here.
                data.updateLastAction();
                return;
            }
        }
    }
}
