package com.wiredid.skytree.fishing;

public class FishingModels {

    public enum Rarity {
        COMMON("Common", "&f"),
        UNCOMMON("Uncommon", "&a"),
        RARE("Rare", "&9"),
        MYTHIC("Mythic", "&5"),
        LEGEND("Legend", "&6&l"),
        LIMITED("Limited", "<rainbow><b>");

        private final String displayName;
        private final String color;

        Rarity(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }
    }

    public enum Mutation {
        NORMAL("Normal", 1.0, 0),
        BIG("Big", 1.3, 0.05),
        SHINY("Shiny", 2.0, 0.01),
        GOLD("Gold", 5.0, 0.003),
        ANCIENT("Ancient", 10.0, 0.0005);

        private final String displayName;
        private final double priceMultiplier;
        private final double chance;

        Mutation(String displayName, double priceMultiplier, double chance) {
            this.displayName = displayName;
            this.priceMultiplier = priceMultiplier;
            this.chance = chance;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getPriceMultiplier() {
            return priceMultiplier;
        }

        public double getChance() {
            return chance;
        }
    }

    public record Fish(String id, Rarity rarity, org.bukkit.Material material, double basePrice, double minWeight,
            double maxWeight) {
    }

    public enum RodTier {
        BASIC("Basic Rod", 5, 1.0, 1.0),
        ADVANCED("Advanced Rod", 5, 1.2, 1.2),
        MYTHIC("Mythic Rod", 5, 1.5, 1.5),
        RELIC("Relic Rod", 5, 2.0, 2.0);

        private final String displayName;
        private final int slots;
        private final double mutationModifier;
        private final double weightModifier;

        RodTier(String displayName, int slots, double mutationModifier, double weightModifier) {
            this.displayName = displayName;
            this.slots = slots;
            this.mutationModifier = mutationModifier;
            this.weightModifier = weightModifier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getSlots() {
            return slots;
        }

        public double getMutationModifier() {
            return mutationModifier;
        }

        public double getWeightModifier() {
            return weightModifier;
        }
    }

    public enum RodEnchant {
        // --- Rate & Speed ---
        TIDAL_SPEED("Tidal Speed", "Decreases bite time by 8% per level"),
        INSTANT_STRIKE("Instant Strike", "0.5% chance per level for instant bite"),
        STEADY_HAND("Steady Hand", "Reduces wait time variance by 10% per level"),

        // --- Rarity & Quality ---
        ABYSSAL_LUCK("Abyssal Luck", "Increases rarity roll quality by 5% per level"),
        MUTATION_MASTERY("Mutation Mastery", "Increases mutation chance by 5% per level"),
        SHINY_GLOW("Shiny Glow", "Increases SHINY mutation chance by 10% per level"),
        ANCIENT_WISDOM("Ancient Wisdom", "Increases GOLD/ANCIENT chance by 5% per level"),
        LIMITED_REACH("Limited Reach", "Boosts LIMITED rarity chance by 10% per level"),
        DEPTH_DIVER("Depth Diver", "Better catches in deep water (+10% roll)"),
        BIOME_MASTER("Biome Master", "15% better catch rate in favorable biomes"),

        // --- Quantity ---
        DOUBLE_HOOK("Double Hook", "5% chance per level to catch 2 fish"),
        TRIPLE_HOOK("Triple Hook", "2% chance per level to catch 3 fish"),
        TRAWLING_NET("Trawling Net", "0.5% chance per level to catch a whole stack (4-8x)"),

        // --- Value & USDT ---
        GILDED_HOOK("Gilded Hook", "Increases fish sell price by 10% per level"),
        CAPITALISM("Capitalism", "Extra USDT based on fish weight (+5% per level)"),
        MERCHANT_EYE("Merchant Eye", "5% discount in fishing shop per level (while held)"),

        // --- Weight ---
        HEAVY_WEIGHT("Heavy Weight", "Boosts fish weight by 10% per level"),
        CALIBRATED_SCALE("Calibrated Scale", "Reduces weight variance, favoring higher values"),

        // --- Treasure & Utility ---
        TREASURE_HUNTER("Treasure Hunter", "2% chance per level to find a chest"),
        BAIT_SAVER("Bait Saver", "10% chance per level to not consume bait"),
        INFERNAL_HOOK("Infernal Hook", "10% chance per level to catch pre-cooked fish"),
        OCEAN_GRACE("Sea's Grace", "Restores 1 HP to player on catch per level"),
        ADRENALINE_RUSH("Adrenaline Rush", "Gives Speed II for 5s after catch per level"),
        ZEN_MEDITATION("Zen Meditation", "Slight health regen while holding the rod"),
        GLOW_EYES("Glow Eyes", "Gives Night Vision while holding the rod"),
        BARBED_HOOK("Barbed Hook", "Reduces escaped fish chance by 15% per level"),

        // --- Pity & Meta ---
        COMPASSION("Compassion", "Reduces all pity thresholds by 5% per level"),
        CRATE_OPENER("Crate Opener", "2% better luck in Gacha per level (while held)"),
        SEA_BLESSING("Sea Blessing", "1% chance per level to skip ALL common fish");

        private final String displayName;
        private final String description;

        RodEnchant(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public int getMaxLevel() {
            return 5;
        }
    }
}
