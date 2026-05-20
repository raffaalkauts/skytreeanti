package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.system.QuestSystem;
import com.wiredid.skytree.system.QuestSystem.QuestData;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for Quest GUI interactions (Updated)
 */
public class QuestGUIListener implements Listener {

    private final SkytreePlugin plugin;
    private final QuestSystem questSystem;

    public QuestGUIListener(SkytreePlugin plugin, QuestSystem questSystem) {
        this.plugin = plugin;
        this.questSystem = questSystem;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Quests"))
            return;

        if (!(event.getWhoClicked() instanceof Player player))
            return;

        // Don't cancel when clicking in player's own inventory
        if (event.getClickedInventory() == player.getInventory())
            return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        // Recipe Browser
        if (clicked.getType() == Material.BOOK) {
            String name = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());
            if (name != null && name.contains("Recipe Browser")) {
                plugin.getRecipeBrowserGUI().open(player);
                return;
            }
        }

        // Quest Selection
        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName())
            return;
        String name = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());

        for (QuestData quest : questSystem.getQuests().values()) {
            if (name.contains(quest.name)) {
                if (questSystem.isCompleted(player.getUniqueId(), quest.id)) {
                    player.sendMessage("§a§l[Quest] §7You have already completed this quest!");
                    return;
                }

                if (!questSystem.canStart(player.getUniqueId(), quest)) {
                    player.sendMessage("§c§l[Quest] §cThis quest is locked! Complete previous quests first.");
                    return;
                }

                if (questSystem.getActiveQuest(player.getUniqueId()) != null) {
                    player.sendMessage("§c§l[Quest] §cYou already have an active quest!");
                    return;
                }

                questSystem.startQuest(player, quest.id);
                player.sendMessage("§a§l[Quest] §aQuest started: §e" + quest.name);
                player.closeInventory();
                return;
            }
        }
    }
}
