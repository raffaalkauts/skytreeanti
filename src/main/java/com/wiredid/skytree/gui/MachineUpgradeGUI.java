package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.machine.MachineProcessor;
import com.wiredid.skytree.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class MachineUpgradeGUI implements Listener {

    private final SkytreePlugin plugin;
    private final MachineProcessor machineProcessor;
    private final NamespacedKey locKey;

    public MachineUpgradeGUI(SkytreePlugin plugin, MachineProcessor machineProcessor) {
        this.plugin = plugin;
        this.machineProcessor = machineProcessor;
        this.locKey = new NamespacedKey(plugin, "machine_loc");
    }

    public void open(Player player, Location loc) {
        MachineProcessor.MachineData data = machineProcessor.getMachineData(loc);
        if (data == null)
            return;

        Inventory inv = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lMachine Upgrade"));

        // Fill background
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        gMeta.displayName(ComponentUtil.parse(" "));
        glass.setItemMeta(gMeta);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, glass);

        // Machine Info
        ItemStack info = new ItemStack(Material.DISPENSER);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(ComponentUtil.parse("§eMachine: §f" + data.getType().name()));
        List<Component> iLore = new ArrayList<>();
        iLore.add(ComponentUtil.parse("§7Current Tier: §b" + data.getTier()));

        MachineProcessor.TierConfig tc = machineProcessor.getTierConfig(data.getTier());
        iLore.add(ComponentUtil.parse("§7- Speed: §a" + (tc.speed * 100) + "%"));
        iLore.add(ComponentUtil.parse("§7- Efficiency: §a" + (tc.efficiency * 100) + "%"));

        iMeta.lore(iLore);
        info.setItemMeta(iMeta);
        inv.setItem(10, info);

        // Upgrade Button
        if (data.getTier() < 3) {
            int nextTier = data.getTier() + 1;
            // Get cost from config (hardcoded for now or from upgrade file if loaded)
            double cost = nextTier == 2 ? 50000 : 200000;

            ItemStack upgrade = new ItemStack(Material.NETHER_STAR);
            ItemMeta uMeta = upgrade.getItemMeta();
            uMeta.displayName(ComponentUtil.parse("§a§lUpgrade to Tier " + nextTier));
            List<Component> uLore = new ArrayList<>();
            uLore.add(ComponentUtil.parse("§7Cost: §e" + cost + " USDT"));

            MachineProcessor.TierConfig ntc = machineProcessor.getTierConfig(nextTier);
            uLore.add(ComponentUtil.parse("§7Next Stats:"));
            uLore.add(ComponentUtil.parse("§7- Speed: §a" + (ntc.speed * 100) + "%"));
            uLore.add(ComponentUtil.parse("§7- Efficiency: §a" + (ntc.efficiency * 100) + "%"));

            uLore.add(ComponentUtil.parse(""));
            uLore.add(ComponentUtil.parse("§eClick to upgrade!"));
            uMeta.lore(uLore);

            // Store location in metadata
            uMeta.getPersistentDataContainer().set(locKey, PersistentDataType.STRING, serializeLoc(loc));

            upgrade.setItemMeta(uMeta);
            inv.setItem(13, upgrade);
        } else {
            ItemStack capped = new ItemStack(Material.BARRIER);
            ItemMeta cMeta = capped.getItemMeta();
            cMeta.displayName(ComponentUtil.parse("§c§lMaximum Tier Reached"));
            capped.setItemMeta(cMeta);
            inv.setItem(13, capped);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(ComponentUtil.parse("§6§lMachine Upgrade")))
            return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        Player player = (Player) event.getWhoClicked();
        if (item.getType() == Material.NETHER_STAR) {
            String locStr = item.getItemMeta().getPersistentDataContainer().get(locKey, PersistentDataType.STRING);
            Location loc = deserializeLoc(locStr);
            if (loc == null)
                return;

            MachineProcessor.MachineData data = machineProcessor.getMachineData(loc);
            if (data == null)
                return;

            int nextTier = data.getTier() + 1;
            double cost = nextTier == 2 ? 50000 : 200000;

            if (plugin.getEconomyService().getBalance(player.getUniqueId()) >= cost) {
                plugin.getEconomyService().removeBalance(player.getUniqueId(), cost);
                machineProcessor.upgradeMachine(loc);
                player.sendMessage("§a[Upgrade] §fMachine upgraded to Tier " + nextTier + "!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                open(player, loc); // Refresh
            } else {
                player.sendMessage("§c[Upgrade] §fNot enough USDT! Need " + cost);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    private String serializeLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location deserializeLoc(String str) {
        if (str == null)
            return null;
        String[] parts = str.split(",");
        return new Location(Bukkit.getWorld(parts[0]),
                Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
}
