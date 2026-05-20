package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.IslandSettingsService;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class IslandSettingsGUI implements Listener {

    private static final class IslandSettingsHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final SkytreePlugin plugin;
    private final NamespacedKey settingKey;

    public IslandSettingsGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.settingKey = new NamespacedKey(plugin, "setting_key");
    }

    public void open(Player player) {
        Optional<Island> optIsland = plugin.getIslandService().getIsland(player.getUniqueId());
        if (optIsland.isEmpty()) {
            return;
        }

        Island island = optIsland.get();
        UUID owner = island.getOwnerUUID();
        if (owner == null || !owner.equals(player.getUniqueId())) {
            player.sendMessage("§cOnly the island owner can change settings!");
            return;
        }

        Inventory inv = Bukkit.createInventory(new IslandSettingsHolder(), 45,
                ComponentUtil.parse("§8» §a§lIsland Settings"));
        IslandSettingsService service = plugin.getIslandSettingsService();

        inv.setItem(10, createToggle(service, island, "PvP", "pvp", Material.DIAMOND_SWORD,
                "Allow players to fight each other."));
        inv.setItem(11, createToggle(service, island, "Mob Spawning", "mob_spawning", Material.ZOMBIE_HEAD,
                "Allow monsters to spawn naturally."));
        inv.setItem(12, createToggle(service, island, "Fire Spread", "fire_spread", Material.FLINT_AND_STEEL,
                "Allow fire to burn blocks."));
        inv.setItem(13, createToggle(service, island, "TNT Damage", "tnt", Material.TNT,
                "Allow explosions to destroy blocks."));
        inv.setItem(14, createToggle(service, island, "Leaf Decay", "leaf_decay", Material.OAK_LEAVES,
                "Allow leaves to decay naturally."));
        inv.setItem(15, createToggle(service, island, "Visitor Entry", "visitor_entry", Material.OAK_DOOR,
                "Allow visitors to warp to your island."));
        inv.setItem(16, createToggle(service, island, "Piston Push", "pistons", Material.PISTON,
                "Allow pistons to push blocks."));
        inv.setItem(19, createToggle(service, island, "Mob Stacking", "mob_stacking", Material.EGG,
                "Toggle whether mobs stack into one entity."));
        inv.setItem(20, createToggle(service, island, "Spawner Stacking", "spawner_stacking", Material.SPAWNER,
                "Toggle whether spawners stack when placed."));
        inv.setItem(21, createToggle(service, island, "Drop Protection", "drop_protection",
                Material.IRON_CHESTPLATE, "Prevent visitors from picking up dropped items."));

        inv.setItem(36, createButton(Material.ARROW, "§cBack", "BACK"));

        GuiUtil.applyPremiumBorder(inv, Material.GRAY_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE);
        player.openInventory(inv);
    }

    private ItemStack createToggle(IslandSettingsService service, Island island, String name, String key,
            Material mat, String desc) {
        boolean enabled = service.getSetting(island, key);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        String color = enabled ? "§a" : "§c";
        String status = enabled ? "ENABLED" : "DISABLED";

        meta.displayName(ComponentUtil.parse(color + "§l" + name));
        List<String> lore = new ArrayList<>();
        lore.add("§7" + desc);
        lore.add("");
        lore.add("§7Status: " + color + "§l" + status);
        lore.add("");
        lore.add("§eClick to toggle!");
        meta.lore(ComponentUtil.parseList(lore));
        meta.getPersistentDataContainer().set(settingKey, PersistentDataType.STRING, key);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButton(Material mat, String name, String key) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.getPersistentDataContainer().set(settingKey, PersistentDataType.STRING, key);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof IslandSettingsHolder)) {
            return;
        }

        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        String key = clicked.getItemMeta().getPersistentDataContainer().get(settingKey, PersistentDataType.STRING);
        if (key == null) {
            return;
        }

        if ("BACK".equals(key)) {
            plugin.getSettingsMainGUI().open(player);
            return;
        }

        Optional<Island> optIsland = plugin.getIslandService().getIsland(player.getUniqueId());
        if (optIsland.isPresent()) {
            Island island = optIsland.get();
            boolean current = plugin.getIslandSettingsService().getSetting(island, key);
            plugin.getIslandSettingsService().setSetting(island, key, !current);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            open(player);
        }
    }
}
