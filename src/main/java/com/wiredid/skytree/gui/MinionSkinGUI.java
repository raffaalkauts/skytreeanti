package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.MinionService;
import com.wiredid.skytree.model.MinionSkin;
import com.wiredid.skytree.model.MinionData;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MinionSkinGUI implements Listener {

    private final SkytreePlugin plugin;
    private final MinionService minionService;
    private final NamespacedKey SKIN_KEY;
    private final NamespacedKey MINION_ID_KEY;

    public MinionSkinGUI(SkytreePlugin plugin, MinionService minionService) {
        this.plugin = plugin;
        this.minionService = minionService;
        this.SKIN_KEY = new NamespacedKey(plugin, "minion_skin");
        this.MINION_ID_KEY = new NamespacedKey(plugin, "minion_id");
    }

    public void open(Player player, UUID minionId) {
        MinionData data = minionService.getMinionData(minionId);
        if (data == null)
            return;

        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§b§lMinion Skins"));
        GuiUtil.applyPremiumBorder(gui, Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE);

        int[] slots = { 10, 11, 12, 13, 14, 15, 16 };
        MinionSkin[] skins = MinionSkin.values();

        for (int i = 0; i < Math.min(skins.length, slots.length); i++) {
            MinionSkin skin = skins[i];
            gui.setItem(slots[i], createSkinItem(player, data, skin));
        }

        player.openInventory(gui);
    }

    private ItemStack createSkinItem(Player player, MinionData data, MinionSkin skin) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD); // Simplified, could use actual skin texture
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§e" + skin.name()));

        List<String> lore = new ArrayList<>();
        lore.add("§7Description: §fCustom visual appearance");
        lore.add("");

        boolean hasUnlocked = minionService.hasSkinUnlocked(player.getUniqueId(), skin);
        boolean isActive = data.getSkin() == skin;

        if (isActive) {
            lore.add("§a§lALREADY ACTIVE");
        } else if (hasUnlocked) {
            lore.add("§eClick to EQUIPS");
        } else {
            double cost = 50000; // Placeholder cost
            lore.add("§cLocked");
            lore.add("§7Cost: §e₮" + NumberUtil.formatCurrency(cost));
            lore.add("");
            lore.add("§eClick to UNLOCK");
        }

        meta.lore(ComponentUtil.parseList(lore));
        meta.getPersistentDataContainer().set(SKIN_KEY, PersistentDataType.STRING, skin.name());
        meta.getPersistentDataContainer().set(MINION_ID_KEY, PersistentDataType.STRING, data.getMinionId().toString());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!ComponentUtil.stripColor(event.getView().title()).equals("Minion Skins"))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta())
            return;

        String skinName = clicked.getItemMeta().getPersistentDataContainer().get(SKIN_KEY, PersistentDataType.STRING);
        String minionIdStr = clicked.getItemMeta().getPersistentDataContainer().get(MINION_ID_KEY,
                PersistentDataType.STRING);

        if (skinName == null || minionIdStr == null)
            return;

        MinionSkin skin = MinionSkin.valueOf(skinName);
        UUID minionId = UUID.fromString(minionIdStr);
        MinionData data = minionService.getMinionData(minionId);

        if (data == null)
            return;

        if (minionService.hasSkinUnlocked(player.getUniqueId(), skin)) {
            // Equips
            data.setSkin(skin);
            minionService.saveMinionData(data);
            player.sendMessage("§aMinion skin updated to " + skin.name() + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1.2f);
            player.closeInventory();
        } else {
            // Unlock/Buy
            double cost = 50000; // Placeholder
            if (plugin.getEconomyService().getBalance(player.getUniqueId()) >= cost) {
                plugin.getEconomyService().removeBalance(player.getUniqueId(), cost);
                minionService.unlockSkin(player.getUniqueId(), skin);
                player.sendMessage("§aUnlocked §e" + skin.name() + " §askin!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                open(player, minionId); // Refresh
            } else {
                player.sendMessage("§cInsufficient funds to unlock this skin!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }
}
