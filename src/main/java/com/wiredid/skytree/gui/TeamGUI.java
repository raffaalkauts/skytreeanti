package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.model.IslandMember;
import com.wiredid.skytree.model.IslandRole;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamGUI implements Listener {

    private static final class TeamHolder implements InventoryHolder {
        private final UUID islandId;

        private TeamHolder(UUID islandId) {
            this.islandId = islandId;
        }

        public UUID getIslandId() {
            return islandId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class MemberActionsHolder implements InventoryHolder {
        private final UUID targetUuid;

        private MemberActionsHolder(UUID targetUuid) {
            this.targetUuid = targetUuid;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final SkytreePlugin plugin;
    private final NamespacedKey memberUuidKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey navigationKey;

    public TeamGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.memberUuidKey = new NamespacedKey(plugin, "member_uuid");
        this.actionKey = new NamespacedKey(plugin, "team_action");
        this.navigationKey = new NamespacedKey(plugin, "nav_action");
    }

    public void open(Player player, Island island) {
        Inventory gui = Bukkit.createInventory(new TeamHolder(island.getIslandId()), 54,
                ComponentUtil.parse("§6§lIsland §8» §7Team"));

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(ComponentUtil.parse("§e§lIsland Team"));
        List<String> infoLore = new ArrayList<>();
        int maxMembers = 10 + (island.getUpgrades().getOrDefault("members", 0) * 5);
        infoLore.add("§7Members: §f" + (island.getMembers().size() + 1) + "/" + maxMembers);
        infoLore.add("§7Owner: §f" + Bukkit.getOfflinePlayer(island.getOwnerUUID()).getName());
        infoLore.add("");
        infoLore.add("§7Click on a member to manage them.");
        infoMeta.lore(ComponentUtil.parseList(infoLore));
        info.setItemMeta(infoMeta);
        gui.setItem(10, info);

        gui.setItem(13, createPlayerHead(island.getOwnerUUID(), IslandRole.OWNER));

        int[] slots = { 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43 };
        int i = 0;
        for (IslandMember member : island.getMembers()) {
            if (i >= slots.length) {
                break;
            }
            gui.setItem(slots[i++], createPlayerHead(member.getUuid(), member.getRole()));
        }

        if (island.getMembers().size() + 1 < maxMembers) {
            ItemStack invite = new ItemStack(Material.OAK_SIGN);
            ItemMeta invMeta = invite.getItemMeta();
            invMeta.displayName(ComponentUtil.parse("§a§lInvite Player"));
            invMeta.lore(ComponentUtil.parseList(List.of(
                    "§7Use this island command to invite someone.",
                    "§7Type the player name in chat.")));
            invMeta.getPersistentDataContainer().set(navigationKey, PersistentDataType.STRING, "invite");
            invite.setItemMeta(invMeta);
            gui.setItem(16, invite);
        } else {
            ItemStack full = new ItemStack(Material.BARRIER);
            ItemMeta fullMeta = full.getItemMeta();
            fullMeta.displayName(ComponentUtil.parse("§c§lIsland Full"));
            full.setItemMeta(fullMeta);
            gui.setItem(16, full);
        }

        ItemStack trust = new ItemStack(Material.GOLD_INGOT);
        ItemMeta trustMeta = trust.getItemMeta();
        trustMeta.displayName(ComponentUtil.parse("§6§lTrusted Players"));
        trustMeta.lore(ComponentUtil.parseList(List.of("§7Manage players with custom", "§7permissions on your island.")));
        trustMeta.getPersistentDataContainer().set(navigationKey, PersistentDataType.STRING, "trusted");
        trust.setItemMeta(trustMeta);
        gui.setItem(20, trust);

        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.LIME_STAINED_GLASS_PANE,
                Material.GREEN_STAINED_GLASS_PANE);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(ComponentUtil.parse("§cBack to Menu"));
        backMeta.getPersistentDataContainer().set(navigationKey, PersistentDataType.STRING, "back");
        back.setItemMeta(backMeta);
        gui.setItem(24, back);

        player.openInventory(gui);
    }

    private ItemStack createPlayerHead(UUID uuid, IslandRole role) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (uuid == null) return head;
        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
        meta.setOwningPlayer(op);

        IslandRole safeRole = role != null ? role : IslandRole.MEMBER;
        String roleColor = switch (safeRole) {
            case OWNER -> "§c";
            case CO_OWNER -> "§6";
            case MEMBER -> "§a";
            case COOP -> "§e";
            default -> "§7";
        };

        meta.displayName(ComponentUtil.parse(roleColor + "§l" + (op.getName() != null ? op.getName() : "Unknown")));
        meta.lore(ComponentUtil.parseList(List.of(
                "§7Role: " + roleColor + safeRole.name(),
                "",
                safeRole == IslandRole.OWNER ? "§7The Island Owner" : "§eClick to manage member")));
        meta.getPersistentDataContainer().set(memberUuidKey, org.bukkit.persistence.PersistentDataType.STRING, uuid.toString());

        head.setItemMeta(meta);
        return head;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof TeamHolder teamHolder) {
            handleTeamViewClick(event, teamHolder);
            return;
        }
        if (top.getHolder() instanceof MemberActionsHolder actionsHolder) {
            handleMemberActionsClick(event, actionsHolder);
        }
    }

    private void handleTeamViewClick(InventoryClickEvent event, TeamHolder teamHolder) {
        event.setCancelled(true);
        UUID islandId = teamHolder.getIslandId();
        if (islandId == null) return;

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }

        String nav = item.getItemMeta().getPersistentDataContainer()
                .get(navigationKey, PersistentDataType.STRING);
        if (nav != null) {
            switch (nav) {
                case "back" -> {
                    plugin.getIslandMenuGUI().openMenu(player);
                    return;
                }
                case "invite" -> {
                    player.closeInventory();
                    player.sendMessage("§a§l[Skytree] §aType §e/is invite <player> §ato invite someone!");
                    return;
                }
                case "trusted" -> {
                    plugin.getIslandService().getIsland(player.getUniqueId())
                            .ifPresent(island -> plugin.getTrustGUI().open(player, island));
                    return;
                }
            }
        }

        if (item.getType() == Material.PLAYER_HEAD) {
            String uuidStr = item.getItemMeta().getPersistentDataContainer().get(memberUuidKey,
                    PersistentDataType.STRING);
            if (uuidStr == null) {
                return;
            }
            try {
                UUID targetUUID = UUID.fromString(uuidStr);
                if (targetUUID.equals(player.getUniqueId())) {
                    return;
                }
                openMemberActions(player, targetUUID);
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cError: Invalid member data.");
            }
        }
    }

    private void openMemberActions(Player player, UUID targetUUID) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        Inventory gui = Bukkit.createInventory(new MemberActionsHolder(targetUUID), 27,
                ComponentUtil.parse("§6§lIsland §8» §7Member Actions"));

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(ComponentUtil.parse("§b§l" + (target.getName() != null ? target.getName() : "Unknown")));
        meta.lore(ComponentUtil.parseList(List.of("§7Use the buttons below to manage this member.")));
        head.setItemMeta(meta);
        gui.setItem(13, head);

        gui.setItem(10, createActionButton(Material.BARRIER, "§cKick Member", "KICK"));
        gui.setItem(12, createActionButton(Material.GOLD_INGOT, "§6Promote Member", "PROMOTE"));
        gui.setItem(14, createActionButton(Material.IRON_INGOT, "§eDemote Member", "DEMOTE"));
        gui.setItem(16, createActionButton(Material.ARROW, "§7Back", "BACK"));

        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.LIME_STAINED_GLASS_PANE,
                Material.GREEN_STAINED_GLASS_PANE);
        player.openInventory(gui);
    }

    private ItemStack createActionButton(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.lore(ComponentUtil.parseList(List.of("§7Click to select.", "", "§8Action: §f" + action)));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private void handleMemberActionsClick(InventoryClickEvent event, MemberActionsHolder holder) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }

        String action = item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) {
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(holder.targetUuid);
        String targetName = target.getName() != null ? target.getName() : holder.targetUuid.toString();

        switch (action) {
            case "BACK" -> {
                player.closeInventory();
                plugin.getIslandService().getIsland(player.getUniqueId())
                        .ifPresent(island -> open(player, island));
            }
            case "KICK" -> {
                player.closeInventory();
                player.performCommand("is team kick " + targetName);
            }
            case "PROMOTE" -> {
                player.closeInventory();
                player.performCommand("is team promote " + targetName);
            }
            case "DEMOTE" -> {
                player.closeInventory();
                player.performCommand("is team demote " + targetName);
            }
            default -> {
            }
        }
    }
}
