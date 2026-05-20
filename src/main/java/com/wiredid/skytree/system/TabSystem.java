package com.wiredid.skytree.system;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class TabSystem implements Listener {

    private final SkytreePlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public TabSystem(SkytreePlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        startUpdateTask();
    }

    private void loadConfig() {
        this.configFile = new File(plugin.getDataFolder(), "tab.yml");
        if (!configFile.exists()) {
            plugin.saveResource("tab.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        loadConfig();
    }

    private void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateTab(player);
            }
        }, 20L, 100L); // Update every 5 seconds
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updateTab(event.getPlayer());
    }

    private void updateTab(Player player) {
        // 1. Header & Footer from Config
        // 1. Header & Footer from Config
        String headerStr = config.getString("header", "\n §b§lSKYTREE NETWORK \n §7Premium Skyblock Experience \n");
        String footerStr = config.getString("footer",
                "\n §fOnline: §a%online% §7| §fPing: §a%ping%ms \n §fRank: %rank% §7| §fBalance: §e%balance% \n\n §7Store: §bstore.skytreenetwork.net \n");

        // Replace placeholders
        headerStr = replacePlaceholders(headerStr, player);
        footerStr = replacePlaceholders(footerStr, player);

        Component header = ComponentUtil.smartParse(headerStr);
        Component footer = ComponentUtil.smartParse(footerStr);

        player.sendPlayerListHeaderAndFooter(header, footer);

        // 2. Player Name (Name + Island)
        // 2. Player Name (Rank + Name + Island)
        String prefix = plugin.getRankService().getPrefix(player.getUniqueId());

        String islandLevel = "0";
        var islandOpt = plugin.getIslandService().getIsland(player.getUniqueId());
        if (islandOpt.isPresent()) {
            islandLevel = String.valueOf(islandOpt.get().getLevel());
        }

        // Format: [Rank] Name [Lvl X]
        // RankService prefix usually includes colors and brackets, e.g. "§c[Admin]"
        Component name = ComponentUtil
                .parse(prefix + " " + "§f" + player.getName() + " §8[§bLvl " + islandLevel + "§8]");
        player.playerListName(name);

        // Note: playerListName() already handles display and sorting via Component
        // formatting
        // The old setPlayerListName() is deprecated and not needed
    }

    private String replacePlaceholders(String text, Player player) {
        if (text == null)
            return "";
        double balance = plugin.getEconomyService().getBalance(player.getUniqueId());
        String rankPrefix = plugin.getRankService().getPrefix(player.getUniqueId());

        return text.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%ping%", String.valueOf(player.getPing()))
                .replace("%player%", player.getName())
                .replace("%rank%", rankPrefix)
                .replace("%balance%", String.format("%,.0f USDT", balance));
    }
}
