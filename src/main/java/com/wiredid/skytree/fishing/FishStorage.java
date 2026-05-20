package com.wiredid.skytree.fishing;

import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;
// import com.wiredid.skytree.fishing.NbtUtils;

public class FishStorage {

    // Map<PlayerUUID, List<FishItem>>
    private final Map<UUID, List<ItemStack>> storageMap = new HashMap<>();
    private final File storageFile;

    public FishStorage(File dataFolder) {
        this.storageFile = new File(dataFolder, "fish_storage.yml");
        load();
    }

    public void addFish(UUID uuid, ItemStack fish) {
        storageMap.computeIfAbsent(uuid, k -> new ArrayList<>()).add(fish);
        save();
    }

    public List<ItemStack> getFish(UUID uuid) {
        return storageMap.getOrDefault(uuid, new ArrayList<>());
    }

    public void removeFish(UUID uuid, ItemStack fish) {
        if (storageMap.containsKey(uuid)) {
            storageMap.get(uuid).remove(fish);
            save();
        }
    }

    public void save() {
        // Async save to avoid main thread hang
        org.bukkit.Bukkit.getScheduler()
                .runTaskAsynchronously(org.bukkit.Bukkit.getPluginManager().getPlugin("Skytree"), this::saveNow);
    }

    public void saveNow() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            for (Map.Entry<UUID, List<ItemStack>> entry : storageMap.entrySet()) {
                config.set(entry.getKey().toString(), entry.getValue());
            }
            config.save(storageFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (!storageFile.exists())
            return;
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(storageFile);
            for (String key : config.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                @SuppressWarnings("unchecked")
                List<ItemStack> list = (List<ItemStack>) config.getList(key); // Unchecked cast
                storageMap.put(uuid, list != null ? list : new ArrayList<>());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sorter logic
     */
    public void sort(UUID uuid, SortType type) {
        List<ItemStack> list = getFish(uuid);
        if (list.isEmpty())
            return;

        list.sort((a, b) -> {
            switch (type) {
                case RARITY:
                    // Rarity Enum Ordinal? Need to parse NBT.
                    String rA = NbtUtils.getString(a, NbtUtils.KEY_FISH_RARITY);
                    String rB = NbtUtils.getString(b, NbtUtils.KEY_FISH_RARITY);
                    // Simple string compare or enum lookup?
                    // Let's assume Enum order: COMMON, UNCOMMON, RARE...
                    // We need the Enum.
                    return compareRarity(rA, rB);
                case WEIGHT:
                    double wA = NbtUtils.getDouble(a, NbtUtils.KEY_FISH_WEIGHT);
                    double wB = NbtUtils.getDouble(b, NbtUtils.KEY_FISH_WEIGHT);
                    return Double.compare(wB, wA); // Descending
                case FAVORITE:
                    // Check NBT for favorite?
                    boolean fA = NbtUtils.getBoolean(a, "IsFavorite");
                    boolean fB = NbtUtils.getBoolean(b, "IsFavorite");
                    return Boolean.compare(fB, fA); // True first
                default:
                    return 0;
            }
        });
    }

    private int compareRarity(String r1, String r2) {
        if (r1 == null)
            r1 = "COMMON";
        if (r2 == null)
            r2 = "COMMON";
        FishingModels.Rarity rab = FishingModels.Rarity.valueOf(r1);
        FishingModels.Rarity rbb = FishingModels.Rarity.valueOf(r2);
        return Integer.compare(rbb.ordinal(), rab.ordinal()); // Descending
    }

    public enum SortType {
        FAVORITE, RARITY, WEIGHT
    }
}
