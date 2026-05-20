package com.wiredid.skytree.api.event;

import com.wiredid.skytree.api.CustomEnchant;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CustomEnchantTriggerEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final CustomEnchant enchant;
    private final int level;
    private final ItemStack item;
    private final Event originalEvent;

    public CustomEnchantTriggerEvent(Player player, CustomEnchant enchant, int level, ItemStack item,
            Event originalEvent) {
        this.player = player;
        this.enchant = enchant;
        this.level = level;
        this.item = item;
        this.originalEvent = originalEvent;
    }

    public Player getPlayer() {
        return player;
    }

    public CustomEnchant getEnchant() {
        return enchant;
    }

    public int getLevel() {
        return level;
    }

    public ItemStack getItem() {
        return item;
    }

    public Event getOriginalEvent() {
        return originalEvent;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
