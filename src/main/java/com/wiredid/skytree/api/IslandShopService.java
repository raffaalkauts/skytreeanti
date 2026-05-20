package com.wiredid.skytree.api;

import com.wiredid.skytree.model.IslandShop;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.Collection;
import java.util.Optional;

public interface IslandShopService {
    void createShop(Player owner, Block chest, double buyPrice, double sellPrice);

    void removeShop(Block chest);

    Optional<IslandShop> getShop(Block block);

    Collection<IslandShop> getAllShops();

    void saveShops();

    void loadShops();
}
