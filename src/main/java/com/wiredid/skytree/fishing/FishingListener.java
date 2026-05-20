package com.wiredid.skytree.fishing;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.Map;
import com.wiredid.skytree.fishing.FishingModels.RodEnchant;

public class FishingListener implements Listener {

    private final FishingService fishingService;
    private final FishStorage fishStorage;

    public FishingListener(FishingService fishingService, FishStorage fishStorage) {
        this.fishingService = fishingService;
        this.fishStorage = fishStorage;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        // Auto Reel Logic
        if (event.getState() == PlayerFishEvent.State.BITE) {
            // Is it auto-reel?
            // All players get auto-reel in this server or toggleable?
            // "Autoreel error, ga ke angkat otomatis rod nya" implies it should be default
            // feature.

            fishingService.handleAutoReel(player, event.getHook(), fishStorage);
            return;
        }

        // Catch Logic happens when we Reel In (CAUGHT_FISH)
        // But since we are Auto-Reeling manually in 'handleAutoReel' (by removing hook
        // and giving fish),
        // we might NOT get the CAUGHT_FISH state if we bypass it.
        // 'handleAutoReel' implementation: hook.remove() -> gives fish.
        // So this event won't trigger CAUGHT_FISH if we remove the hook ourselves!
        // That's fine.

        // However, if player manually reels in:
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            // Give custom fish instead of vanilla
            event.setExpToDrop(5);
            org.bukkit.entity.Entity caught = event.getCaught();
            if (caught != null) {
                caught.remove();
            }

            // Generate Custom Fish
            org.bukkit.inventory.ItemStack rod = player.getInventory().getItemInMainHand();
            Map<RodEnchant, Integer> enchants = fishingService.getEnchantMap(rod);

            int catchCount = 1;
            if (enchants.containsKey(RodEnchant.DOUBLE_HOOK)
                    && Math.random() < (enchants.get(RodEnchant.DOUBLE_HOOK) * 0.05))
                catchCount = 2;
            if (enchants.containsKey(RodEnchant.TRIPLE_HOOK)
                    && Math.random() < (enchants.get(RodEnchant.TRIPLE_HOOK) * 0.02))
                catchCount = 3;
            if (enchants.containsKey(RodEnchant.TRAWLING_NET)
                    && Math.random() < (enchants.get(RodEnchant.TRAWLING_NET) * 0.005))
                catchCount = (int) (Math.random() * 5) + 4;

            for (int i = 0; i < catchCount; i++) {
                org.bukkit.inventory.ItemStack fish = fishingService.generateFish(player, rod);
                fishStorage.addFish(player.getUniqueId(), fish);
            }

            player.sendMessage("§a§l[Fishing] §7Caught " + catchCount + "x fish! Sent to storage.");

            // Auto Cast Schedule
            fishingService.scheduleAutoCast(player);
        } else if (event.getState() == PlayerFishEvent.State.IN_GROUND
                || event.getState() == PlayerFishEvent.State.REEL_IN) {
            // If they miss or just reel in empty
            fishingService.scheduleAutoCast(player);
        }
    }

    // We added handleAutoReel to FishingService which does: hook.remove(), generate
    // fish, add to storage.
    // So 'handleAutoReel' needs access to FishStorage!
    // Wait, I updated FishingService WITHOUT Storage reference.
    // I need to update FishingService to actually store the fish!
    // Or I pass the storage to handleAutoReel?
    // Let's modify FishingService one more time or put the logic HERE in Listener?

    // Logic in Service is better but Service needs dependency.
    // I will update FishingService in the next tool call to accept Storage or
    // handle it.
    // Actually, I can pass it in method call: handleAutoReel(player, hook,
    // fishStorage);

}
