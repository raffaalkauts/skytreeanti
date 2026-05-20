package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Rank;
import com.wiredid.skytree.util.ComponentUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {

    private static final String VIEW_CMD = "/stview";
    private static final String SHOWCASE_TITLE_TOKEN = "[Showcase]";
    private final SkytreePlugin plugin;
    private final Map<String, ShowcaseEntry> showcases = new ConcurrentHashMap<>();

    public ChatListener(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        if (player.hasMetadata("shop_search_context")) return;

        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        plainMessage = plugin.getChatService().filterProfanity(plainMessage);
        plugin.getChatService().handleMentions(player, plainMessage);

        String channel = getChannel(player);
        if (plainMessage.startsWith("!")) {
            channel = "GLOBAL";
            plainMessage = plainMessage.substring(1).trim();
            if (plainMessage.isEmpty()) return;
        } else if (plainMessage.startsWith("@")) {
            channel = "ISLAND";
            plainMessage = plainMessage.substring(1).trim();
            if (plainMessage.isEmpty()) return;
        }

        Component renderedMessage = renderShowcasePlaceholders(player, plainMessage);
        event.message(renderedMessage);

        String islandLvl = "0";
        var island = plugin.getIslandService().getIsland(player.getUniqueId());
        if (island.isPresent()) islandLvl = String.valueOf(island.get().getLevel());

        String rank = "Player";
        if (player.isOp()) rank = "§cAdmin";
        String prefixFormat = "§8[§b" + islandLvl + "§8] §7[" + rank + "§7] §f" + player.getName();

        if ("ISLAND".equals(channel)) {
            event.setCancelled(true);
            if (island.isEmpty()) {
                player.sendMessage("§cYou must have an island to use Island Chat!");
                setChannel(player, "GLOBAL");
                return;
            }
            sendIslandMessage(player, island.get().getIslandId(), prefixFormat, renderedMessage);
            return;
        }

        if ("LOCAL".equals(channel)) {
            event.setCancelled(true);
            sendLocalMessage(player, prefixFormat, renderedMessage);
            return;
        }

        Rank playerRank = plugin.getRankService().getRank(player.getUniqueId());
        String rankPrefix = playerRank.getPrefix();
        String tag = plugin.getTagService().getActiveTagDisplay(player);
        String finalTag = (tag != null) ? tag + " " : "";
        final String finalIslandLvl = islandLvl;

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component formatted = ComponentUtil.parse("§8[§b" + finalIslandLvl + "§8] ")
                    .append(ComponentUtil.parse(finalTag))
                    .append(ComponentUtil.parse(rankPrefix + " "))
                    .append(sourceDisplayName)
                    .append(ComponentUtil.parse(" §8» §f"))
                    .append(message);

            plugin.getChatService().logMessage(source, formatted);
            return formatted;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onViewCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null) return;
        if (!raw.toLowerCase().startsWith(VIEW_CMD + " ")) return;

        event.setCancelled(true);
        String[] split = raw.split("\\s+", 2);
        if (split.length < 2) return;

        String token = split[1].trim();
        ShowcaseEntry entry = showcases.get(token);
        if (entry == null) {
            event.getPlayer().sendMessage("§cShowcase expired.");
            return;
        }
        openShowcase(event.getPlayer(), entry);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShowcaseClick(InventoryClickEvent event) {
        String plainTitle = ComponentUtil.stripColor(event.getView().title());
        if (plainTitle.contains(SHOWCASE_TITLE_TOKEN)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShowcaseDrag(InventoryDragEvent event) {
        String plainTitle = ComponentUtil.stripColor(event.getView().title());
        if (plainTitle.contains(SHOWCASE_TITLE_TOKEN)) {
            event.setCancelled(true);
        }
    }

    private Component renderShowcasePlaceholders(Player player, String message) {
        Component root = Component.empty();
        int cursor = 0;
        String lowered = message.toLowerCase();

        while (cursor < message.length()) {
            int next = findNextPlaceholder(lowered, cursor);
            if (next < 0) {
                root = root.append(ComponentUtil.parse(message.substring(cursor)));
                break;
            }

            if (next > cursor) {
                root = root.append(ComponentUtil.parse(message.substring(cursor, next)));
            }

            if (lowered.startsWith("[inv]", next) || lowered.startsWith("[inventory]", next)) {
                root = root.append(buildInvToken(player));
                cursor = next + (lowered.startsWith("[inventory]", next) ? "[inventory]".length() : "[inv]".length());
                continue;
            }

            root = root.append(buildItemToken(player));
            cursor = next + (lowered.startsWith("[item]", next) ? "[item]".length() : "[i]".length());
        }

        return root;
    }

    private int findNextPlaceholder(String lowered, int from) {
        int i = lowered.indexOf("[i]", from);
        int item = lowered.indexOf("[item]", from);
        int inv = lowered.indexOf("[inv]", from);
        int inventory = lowered.indexOf("[inventory]", from);
        return Arrays.stream(new int[]{i, item, inv, inventory}).filter(v -> v >= 0).min().orElse(-1);
    }

    private Component buildItemToken(Player player) {
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType() == Material.AIR) {
            return ComponentUtil.parse("§8[§7No Item§8]");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        showcases.put(token, ShowcaseEntry.forItem(player.getUniqueId(), inHand.clone()));

        String itemName = getDisplayName(inHand);
        return ComponentUtil.parse("§8[§bItem§8] §f" + itemName)
                .clickEvent(ClickEvent.runCommand(VIEW_CMD + " " + token))
                .hoverEvent(HoverEvent.showText(ComponentUtil.parse("§eClick to inspect item")));
    }

    private Component buildInvToken(Player player) {
        String token = UUID.randomUUID().toString().replace("-", "");
        showcases.put(token, ShowcaseEntry.forInventory(player.getUniqueId(), snapshotInventory(player.getInventory())));

        return ComponentUtil.parse("§8[§dInventory§8]")
                .clickEvent(ClickEvent.runCommand(VIEW_CMD + " " + token))
                .hoverEvent(HoverEvent.showText(ComponentUtil.parse("§eClick to inspect inventory")));
    }

    private ItemStack[] snapshotInventory(PlayerInventory inv) {
        ItemStack[] snap = new ItemStack[41];
        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < Math.min(contents.length, 36); i++) {
            snap[i] = contents[i] == null ? null : contents[i].clone();
        }
        snap[36] = cloneOrNull(inv.getHelmet());
        snap[37] = cloneOrNull(inv.getChestplate());
        snap[38] = cloneOrNull(inv.getLeggings());
        snap[39] = cloneOrNull(inv.getBoots());
        snap[40] = cloneOrNull(inv.getItemInOffHand());
        return snap;
    }

    private ItemStack cloneOrNull(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private void openShowcase(Player viewer, ShowcaseEntry entry) {
        if (entry.type == ShowcaseType.ITEM) {
            var inv = Bukkit.createInventory(null, 27, ComponentUtil.parse("§8[Showcase] §7Item View"));
            inv.setItem(13, entry.item == null ? null : entry.item.clone());
            viewer.openInventory(inv);
            return;
        }

        var inv = Bukkit.createInventory(null, 54, ComponentUtil.parse("§8[Showcase] §7Inventory View"));
        for (int i = 0; i < 36; i++) {
            if (entry.inventory[i] != null) inv.setItem(i, entry.inventory[i].clone());
        }
        inv.setItem(45, entry.inventory[36] == null ? new ItemStack(Material.GRAY_STAINED_GLASS_PANE) : entry.inventory[36].clone());
        inv.setItem(46, entry.inventory[37] == null ? new ItemStack(Material.GRAY_STAINED_GLASS_PANE) : entry.inventory[37].clone());
        inv.setItem(47, entry.inventory[38] == null ? new ItemStack(Material.GRAY_STAINED_GLASS_PANE) : entry.inventory[38].clone());
        inv.setItem(48, entry.inventory[39] == null ? new ItemStack(Material.GRAY_STAINED_GLASS_PANE) : entry.inventory[39].clone());
        inv.setItem(49, entry.inventory[40] == null ? new ItemStack(Material.GRAY_STAINED_GLASS_PANE) : entry.inventory[40].clone());
        viewer.openInventory(inv);
    }

    private String getDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return ComponentUtil.stripColor(item.getItemMeta().displayName());
        }
        String raw = item.getType().name().toLowerCase().replace("_", " ");
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private void sendIslandMessage(Player sender, UUID islandId, String prefix, Component message) {
        String tag = plugin.getTagService().getActiveTagDisplay(sender);
        String tagPrefix = (tag != null) ? tag + " " : "";
        Component formatted = ComponentUtil.parse("§b[ISLAND] " + tagPrefix + prefix + " §8» §b")
                .append(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            var pIsland = plugin.getIslandService().getIsland(p.getUniqueId());
            if (pIsland.isPresent() && pIsland.get().getIslandId().equals(islandId)) {
                p.sendMessage(formatted);
            }
        }
        Bukkit.getConsoleSender().sendMessage(formatted);
    }

    private void sendLocalMessage(Player sender, String prefix, Component message) {
        String tag = plugin.getTagService().getActiveTagDisplay(sender);
        String tagPrefix = (tag != null) ? tag + " " : "";
        Component formatted = ComponentUtil.parse("§e[LOCAL] " + tagPrefix + prefix + " §8» §e")
                .append(message);
        int radius = 100;
        int count = 0;
        for (Player p : sender.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(sender.getLocation()) <= radius * radius) {
                p.sendMessage(formatted);
                count++;
            }
        }
        if (count <= 1) sender.sendMessage("§7(No one heard you...)");
    }

    public String getChannel(Player player) {
        if (player.hasMetadata("chat_channel")) {
            return player.getMetadata("chat_channel").get(0).asString();
        }
        return "GLOBAL";
    }

    public void setChannel(Player player, String channel) {
        player.setMetadata("chat_channel", new FixedMetadataValue(plugin, channel));
    }

    private enum ShowcaseType {
        ITEM,
        INVENTORY
    }

    private static final class ShowcaseEntry {
        private final ShowcaseType type;
        private final ItemStack item;
        private final ItemStack[] inventory;

        private ShowcaseEntry(ShowcaseType type, ItemStack item, ItemStack[] inventory) {
            this.type = type;
            this.item = item;
            this.inventory = inventory;
        }

        static ShowcaseEntry forItem(UUID owner, ItemStack item) {
            return new ShowcaseEntry(ShowcaseType.ITEM, item, null);
        }

        static ShowcaseEntry forInventory(UUID owner, ItemStack[] inventory) {
            return new ShowcaseEntry(ShowcaseType.INVENTORY, null, inventory);
        }
    }
}
