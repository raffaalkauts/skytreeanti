package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.MinionService;
import com.wiredid.skytree.model.MinionData;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class MinionStorageGUI implements Listener {

    private final MinionService minionService;

    public MinionStorageGUI(SkytreePlugin plugin, MinionService minionService) {
        this.minionService = minionService;
    }

    public void open(Player player, UUID minionId) {
        MinionData data = minionService.getMinionData(minionId);
        if (data == null)
            return;

        Inventory inv = Bukkit.createInventory(null, 54, ComponentUtil.parse("§6§lMinion Storage"));

        // Premium Border
        GuiUtil.applyPremiumBorder(inv, Material.ORANGE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE);

        // Minion Storage Slots
        List<ItemStack> storage = data.getStorage();
        for (int i = 0; i < 27; i++) {
            int slot = 10 + (i % 9) + (i / 9) * 9;
            if (i < storage.size()) {
                inv.setItem(slot, storage.get(i));
            } else {
                inv.setItem(slot, new ItemStack(Material.AIR));
            }
        }

        // Collect All Button
        ItemStack collect = new ItemStack(Material.CHEST_MINECART);
        ItemMeta meta = collect.getItemMeta();
        meta.displayName(ComponentUtil.parse("§a§lCollect All"));
        meta.lore(ComponentUtil.parseList("§7Click to move all items to inventory", "§7", "§8ID: " + minionId));
        collect.setItemMeta(meta);
        inv.setItem(49, collect);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(ComponentUtil.parse("§6§lMinion Storage")))
            return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        Player player = (Player) event.getWhoClicked();

        if (event.getRawSlot() == 49) {
            // Find ID from lore or use a better way (Holder?)
            // For now, let's assume we use a holder for better safety
            // but I'll check the lore for now to save time on holder creation.
            String idStr = ComponentUtil.toLegacy(item.getItemMeta().lore().get(2)).replace("§8ID: ", "");
            UUID minionId = UUID.fromString(idStr);

            List<ItemStack> items = minionService.clearStorage(minionId);
            for (ItemStack stack : items) {
                if (stack == null || stack.getType() == Material.AIR)
                    continue;
                player.getInventory().addItem(stack).values()
                        .forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));
            }

            player.sendMessage("§a[Minion] §fAll items collected!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            player.closeInventory();
        }
    }
}
