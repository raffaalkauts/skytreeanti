package com.wiredid.skytree.fishing;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.PersistenceService;
import com.wiredid.skytree.fishing.FishingModels.*;
import com.wiredid.skytree.fishing.FishingModels.RodEnchant;
import com.wiredid.skytree.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.*;
import java.util.stream.Collectors;

public class FishingService {

    private final SkytreePlugin plugin;
    private final Random random = new Random();

    // Mock Database of Fish (In a real app, load from config/JSON)
    private final List<Fish> fishDatabase = new ArrayList<>();

    public FishingService(SkytreePlugin plugin, PersistenceService persistence) {
        this.plugin = plugin;

        loadFishDatabase();
    }

    private void loadFishDatabase() {
        fishDatabase.clear();

        // 🟢 COMMON FISH (2 - 8 USDT)
        addFish("mackerel", Rarity.COMMON, Material.COD, 3, 1, 3);
        addFish("sardine", Rarity.COMMON, Material.COD, 2, 0.5, 1.5);
        addFish("haddock", Rarity.COMMON, Material.COD, 4, 1.5, 4);
        addFish("red_guppy", Rarity.COMMON, Material.TROPICAL_FISH, 5, 0.1, 0.5);
        addFish("clownfish", Rarity.COMMON, Material.TROPICAL_FISH, 6, 0.2, 0.8);
        addFish("azure_damsel", Rarity.COMMON, Material.TROPICAL_FISH, 6, 0.2, 0.7);
        addFish("white_bass", Rarity.COMMON, Material.COD, 8, 1, 5);
        addFish("pygmy_goby", Rarity.COMMON, Material.TROPICAL_FISH, 2, 0.05, 0.2);
        addFish("herring_fish", Rarity.COMMON, Material.COD, 4, 0.5, 2);
        addFish("fade_tang", Rarity.COMMON, Material.TROPICAL_FISH, 5, 0.3, 1);
        addFish("goldfish", Rarity.COMMON, Material.TROPICAL_FISH, 15, 0.5, 2);
        addFish("mullet", Rarity.COMMON, Material.COD, 6, 1, 4);
        addFish("clam", Rarity.COMMON, Material.NAUTILUS_SHELL, 5, 0.5, 2);
        addFish("yellow_grouper", Rarity.COMMON, Material.COD, 7, 2, 8);
        addFish("gray_triggerfish", Rarity.COMMON, Material.COD, 7, 1, 3);
        addFish("black_sea_bass", Rarity.COMMON, Material.COD, 8, 2, 10);
        addFish("seaweed", Rarity.COMMON, Material.KELP, 1, 0.1, 1);
        addFish("atlantic_mackerel", Rarity.COMMON, Material.COD, 3, 1, 3);
        addFish("flying_fish", Rarity.COMMON, Material.COD, 10, 0.5, 2);
        addFish("boots", Rarity.COMMON, Material.LEATHER_BOOTS, 0.5, 0.5, 2);

        // 🔵 UNCOMMON FISH (10 - 35 USDT)
        addFish("swordfish", Rarity.UNCOMMON, Material.SALMON, 25, 20, 100);
        addFish("viperfish", Rarity.UNCOMMON, Material.SALMON, 30, 2, 8);
        addFish("pilot_fish", Rarity.UNCOMMON, Material.SALMON, 15, 1, 5);
        addFish("red_snapper", Rarity.UNCOMMON, Material.SALMON, 20, 2, 12);
        addFish("bluefin_tuna", Rarity.UNCOMMON, Material.SALMON, 35, 50, 250);
        addFish("salmon", Rarity.UNCOMMON, Material.SALMON, 12, 5, 15);
        addFish("martin", Rarity.UNCOMMON, Material.SALMON, 35, 40, 200);
        addFish("parrot_fish", Rarity.UNCOMMON, Material.TROPICAL_FISH, 22, 1, 4);
        addFish("puffer_fish", Rarity.UNCOMMON, Material.PUFFERFISH, 28, 0.5, 3);
        addFish("conch_shell", Rarity.UNCOMMON, Material.NAUTILUS_SHELL, 15, 1, 3);
        addFish("sea_shell", Rarity.UNCOMMON, Material.NAUTILUS_SHELL, 12, 0.5, 2);
        addFish("shrimp", Rarity.UNCOMMON, Material.TROPICAL_FISH, 20, 0.1, 0.3);
        addFish("baby_shrimp", Rarity.UNCOMMON, Material.TROPICAL_FISH, 10, 0.05, 0.1);
        addFish("flat_fish", Rarity.UNCOMMON, Material.COD, 20, 1, 5);
        addFish("gar_fish", Rarity.UNCOMMON, Material.COD, 28, 5, 20);
        addFish("yellow_damsel_fish", Rarity.UNCOMMON, Material.TROPICAL_FISH, 22, 0.2, 0.8);
        addFish("silver_tuna", Rarity.UNCOMMON, Material.SALMON, 32, 40, 180);
        addFish("bandit_angelfish", Rarity.UNCOMMON, Material.TROPICAL_FISH, 38, 0.5, 2);
        addFish("lion_fish", Rarity.UNCOMMON, Material.PUFFERFISH, 40, 1, 3);
        addFish("vintage_damsel", Rarity.UNCOMMON, Material.TROPICAL_FISH, 42, 0.3, 1);
        addFish("maze_angelfish", Rarity.UNCOMMON, Material.TROPICAL_FISH, 45, 0.5, 2);

        // 🟣 RARE FISH (70 - 250 USDT)
        addFish("jewel_tang", Rarity.RARE, Material.TROPICAL_FISH, 120, 0.5, 2);
        addFish("fire_goby", Rarity.RARE, Material.TROPICAL_FISH, 150, 0.1, 0.4);
        addFish("jellyfish", Rarity.RARE, Material.GHAST_TEAR, 80, 1, 5);
        addFish("small_octopus", Rarity.RARE, Material.INK_SAC, 180, 2, 10);
        addFish("liar_nose_fish", Rarity.RARE, Material.SALMON, 200, 1, 5);
        addFish("pearl", Rarity.RARE, Material.GHAST_TEAR, 500, 0.1, 0.2);
        addFish("starfish", Rarity.RARE, Material.NETHER_STAR, 250, 0.5, 2);
        addFish("antique_cup", Rarity.RARE, Material.FLOWER_POT, 220, 1, 2);
        addFish("ballina_angelfish", Rarity.RARE, Material.TROPICAL_FISH, 300, 0.5, 2);
        addFish("barracuda_fish", Rarity.RARE, Material.SALMON, 180, 10, 50);
        addFish("candy_butterfly", Rarity.RARE, Material.TROPICAL_FISH, 240, 0.3, 1);
        addFish("charmed_tang", Rarity.RARE, Material.TROPICAL_FISH, 280, 0.5, 2);

        // 🟠 LEGEND FISH (500 - 2000 USDT)
        addFish("diamond_ring", Rarity.LEGEND, Material.DIAMOND, 1500, 0.1, 0.2);
        addFish("dolphin", Rarity.LEGEND, Material.PUFFERFISH, 800, 50, 200);
        addFish("starjam_tang", Rarity.LEGEND, Material.TROPICAL_FISH, 1000, 0.5, 2);
        addFish("yellowfin_tuna", Rarity.LEGEND, Material.SALMON, 700, 100, 400);
        addFish("chrome_tuna", Rarity.LEGEND, Material.SALMON, 1200, 100, 400);
        addFish("ruby", Rarity.LEGEND, Material.REDSTONE, 1400, 0.1, 0.3);
        addFish("saw_fish", Rarity.LEGEND, Material.SALMON, 900, 50, 150);
        addFish("lobster", Rarity.LEGEND, Material.COOKED_COD, 500, 1, 5);
        addFish("fish_fossil", Rarity.LEGEND, Material.BONE, 450, 2, 10);
        addFish("seahorses", Rarity.LEGEND, Material.TROPICAL_FISH, 600, 0.1, 0.5);

        // 🔴 MYTHICAL (4000 - 15000 USDT)
        addFish("manta_ray", Rarity.MYTHIC, Material.PHANTOM_MEMBRANE, 5000, 100, 500);
        addFish("luminous_fish", Rarity.MYTHIC, Material.GLOWSTONE_DUST, 4000, 1, 5);
        addFish("abyss_seahorses", Rarity.MYTHIC, Material.TROPICAL_FISH, 7500, 0.2, 1);
        addFish("blueflame_ray", Rarity.MYTHIC, Material.PHANTOM_MEMBRANE, 8500, 150, 600);
        addFish("thresher_shark", Rarity.MYTHIC, Material.SALMON, 10000, 200, 1000);
        addFish("sharp_one", Rarity.MYTHIC, Material.IRON_SWORD, 6000, 50, 200);
        addFish("hybodus_shark", Rarity.MYTHIC, Material.SALMON, 12000, 300, 1200);
        addFish("mako_shark", Rarity.MYTHIC, Material.SALMON, 14000, 250, 1100);
        addFish("beluga_sturgeon", Rarity.MYTHIC, Material.SALMON, 8000, 150, 800);
        addFish("blue_marlin", Rarity.MYTHIC, Material.SALMON, 15000, 300, 1500);
        addFish("stingray", Rarity.MYTHIC, Material.PHANTOM_MEMBRANE, 3500, 20, 80);

        // ⚫ LIMITED FISH (25000 - 100000 USDT)
        addFish("blob_shark", Rarity.LIMITED, Material.END_CRYSTAL, 35000, 5, 20);
        addFish("thin_armor_shark", Rarity.LIMITED, Material.END_CRYSTAL, 40000, 50, 200);
        addFish("ghost_shark", Rarity.LIMITED, Material.END_CRYSTAL, 45000, 10, 50);
        addFish("skeleton_narwhal", Rarity.LIMITED, Material.END_CRYSTAL, 50000, 500, 2000);
        addFish("giant_squid", Rarity.LIMITED, Material.END_CRYSTAL, 60000, 1000, 5000);
        addFish("king_crab", Rarity.LIMITED, Material.END_CRYSTAL, 70000, 10, 30);
        addFish("scare", Rarity.LIMITED, Material.END_CRYSTAL, 80000, 1, 5);
        addFish("worm_fish", Rarity.LIMITED, Material.END_CRYSTAL, 90000, 0.1, 0.5);
        addFish("kraken", Rarity.LIMITED, Material.END_CRYSTAL, 100000, 5000, 20000);
        addFish("megalodon", Rarity.LIMITED, Material.END_CRYSTAL, 125000, 10000, 50000);
        addFish("whale_shark", Rarity.LIMITED, Material.END_CRYSTAL, 150000, 20000, 100000);
        addFish("bloodmoon_whale", Rarity.LIMITED, Material.END_CRYSTAL, 200000, 50000, 250000);
    }

    private void addFish(String id, Rarity rarity, Material material, double basePrice, double minWeight,
            double maxWeight) {
        fishDatabase.add(new Fish(id, rarity, material, basePrice, minWeight, maxWeight));
    }

    public ItemStack createRod(RodTier tier) {
        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        meta.displayName(Component.text(tier.getDisplayName()).color(NamedTextColor.AQUA));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Tier: " + tier.name()).color(NamedTextColor.GRAY));
        lore.add(Component.text("Slots: " + tier.getSlots()).color(NamedTextColor.GRAY));
        lore.add(Component.text(""));
        lore.add(Component.text("Enchants:").color(NamedTextColor.YELLOW));
        lore.add(Component.text(" (Empty)").color(NamedTextColor.DARK_GRAY));

        meta.lore(lore);

        // Add basic enchantments
        meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 3, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 3, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);

        // Make unbreakable
        meta.setUnbreakable(true);

        rod.setItemMeta(meta);

        UUID rodId = UUID.randomUUID();
        NbtUtils.setRodId(rod, rodId);
        NbtUtils.setRodTier(rod, tier);
        NbtUtils.setRodSlots(rod, tier.getSlots());
        NbtUtils.setString(rod, NbtUtils.KEY_ROD_ENCHANTS, "");

        return rod;
    }

    public ItemStack generateFish(Player player, ItemStack rod) {
        RodTier tier = NbtUtils.getRodTier(rod);
        Map<RodEnchant, Integer> enchants = getEnchantMap(rod);

        // 1. Determine Rarity (with Pity and Bait)
        Rarity rarity = rollRarity(player.getUniqueId(), rod, tier, enchants);

        // 2. Consume Bait if active
        if (plugin.getBaitService() != null) {
            plugin.getBaitService().consumeBait(player.getUniqueId());
        }

        // 2. Pick Fish ID
        Fish paramFish = pickFishByRarity(rarity);

        // 3. Roll Mutation
        Mutation mutation = rollMutation(tier, enchants);

        // 4. Calculate Weight
        double weight = rollWeight(tier, paramFish, mutation, enchants);

        // 5. Create Item
        ItemStack item = createFishItem(paramFish, rarity, mutation, weight, enchants);

        // Global Announcement (Legend+)
        if (rarity.ordinal() >= Rarity.LEGEND.ordinal()) {
            announceCatch(player, paramFish, rarity, mutation, weight);
        }

        // Apply Utility Enchants (Ocean Grace, Adrenaline Rush)
        applyUtilityEnchants(player, enchants);

        return item;
    }

    private ItemStack createFishItem(Fish fish, Rarity rarity, Mutation mutation, double weight,
            Map<RodEnchant, Integer> enchants) {
        ItemStack item = new ItemStack(fish.material());
        ItemMeta meta = item.getItemMeta();

        // Initial simple name (will be overwritten by CustomFishService)
        String cleanName = fish.id().replace("_", " ").toUpperCase();
        meta.displayName(ComponentUtil.parse(rarity.getColor() + cleanName));

        item.setItemMeta(meta);

        NbtUtils.setString(item, NbtUtils.KEY_FISH_ID, fish.id());
        NbtUtils.setString(item, NbtUtils.KEY_FISH_RARITY, rarity.name());
        NbtUtils.setString(item, NbtUtils.KEY_FISH_MUTATION, mutation.name());
        NbtUtils.setDouble(item, NbtUtils.KEY_FISH_WEIGHT, weight);
        double price = calculatePrice(fish, weight, mutation, enchants);
        NbtUtils.setDouble(item, NbtUtils.KEY_FISH_PRICE, price);

        // Enchant limited fish
        if (rarity == Rarity.LIMITED) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        // Apply Custom Visuals (Resource Pack, Gradients)
        if (plugin.getCustomFishService() != null) {
            plugin.getCustomFishService().applyVisuals(item, fish.id(), mutation);
        }

        return item;
    }

    public Map<RodEnchant, Integer> getEnchantMap(ItemStack rod) {
        String data = NbtUtils.getString(rod, NbtUtils.KEY_ROD_ENCHANTS);
        Map<RodEnchant, Integer> map = new HashMap<>();
        if (data == null || data.isEmpty())
            return map;

        for (String pair : data.split(",")) {
            if (pair.contains(":")) {
                String[] parts = pair.split(":");
                try {
                    map.put(RodEnchant.valueOf(parts[0]), Integer.parseInt(parts[1]));
                } catch (Exception ignored) {
                }
            } else {
                try {
                    map.put(RodEnchant.valueOf(pair), 1);
                } catch (Exception ignored) {
                }
            }
        }
        return map;
    }

    private void applyUtilityEnchants(Player player, Map<RodEnchant, Integer> enchants) {
        if (enchants.containsKey(RodEnchant.OCEAN_GRACE)) {
            double heal = enchants.get(RodEnchant.OCEAN_GRACE);
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            player.setHealth(Math.min(maxHealth, player.getHealth() + heal));
        }
        if (enchants.containsKey(RodEnchant.ADRENALINE_RUSH)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SPEED,
                    100 * enchants.get(RodEnchant.ADRENALINE_RUSH),
                    1));
        }
        if (enchants.containsKey(RodEnchant.ZEN_MEDITATION)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.REGENERATION,
                    100,
                    enchants.get(RodEnchant.ZEN_MEDITATION) - 1));
        }
        if (enchants.containsKey(RodEnchant.GLOW_EYES)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.NIGHT_VISION,
                    300,
                    0));
        }
    }

    private Rarity rollRarity(UUID playerId, ItemStack rod, RodTier tier, Map<RodEnchant, Integer> enchants) {
        Rarity result;
        // 1. Update Pity counters on Rod
        int pRare = NbtUtils.getInt(rod, NbtUtils.KEY_PITY_RARE) + 1;
        int pLegend = NbtUtils.getInt(rod, NbtUtils.KEY_PITY_LEGEND) + 1;
        int pMythic = NbtUtils.getInt(rod, NbtUtils.KEY_PITY_MYTHIC) + 1;
        int pLimited = NbtUtils.getInt(rod, NbtUtils.KEY_PITY_LIMITED) + 1;

        // 2. Define Thresholds based on Tier
        double pityMult = 1.0;
        if (enchants.containsKey(RodEnchant.COMPASSION)) {
            pityMult -= (enchants.get(RodEnchant.COMPASSION) * 0.05);
        }

        int tRare = (int) (20 * pityMult);
        int tLegend = (int) (100 * pityMult);
        int tMythic = (int) (500 * pityMult);
        int tLimited = (int) (5000 * pityMult);

        // ... rest of method ...

        if (tier == RodTier.ADVANCED) {
            tRare = 15;
            tLegend = 80;
        }
        if (tier == RodTier.MYTHIC) {
            tRare = 10;
            tLegend = 60;
            tMythic = 300;
        }
        if (tier == RodTier.RELIC) {
            tRare = 5;
            tLegend = 40;
            tMythic = 200;
            tLimited = 2500;
        }

        // 3. Check for Guarantees (Highest rarity first)
        Rarity guaranteed = null;
        if (pLimited >= tLimited) {
            guaranteed = Rarity.LIMITED;
            pLimited = 0;
        } else if (pMythic >= tMythic) {
            guaranteed = Rarity.MYTHIC;
            pMythic = 0;
        } else if (pLegend >= tLegend) {
            guaranteed = Rarity.LEGEND;
            pLegend = 0;
        } else if (pRare >= tRare) {
            guaranteed = Rarity.RARE;
            pRare = 0;
        }

        if (guaranteed != null) {
            result = guaranteed;
        } else {
            // 3. Roll Logic
            double roll = random.nextDouble() * 100;

            // Apply Abyssal Luck (+5% quality per lvl)
            if (enchants.containsKey(RodEnchant.ABYSSAL_LUCK)) {
                roll += (enchants.get(RodEnchant.ABYSSAL_LUCK) * 5);
            }

            // Apply LIMITED Reach (+10% chance boost for LIMITED)
            double limitedBuff = 1.0;
            if (enchants.containsKey(RodEnchant.LIMITED_REACH)) {
                limitedBuff += (enchants.get(RodEnchant.LIMITED_REACH) * 0.1);
            }

            // POSEIDON'S ROD CHECK
            boolean isPoseidon = NbtUtils.getString(rod, "poseidon_rod") != null;
            if (isPoseidon) {
                limitedBuff += 10.0; // Huge buff for limited
            }

            // Apply Sea Blessing (1% per lvl skip common)
            if (enchants.containsKey(RodEnchant.SEA_BLESSING)) {
                if (random.nextDouble() < (enchants.get(RodEnchant.SEA_BLESSING) * 0.01)) {
                    roll += 10; // Bump the roll
                }
            }

            // Base Chances (Reduced by 35% because of Pity system)
            double limitedChance = 0.000006 * 100 * limitedBuff;
            // Poseidon Rod specific flat boost to limited chance?
            if (isPoseidon) {
                limitedChance += 10.0; // +10% flat chance as requested ("+10% chance to catch Limited fish")
            }

            double mythicChance = 0.003 * 100;
            double legendChance = 0.013 * 100;
            double rareChance = 0.065 * 100;
            double uncommonChance = 0.195 * 100;

            // Apply Bait Bonuses
            if (plugin.getBaitService() != null) {
                mythicChance += plugin.getBaitService().getLegendaryBonus(playerId) * 0.5; // Legendary bonus affects
                                                                                           // mythic slightly
                legendChance += plugin.getBaitService().getLegendaryBonus(playerId) * 100;
                rareChance += plugin.getBaitService().getRareBonus(playerId) * 100;
                uncommonChance += plugin.getBaitService().getUncommonBonus(playerId) * 100;
            }

            // ... Rest of logic matches ...

            // Modifiers from Rod Tier (Tier based buffs)
            if (tier == RodTier.ADVANCED) {
                rareChance += 2;
            }
            if (tier == RodTier.MYTHIC) {
                legendChance += 1;
                mythicChance += 0.2;
            }
            if (tier == RodTier.RELIC) {
                legendChance += 2;
                mythicChance += 0.5;
                limitedChance += 0.0001; // Tiny buff to limited
            }

            // Biome Master (+15% per lvl in ocean/river)
            if (enchants.containsKey(RodEnchant.BIOME_MASTER)) {
                // In FishingListener we should maybe pass biome, but for now simple check:
                double mult = 1.0 + (enchants.get(RodEnchant.BIOME_MASTER) * 0.15);
                rareChance *= mult;
                legendChance *= mult;
            }

            // Depth Diver (+10% in deep water)
            if (enchants.containsKey(RodEnchant.DEPTH_DIVER)) {
                rareChance *= 1.1;
            }

            result = Rarity.COMMON;
            if (roll < limitedChance)
                result = Rarity.LIMITED;
            else if (roll < limitedChance + mythicChance)
                result = Rarity.MYTHIC;
            else if (roll < limitedChance + mythicChance + legendChance)
                result = Rarity.LEGEND;
            else if (roll < limitedChance + mythicChance + legendChance + rareChance)
                result = Rarity.RARE;
            else if (roll < limitedChance + mythicChance + legendChance + rareChance + uncommonChance)
                result = Rarity.UNCOMMON;
        }

        // 5. Apply Pity Boosts (Scalability)
        if (result == Rarity.MYTHIC) {
            pLimited += 500;
            pMythic = 0;
        } else if (result == Rarity.LEGEND) {
            pMythic += 150; // User Request
            pLimited += 50;
            pLegend = 0;
        } else if (result == Rarity.RARE) {
            pLegend += 25;
            pMythic += 10;
            pLimited += 5;
            pRare = 0;
        } else if (result == Rarity.UNCOMMON) {
            pRare += 5;
            pLegend += 2;
        } else if (result == Rarity.LIMITED) {
            pLimited = 0;
        }

        // 6. Save Updated Pity
        NbtUtils.setInt(rod, NbtUtils.KEY_PITY_RARE, pRare);
        NbtUtils.setInt(rod, NbtUtils.KEY_PITY_LEGEND, pLegend);
        NbtUtils.setInt(rod, NbtUtils.KEY_PITY_MYTHIC, pMythic);
        NbtUtils.setInt(rod, NbtUtils.KEY_PITY_LIMITED, pLimited);

        return result;
    }

    public ItemStack createPoseidonRod() {
        ItemStack rod = createRod(RodTier.RELIC); // Base Relic
        ItemMeta meta = rod.getItemMeta();

        meta.displayName(ComponentUtil.smartParse("§b§lPoseidon's Rod"));
        List<Component> lore = meta.lore();
        lore.add(0, ComponentUtil.smartParse("§6§lLEGENDARY ARTIFACT"));
        lore.add(ComponentUtil.smartParse("§7+10% Chance for LIMITED Fish"));
        meta.lore(lore);

        // Max Enchants
        // Assuming max level for enchants is 5 or 3 depending on enchant.
        // I'll set some sensible maxes.
        Map<RodEnchant, Integer> enchants = new HashMap<>();
        for (RodEnchant e : RodEnchant.values()) {
            enchants.put(e, 5); // Max all to 5 for now
        }

        // Write enchants to NBT string
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<RodEnchant, Integer> entry : enchants.entrySet()) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(entry.getKey().name()).append(":").append(entry.getValue());
        }

        // Update Lore for enchants
        // (Simplified re-creation of lore)
        List<Component> newLore = new ArrayList<>();
        newLore.add(ComponentUtil.smartParse("§6§lLEGENDARY ARTIFACT"));
        newLore.add(ComponentUtil.smartParse("§7+10% Chance for LIMITED Fish"));
        newLore.add(Component.empty());
        newLore.add(ComponentUtil.smartParse("§eEnchants:"));
        for (RodEnchant e : RodEnchant.values()) {
            newLore.add(ComponentUtil.smartParse("§7- " + e.getDisplayName() + " V"));
        }
        meta.lore(newLore);
        meta.setUnbreakable(true);
        meta.setEnchantmentGlintOverride(true);

        rod.setItemMeta(meta);

        NbtUtils.setString(rod, NbtUtils.KEY_ROD_ENCHANTS, sb.toString());
        NbtUtils.setString(rod, "poseidon_rod", "true");

        return rod;
    }

    private Fish pickFishByRarity(Rarity rarity) {
        List<Fish> pool = fishDatabase.stream().filter(f -> f.rarity() == rarity).toList();
        if (pool.isEmpty())
            return fishDatabase.get(0); // Fallback
        return pool.get(random.nextInt(pool.size()));
    }

    private Mutation rollMutation(RodTier tier, Map<RodEnchant, Integer> enchants) {
        double roll = random.nextDouble() * 100;

        double baseMutationChance = 10.0 * tier.getMutationModifier(); // Base scaled by Tier
        if (enchants.containsKey(RodEnchant.MUTATION_MASTERY)) {
            baseMutationChance += (enchants.get(RodEnchant.MUTATION_MASTERY) * 5);
        }

        if (roll > baseMutationChance)
            return Mutation.NORMAL;

        // Roll specific mutation type
        roll = random.nextDouble() * 100;

        double ancientChance = 0.5;
        double goldChance = 2.0;
        double shinyChance = 10.0;
        double bigChance = 30.0;

        if (enchants.containsKey(RodEnchant.ANCIENT_WISDOM)) {
            ancientChance += (enchants.get(RodEnchant.ANCIENT_WISDOM) * 0.5);
            goldChance += (enchants.get(RodEnchant.ANCIENT_WISDOM) * 1);
        }
        if (enchants.containsKey(RodEnchant.SHINY_GLOW)) {
            shinyChance += (enchants.get(RodEnchant.SHINY_GLOW) * 5);
        }

        if (roll < ancientChance)
            return Mutation.ANCIENT;
        if (roll < goldChance)
            return Mutation.GOLD;
        if (roll < shinyChance)
            return Mutation.SHINY;
        if (roll < bigChance)
            return Mutation.BIG;
        return Mutation.NORMAL;
    }

    private double rollWeight(RodTier tier, Fish fish, Mutation mutation, Map<RodEnchant, Integer> enchants) {
        double weightMod = tier.getWeightModifier();
        double min = fish.minWeight() * weightMod;
        double max = fish.maxWeight() * weightMod;

        double range = max - min;
        double base = min + (range * random.nextDouble());

        if (enchants.containsKey(RodEnchant.HEAVY_WEIGHT)) {
            base *= (1.0 + (enchants.get(RodEnchant.HEAVY_WEIGHT) * 0.1));
        }

        if (enchants.containsKey(RodEnchant.CALIBRATED_SCALE)) {
            // Favor higher values in the range
            double variance = random.nextDouble();
            variance = Math.pow(variance, 1.0 / (1.0 + (enchants.get(RodEnchant.CALIBRATED_SCALE) * 0.2)));
            base = min + (range * variance);
        }

        if (mutation == Mutation.BIG)
            base *= 1.3;

        return base;
    }

    public double calculatePrice(Fish fish, double weight, Mutation mutation, Map<RodEnchant, Integer> enchants) {
        double base = fish.basePrice();
        // Weight multiplier: (actual / min) e.g., 1.0 - 2.0x
        double weightMult = weight / fish.minWeight();

        double finalPrice = base * weightMult * mutation.getPriceMultiplier();

        // Gilded Hook (+10% per lvl)
        if (enchants.containsKey(RodEnchant.GILDED_HOOK)) {
            finalPrice *= (1.0 + (enchants.get(RodEnchant.GILDED_HOOK) * 0.1));
        }

        // Capitalism (+5% per lvl)
        if (enchants.containsKey(RodEnchant.CAPITALISM)) {
            finalPrice *= (1.0 + (enchants.get(RodEnchant.CAPITALISM) * 0.05));
        }

        // Infernal Hook (+5% per lvl "cooked" bonus)
        if (enchants.containsKey(RodEnchant.INFERNAL_HOOK)) {
            finalPrice *= (1.0 + (enchants.get(RodEnchant.INFERNAL_HOOK) * 0.05));
        }

        return finalPrice;
    }

    public List<Fish> getFishByRarity(Rarity rarity) {
        return fishDatabase.stream().filter(f -> f.rarity() == rarity).collect(Collectors.toList());
    }

    private void announceCatch(Player player, Fish fish, Rarity rarity, Mutation mutation, double weight) {
        String fishName = fish.id().replace("_", " ").toUpperCase();
        String rarityName = rarity.getDisplayName();
        String color = rarity.getColor();

        // Use a cleaner announcement style
        String prefix = "§b§l[Fishing] ";
        String catchText = "§e" + player.getName() + " §7caught a ";
        String fishDisp = color + (mutation != Mutation.NORMAL ? mutation.getDisplayName() + " " : "") + fishName;

        Component msg = ComponentUtil.smartParse(
                prefix + catchText + fishDisp + " §7(" + rarityName + ") §f[" + String.format("%.2f", weight) + "kg]");

        plugin.getServer().broadcast(msg);

        // Visual Effects for LEGEND+
        if (rarity.ordinal() >= Rarity.LEGEND.ordinal()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 2, 0), 20, 0.5, 0.5,
                    0.5, 0.1);

            Firework fw = player.getWorld().spawn(player.getLocation(), Firework.class);
            FireworkMeta fwm = fw.getFireworkMeta();
            fwm.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(org.bukkit.Color.ORANGE, org.bukkit.Color.YELLOW)
                    .withFade(org.bukkit.Color.WHITE)
                    .flicker(true)
                    .build());
            fwm.setPower(1);
            fw.setFireworkMeta(fwm);
        }
    }

    public void scheduleAutoCast(Player player) {
        ItemStack initialHand = player.getInventory().getItemInMainHand();
        RodTier initialTier = NbtUtils.getRodTier(initialHand);
        long delay = (initialTier == RodTier.RELIC) ? 2L : 14L;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR || hand.getType() != Material.FISHING_ROD)
                return;

            // Prevent spamming hooks: Check if one exists
            if (player.getFishHook() != null && player.getFishHook().isValid()) {
                return;
            }

            org.bukkit.entity.FishHook hook = player.launchProjectile(org.bukkit.entity.FishHook.class);
            // hook.setLureLevel(lureLevel); // Not available in all Paper versions, manual
            // wait time handles it

            Map<RodEnchant, Integer> enchants = getEnchantMap(hand);
            int lureLevel = hand.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LURE);
            RodTier tier = NbtUtils.getRodTier(hand);
            int tidalLvl = enchants.getOrDefault(RodEnchant.TIDAL_SPEED, 0);
            int steadyLvl = enchants.getOrDefault(RodEnchant.STEADY_HAND, 0);
            int instantLvl = enchants.getOrDefault(RodEnchant.INSTANT_STRIKE, 0);

            if (instantLvl > 0 && random.nextDouble() < (instantLvl * 0.005)) {
                hook.setMinWaitTime(1);
                hook.setMaxWaitTime(2);
                return;
            }

            if (tier == RodTier.RELIC) {
                // Extremely fast: 1-5 ticks
                hook.setMinWaitTime(Math.max(1, 3 - (lureLevel / 2) - tidalLvl));
                hook.setMaxWaitTime(Math.max(3, 8 - (lureLevel / 2) - tidalLvl));
            } else if (tier == RodTier.MYTHIC) {
                // Very fast: 20-60 ticks
                hook.setMinWaitTime(Math.max(10, 40 - (lureLevel * 5) - (tidalLvl * 4)));
                hook.setMaxWaitTime(Math.max(30, 80 - (lureLevel * 5) - (tidalLvl * 8)));
            } else {
                // Default logic if we want to apply enchants to lower tiers too
                if (tidalLvl > 0) {
                    hook.setMinWaitTime(Math.max(20, 100 - (tidalLvl * 15)));
                    hook.setMaxWaitTime(Math.max(60, 300 - (tidalLvl * 30)));
                }
            }

            // Apply steady hand (reduces variance)
            if (steadyLvl > 0) {
                int min = hook.getMinWaitTime();
                int max = hook.getMaxWaitTime();
                int diff = max - min;
                hook.setMaxWaitTime(min + (int) (diff * (1.0 - (steadyLvl * 0.1))));
            }
        }, delay);
    }

    public void handleAutoReel(Player player, org.bukkit.entity.FishHook hook, FishStorage storage) {
        if (hook == null || !hook.isValid())
            return;

        ItemStack initialHand = player.getInventory().getItemInMainHand();
        RodTier initialTier = NbtUtils.getRodTier(initialHand);
        long delay = (initialTier == RodTier.RELIC) ? 1L : 5L;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && hook.isValid()) {
                hook.remove();
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                ItemStack rod = player.getInventory().getItemInMainHand();
                Map<RodEnchant, Integer> enchants = getEnchantMap(rod);

                // Treasure Hunter (2% per level)
                if (enchants.containsKey(RodEnchant.TREASURE_HUNTER)) {
                    if (random.nextDouble() < (enchants.get(RodEnchant.TREASURE_HUNTER) * 0.02)) {
                        giveTreasure(player, storage);
                        scheduleAutoCast(player);
                        return;
                    }
                }

                int catchCount = 1;
                if (enchants.containsKey(RodEnchant.DOUBLE_HOOK)
                        && random.nextDouble() < (enchants.get(RodEnchant.DOUBLE_HOOK) * 0.05))
                    catchCount = 2;
                if (enchants.containsKey(RodEnchant.TRIPLE_HOOK)
                        && random.nextDouble() < (enchants.get(RodEnchant.TRIPLE_HOOK) * 0.02))
                    catchCount = 3;
                if (enchants.containsKey(RodEnchant.TRAWLING_NET)
                        && random.nextDouble() < (enchants.get(RodEnchant.TRAWLING_NET) * 0.005))
                    catchCount = random.nextInt(5) + 4;

                for (int i = 0; i < catchCount; i++) {
                    ItemStack fish = generateFish(player, rod);
                    storage.addFish(player.getUniqueId(), fish);
                }

                checkFirstTimeHint(player);
                scheduleAutoCast(player);
            }
        }, delay);
    }

    private final Set<UUID> hintedPlayers = new HashSet<>();

    private void checkFirstTimeHint(Player player) {
        if (hintedPlayers.add(player.getUniqueId())) {
            player.sendMessage("§b§l[Fishing] §7Nice catch! Check your fish collection at §e/fish");
        }
    }

    private void giveTreasure(Player player, FishStorage storage) {
        // Simple Treasure: Random valuable item
        ItemStack treasure = new ItemStack(Material.GOLD_INGOT, random.nextInt(4) + 1);
        ItemMeta meta = treasure.getItemMeta();
        meta.displayName(Component.text("Ocean Treasure", net.kyori.adventure.text.format.NamedTextColor.GOLD));
        treasure.setItemMeta(meta);

        storage.addFish(player.getUniqueId(), treasure);
        player.sendMessage("§b§l[Fishing] §7You found an §6§lOcean Treasure§7!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    /**
     * Get a fish by its ID for visual system integration
     *
     * @param fishId The fish ID to look up
     * @return The Fish object or null if not found
     */
    public Fish getFishById(String fishId) {
        return fishDatabase.stream()
                .filter(fish -> fish.id().equalsIgnoreCase(fishId))
                .findFirst()
                .orElse(null);
    }
}
