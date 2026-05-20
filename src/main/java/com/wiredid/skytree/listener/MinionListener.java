package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.api.MinionService;
import com.wiredid.skytree.gui.MinionGUI;
import com.wiredid.skytree.model.MinionData;
import com.wiredid.skytree.model.MinionType;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class MinionListener implements Listener {

    private final SkytreePlugin plugin;
    private final MinionService minionService;
    private final ItemRegistry itemRegistry;
    private final MinionGUI minionGUI;

    public MinionListener(SkytreePlugin plugin, MinionService minionService, ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.minionService = minionService;
        this.itemRegistry = itemRegistry;
        this.minionGUI = new MinionGUI(plugin, minionService);
    }

    @EventHandler
    public void onMinionInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand as))
            return;

        // Find if this AS belongs to a minion
        MinionData data = minionService.getMinionAtLocation(as.getLocation().getBlock().getLocation());
        if (data != null) {
            event.setCancelled(true);
            minionGUI.open(event.getPlayer(), data.getMinionId());
        }
    }

    @EventHandler
    public void onPlace(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ARMOR_STAND)
            return;
        if (!itemRegistry.isCustomItem(item))
            return;

        String id = itemRegistry.getItemId(item);
        if (id == null || !id.startsWith("minion_"))
            return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        // Check Island Limit
        var islandOpt = plugin.getIslandService().getIsland(player.getUniqueId());
        if (islandOpt.isEmpty()) {
            player.sendMessage("§c[Minion] §fYou must be on an island to place a minion.");
            return;
        }

        var island = islandOpt.get();
        int currentMinions = minionService.getAllMinionsByIsland(island.getId()).size();
        int maxMinions = 5 + (island.getLevel() / 10);

        if (currentMinions >= maxMinions) {
            player.sendMessage("§c[Minion] §fIsland limit reached! (" + currentMinions + "/" + maxMinions + ")");
            player.sendMessage("§7Level up your island to increase this limit.");
            return;
        }

        MinionType type = switch (id) {
            case "minion_farmer" -> MinionType.FARMER;
            case "minion_miner" -> MinionType.MINER;
            case "minion_lumberjack" -> MinionType.LUMBERJACK;
            case "minion_fisher" -> MinionType.FISHER;
            case "minion_auto_sieve" -> MinionType.AUTO_SIEVE;
            default -> null;
        };

        if (type == null)
            return;

        if (minionService.placeMinion(player.getUniqueId(),
                event.getClickedBlock().getRelative(event.getBlockFace()).getLocation(), type) != null) {
            item.setAmount(item.getAmount() - 1);
            player.sendMessage("§a[Minion] §fMinion placed! (§7" + (currentMinions + 1) + "/" + maxMinions + "§a)");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ARMOR_STAND_PLACE, 1f, 1f);
        } else {
            player.sendMessage("§c[Minion] §fCould not place minion here.");
        }
    }
}
