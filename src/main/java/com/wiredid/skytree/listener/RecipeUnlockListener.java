package com.wiredid.skytree.listener;

import com.wiredid.skytree.impl.MythicItemManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RecipeUnlockListener implements Listener {

    private final MythicItemManager mythicItemManager;

    public RecipeUnlockListener(MythicItemManager mythicItemManager) {
        this.mythicItemManager = mythicItemManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        mythicItemManager.unlockRecipes(event.getPlayer());

        // Unlock all custom skytree recipes
        // Since we don't have a "get all recipes" method easy to iterate namespaced
        // keys from our service alone without Registry access (which we don't have here
        // explicitly injected as field?),
        // actually we can access it via plugin instance if we had it, but this listener
        // only has mythicItemManager.
        // However, MythicItemManager has access to Registry/Recipes.
        // The previous code only called mythicItemManager.unlockRecipes.
        // Let's assume MythicItemManager handles it, OR we inject plugin here.
        // Since I cannot change the constructor easily without checking where it is
        // instantiated (SkytreePlugin.java line 513).
        // In SkytreePlugin line 513: new RecipeUnlockListener(mythicItemManager)
        // I should modify the class to accept SkytreePlugin or SkytreeRecipeService.

        // Wait, I can't modify the constructor call in SkytreePlugin.java effectively
        // right now without editing that file too.
        // But MythicItemManager's unlockRecipes method might be the place to put logic?
        // Let's check MythicItemManager.
    }
}
