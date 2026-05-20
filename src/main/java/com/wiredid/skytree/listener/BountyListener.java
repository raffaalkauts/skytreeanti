package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.BountyService;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.model.Bounty;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Optional;

public class BountyListener implements Listener {
    private final BountyService bountyService;
    private final EconomyService economyService;

    public BountyListener(SkytreePlugin plugin) {
        this.bountyService = plugin.getBountyService();
        this.economyService = plugin.getEconomyService();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        Optional<Bounty> bountyOpt = bountyService.getBounty(victim.getUniqueId());

        if (bountyOpt.isPresent()) {
            Bounty bounty = bountyOpt.get();
            if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
                // Claim bounty
                double amount = bounty.getAmount();
                economyService.addBalance(killer.getUniqueId(), amount);
                bountyService.removeBounty(victim.getUniqueId());

                Bukkit.broadcast(
                        ComponentUtil.parse("§6§lBOUNTY CLAIMED! §e" + killer.getName() + " §7has claimed the §a$"
                                + NumberUtil.formatCurrency(amount) + " §7bounty on §c" + victim.getName() + "§7!"));
                killer.sendMessage(
                        ComponentUtil.parse("§aYou claimed a bounty of §2$" + NumberUtil.formatCurrency(amount) + "§a!"));
            } else {
                // Victim died but not by another player (fall damage, etc.)
                // Do we keep the bounty or remove it?
                // Standard practice: Keep it active until killed by a player.
                // So we do nothing.
            }
        }
    }
}
