package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.MinionService;
import com.wiredid.skytree.model.MinionData;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MinionGUI {

    private final MinionService minionService;

    public MinionGUI(SkytreePlugin plugin, MinionService minionService) {
        this.minionService = minionService;
    }

    public void open(Player player, UUID minionId) {
        MinionData data = minionService.getMinionData(minionId);
        if (data == null)
            return;

        Inventory gui = Bukkit.createInventory(new MinionInventoryHolder(minionId), 54,
                ComponentUtil.parse("§6§lMinion Menu"));

        // Premium Border
        GuiUtil.applyPremiumBorder(gui, Material.ORANGE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE);

        // Status / Info Item
        ItemStack info = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(ComponentUtil.parse("§6§l" + data.getType().name() + " Minion"));
        List<String> lore = new ArrayList<>();
        lore.add("§7Level: §e" + data.getLevel());
        lore.add("§7Skin: §e" + data.getSkin().name());
        lore.add("§7Speed: §b" + String.format("%.1f", data.getEffectiveSpeed()) + "x");
        lore.add("§7Range: §b" + data.getEffectiveRange() + " blocks");
        lore.add("");
        lore.add("§7Status: " + (data.isActive() ? "§aActive" : "§cIdle"));
        infoMeta.lore(ComponentUtil.parseList(lore));
        info.setItemMeta(infoMeta);
        gui.setItem(13, info);

        // Storage Button
        ItemStack storage = new ItemStack(Material.CHEST);
        ItemMeta storageMeta = storage.getItemMeta();
        storageMeta.displayName(ComponentUtil.parse("§a§lInternal Storage"));
        storageMeta.lore(ComponentUtil.parseList("§7Items collected: §e" + data.getStorage().size(), "",
                "§eClick to view/collect"));
        storage.setItemMeta(storageMeta);
        gui.setItem(29, storage);

        // Upgrade Button
        ItemStack upgrade = new ItemStack(Material.NETHER_STAR);
        ItemMeta upgradeMeta = upgrade.getItemMeta();
        upgradeMeta.displayName(ComponentUtil.parse("§d§lUpgrade Minion"));
        double cost = 10000 * Math.pow(2, data.getLevel());
        upgradeMeta.lore(ComponentUtil.parseList("§7Next Level: §e" + (data.getLevel() + 1),
                "§7Cost: §e₮" + String.format("%.0f", cost), "", "§eClick to upgrade!"));
        upgrade.setItemMeta(upgradeMeta);
        gui.setItem(31, upgrade);

        // Skins Button
        ItemStack skins = new ItemStack(Material.PAINTING);
        ItemMeta skinsMeta = skins.getItemMeta();
        skinsMeta.displayName(ComponentUtil.parse("§b§lSwitch Skin"));
        skinsMeta.lore(ComponentUtil.parseList("§7Change how your minion looks", "", "§eClick to browse skins"));
        skins.setItemMeta(skinsMeta);
        gui.setItem(33, skins);

        // Pickup Button (Remove minion)
        ItemStack pickup = new ItemStack(Material.BARRIER);
        ItemMeta pickupMeta = pickup.getItemMeta();
        pickupMeta.displayName(ComponentUtil.parse("§c§lPickup Minion"));
        pickupMeta.lore(ComponentUtil.parseList("§7Remove the minion and put it", "§7back into your inventory."));
        pickup.setItemMeta(pickupMeta);
        gui.setItem(49, pickup);

        player.openInventory(gui);
    }
}
