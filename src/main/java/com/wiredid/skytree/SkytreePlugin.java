package com.wiredid.skytree;

import com.wiredid.skytree.api.*;
import com.wiredid.skytree.command.*;
import com.wiredid.skytree.impl.*;
import com.wiredid.skytree.listener.*;
import com.wiredid.skytree.machine.MachineProcessor;
import com.wiredid.skytree.fishing.FishingService;
import com.wiredid.skytree.fishing.EnchantService;
import com.wiredid.skytree.command.FishCommand;
import com.wiredid.skytree.command.RodCommand;
import com.wiredid.skytree.fishing.FishingListener;
import com.wiredid.skytree.system.AchievementSystem;
import com.wiredid.skytree.system.EnergySystem;
import com.wiredid.skytree.system.QuestSystem;
import com.wiredid.skytree.system.ScoreboardSystem;
import com.wiredid.skytree.system.TabSystem;
import com.wiredid.skytree.economy.EconomyManager;
import com.wiredid.skytree.economy.JobService;
import com.wiredid.skytree.gui.JobGUI;
import com.wiredid.skytree.listener.JobListener;
import com.wiredid.skytree.world.VoidChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Skytree - Complete SkyFactory-like SkyBlock plugin
 * Version 1.0.0-beta - Development Phase
 */
public class SkytreePlugin extends JavaPlugin {

    // Core Services
    private IslandService islandService;
    private EconomyService economyService;
    private ItemRegistry itemRegistry;
    private ShopService shopService;
    private AuctionHouseService auctionHouseService;
    private PersistenceService persistenceService;
    private MachineService machineService;
    private IslandSettingsService settingsService;
    private RecipeService recipeService;
    private IslandShopService islandShopService;
    private MultiblockService multiblockService;
    private PipeService pipeService;
    private StorageService storageService;
    private GachaService gachaService;
    private ThirstService thirstService;
    private FishingService fishingService;
    // private com.wiredid.skytree.fishing.CustomFishService customFishService;
    private EnchantService enchantService;
    private com.wiredid.skytree.fishing.RodStorage rodStorage;
    private com.wiredid.skytree.fishing.FishStorage fishStorage;
    private com.wiredid.skytree.fishing.gui.FishStorageGUI fishStorageGUI;
    private com.wiredid.skytree.fishing.gui.FishPriceGuideGUI fishPriceGuideGUI;
    private com.wiredid.skytree.fishing.gui.FishShopGUI fishShopGUI;
    private com.wiredid.skytree.fishing.gui.FishSellGUI fishSellGUI;
    private com.wiredid.skytree.banking.BankService bankService;
    private AdminService adminService;
    private RankService rankService;

    private com.wiredid.skytree.api.ChatService chatService;
    private com.wiredid.skytree.api.BaitService baitService;
    private com.wiredid.skytree.gui.BaitGUI baitGUI;
    private com.wiredid.skytree.gui.ChatHistoryGUI chatHistoryGUI;
    private com.wiredid.skytree.api.TeamBankService teamBankService;
    private com.wiredid.skytree.gui.TeamBankGUI teamBankGUI;
    private com.wiredid.skytree.gui.PerformanceMonitorGUI performanceMonitorGUI;
    private com.wiredid.skytree.api.InvestmentService investmentService;
    private com.wiredid.skytree.gui.InvestmentGUI investmentGUI;
    private com.wiredid.skytree.api.MonetizationService monetizationService;
    // Progression Systems
    private AchievementSystem achievementSystem;
    private QuestSystem questSystem;
    private IslandQuestService islandQuestService;
    private ItemTransportService itemTransportService;
    private MinionService minionService;
    private ActionLogger actionLogger;
    private com.wiredid.skytree.api.TagService tagService;
    private com.wiredid.skytree.gui.TagGUI tagGUI;
    private com.wiredid.skytree.api.CrateService crateService;
    private com.wiredid.skytree.gui.CrateOpeningGUI crateOpeningGUI;
    private com.wiredid.skytree.system.EventManager eventManager;

    private TabSystem tabSystem;
    private EnergySystem energySystem;
    private MachineProcessor machineProcessor;
    private ScoreboardSystem scoreboardSystem;
    private WorthService worthService;
    private com.wiredid.skytree.api.ShardService shardService;
    private com.wiredid.skytree.api.PlaytimeService playtimeService;
    private com.wiredid.skytree.api.BountyService bountyService;
    private com.wiredid.skytree.gui.WarpGUI warpGUI;
    private com.wiredid.skytree.gui.BountyGUI bountyGUI;
    private com.wiredid.skytree.gui.ShardShopGUI shardShopGUI;
    private com.wiredid.skytree.gui.LeaderboardGUI leaderboardGUI;
    private com.wiredid.skytree.api.LeaderboardService leaderboardService;
    private com.wiredid.skytree.gui.ConfirmationGUI confirmationGUI;
    private com.wiredid.skytree.gui.AuctionHouseGUI auctionHouseGUI;
    private com.wiredid.skytree.gui.AuctionPurchaseGUI auctionPurchaseGUI;
    private com.wiredid.skytree.gui.OrderFulfillmentGUI orderFulfillmentGUI;
    private com.wiredid.skytree.gui.RecipeBrowserGUI recipeBrowserGUI;
    private com.wiredid.skytree.gui.HomeGUI homeGUI;
    private com.wiredid.skytree.gui.PlayerSettingsGUI playerSettingsGUI;
    private com.wiredid.skytree.gui.IslandQuestGUI islandQuestGUI;
    private com.wiredid.skytree.gui.TrustGUI trustGUI;
    private com.wiredid.skytree.listener.IslandMenuGUI islandMenuGUI;
    private com.wiredid.skytree.gui.TeamGUI teamGUI;
    private com.wiredid.skytree.gui.OrdersGUI ordersGUI;
    private com.wiredid.skytree.gui.OrderSetupGUI orderSetupGUI;
    private com.wiredid.skytree.gui.MainMenuGUI mainMenuGUI; // Added MainMenuGUI
    private com.wiredid.skytree.gui.SettingsMainGUI settingsMainGUI;
    private com.wiredid.skytree.gui.IslandSettingsGUI islandSettingsGUI;
    private com.wiredid.skytree.system.DailyRewardsSystem dailyRewardsSystem;
    private com.wiredid.skytree.gui.DailyRewardsGUI dailyRewardsGUI;
    private com.wiredid.skytree.gui.AdminLogsGUI adminLogsGUI;
    private com.wiredid.skytree.gui.AdminDashboardGUI adminDashboardGUI;
    private com.wiredid.skytree.gui.PlayerLookupGUI playerLookupGUI;
    private com.wiredid.skytree.gui.PlayerEditorGUI playerEditorGUI;
    private MythicItemManager mythicItemManager;
    private com.wiredid.skytree.system.EnchantRegistry globalEnchantRegistry;
    private com.wiredid.skytree.listener.EnchantListener globalEnchantListener;
    private com.wiredid.skytree.gui.RankOverviewGUI rankOverviewGUI;
    private final java.util.Set<java.util.UUID> waitingForBountyItem = new java.util.HashSet<>();
    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> bountyTimeouts = new java.util.HashMap<>();
    private com.wiredid.skytree.gui.UpgradeGUI upgradeGUI;
    private com.wiredid.skytree.fishing.CustomFishService customFishService;
    private com.wiredid.skytree.impl.FictitiousOrderService fictitiousOrderService;
    private EconomyManager economyManager;
    private JobService jobService;
    private JobGUI jobGUI;

    private com.wiredid.skytree.command.UniversalTabCompleter universalTabCompleter;
    private com.wiredid.skytree.listener.ChatListener chatListener;
    private com.wiredid.skytree.listener.IslandSafetyListener islandSafetyListener;
    private com.wiredid.skytree.system.WorthVisualSystem worthVisualSystem;
    private com.wiredid.skytree.system.WorthTooltipPacketSystem worthTooltipPacketSystem;
    private com.wiredid.skytree.listener.ShopGUIListener shopGUIListener;
    private com.wiredid.skytree.listener.GachaGUIListener gachaGUIListener;
    private com.wiredid.skytree.listener.OrdersChatListener ordersChatListener;
    private com.wiredid.skytree.gui.MinionSkinGUI minionSkinGUI;
    private com.wiredid.skytree.listener.SellGUIListener sellGUIListener;

    private boolean listenersRegistered = false;

    public com.wiredid.skytree.gui.MainMenuGUI getMainMenuGUI() {
        return mainMenuGUI;
    }

    public IslandQuestService getIslandQuestService() {
        return islandQuestService;
    }

    public com.wiredid.skytree.system.DailyRewardsSystem getDailyRewardsSystem() {
        return dailyRewardsSystem;
    }

    public com.wiredid.skytree.gui.DailyRewardsGUI getDailyRewardsGUI() {
        return dailyRewardsGUI;
    }

    public MinionService getMinionService() {
        return minionService;
    }

    public com.wiredid.skytree.gui.AdminLogsGUI getAdminLogsGUI() {
        return adminLogsGUI;
    }

    public com.wiredid.skytree.gui.AdminDashboardGUI getAdminDashboardGUI() {
        return adminDashboardGUI;
    }

    public com.wiredid.skytree.gui.MinionSkinGUI getMinionSkinGUI() {
        return minionSkinGUI;
    }

    public ActionLogger getActionLogger() {
        return actionLogger;
    }

    public com.wiredid.skytree.fishing.CustomFishService getCustomFishService() {
        return customFishService;
    }

    public com.wiredid.skytree.impl.FictitiousOrderService getFictitiousOrderService() {
        return fictitiousOrderService;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public JobService getJobService() {
        return jobService;
    }

    public JobGUI getJobGUI() {
        return jobGUI;
    }

    public com.wiredid.skytree.gui.TagGUI getTagGUI() {
        return tagGUI;
    }

    public com.wiredid.skytree.api.TagService getTagService() {
        return tagService;
    }

    public com.wiredid.skytree.api.BaitService getBaitService() {
        return baitService;
    }

    public com.wiredid.skytree.gui.BaitGUI getBaitGUI() {
        return baitGUI;
    }

    public com.wiredid.skytree.listener.OrdersChatListener getOrdersChatListener() {
        return ordersChatListener;
    }

    public boolean isIslandSystemEnabled() {
        return getConfig().getBoolean("features.island", true);
    }

    public boolean isMythicItemsEnabled() {
        return getConfig().getBoolean("features.mythic", true);
    }

    public void setFeatureEnabled(String feature, boolean enabled) {
        getConfig().set("features." + feature, enabled);
        saveConfig();
    }

    @Override
    public void onEnable() {
        try {
            // Save default config
            saveDefaultConfig();

            getLogger().info("Initializing Skytree v1.0...");


            // Initialize world
            try {
                initializeWorld();
            } catch (Exception e) {
                getLogger().severe("[ERROR] Failed to initialize world: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            // Initialize core services
            try {
                this.persistenceService = new YamlPersistenceService(this);
            } catch (Exception e) {
                getLogger().severe("[ERROR] Failed to initialize persistence service: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            // Economy Setup (Standalone)
            try {
                this.economyService = new SkytreeEconomyService(this, persistenceService);

                // Rank Service
                this.rankService = new com.wiredid.skytree.impl.RankServiceImpl(this);

                // Admin Service
                this.adminService = new AdminServiceImpl(this);
            } catch (Exception e) {
                getLogger().severe("[ERROR] Failed to initialize economy service: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            // Item Registry & Recipes
            try {
                this.itemRegistry = new SkytreeItemRegistry(this);
                this.recipeService = new SkytreeRecipeService(this, itemRegistry);
            } catch (Exception e) {
                getLogger().severe("[ERROR] Failed to initialize item registry/recipes: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            // Mythic Items
            try {
                this.mythicItemManager = new MythicItemManager(this, (SkytreeItemRegistry) itemRegistry,
                        (SkytreeRecipeService) recipeService);
                this.mythicItemManager.load(); // Load items from JSON
            } catch (Exception e) {
                getLogger().severe("[ERROR] Failed to load mythic items: " + e.getMessage());
                e.printStackTrace();
                // Don't throw - continue without mythic items
            }

            // Gacha System
            try {
                this.gachaService = new SkytreeGachaService(this, persistenceService, mythicItemManager,
                        (SkytreeItemRegistry) itemRegistry, economyService);
                getLogger().info("[OK] Gacha system initialized (Command disabled)");
            } catch (Exception e) {
                getLogger().warning("[ERROR] Failed to initialize gacha system: " + e.getMessage());
                e.printStackTrace();
            }

            // Core Services
            try {
                this.actionLogger = new com.wiredid.skytree.impl.SkytreeActionLogger(this);
                this.islandService = new SkytreeIslandService(this, persistenceService);
                com.wiredid.skytree.model.Island.setPlugin(this);
                com.wiredid.skytree.util.IslandLevelCalculator.setPlugin(this);
                com.wiredid.skytree.banking.util.BankUtil.setPlugin(this);
                com.wiredid.skytree.fishing.NbtUtils.setPlugin(this);
                this.worthService = new com.wiredid.skytree.impl.SkytreeWorthService(this);
                this.shopService = new SkytreeShopService(this, economyService, itemRegistry);

                // Managed reload to avoid circular dependencies
                this.worthService.reload();
                this.shopService.reload(); // Note: shop reload also triggers worth reload for overrides
                this.auctionHouseService = new SkytreeAuctionHouseService(this, economyService);
                this.machineService = new SkytreeMachineService(this);
                this.settingsService = new SkytreeIslandSettingsService(this);
                this.multiblockService = new MultiblockService();
                this.pipeService = new PipeService(this);
                this.storageService = new StorageService();
                this.bankService = new com.wiredid.skytree.banking.impl.BankServiceImpl(this, economyService);
                this.shardService = new com.wiredid.skytree.impl.SkytreeShardService(this);
                this.playtimeService = new com.wiredid.skytree.impl.SkytreePlaytimeService(this);
                this.bountyService = new com.wiredid.skytree.impl.SkytreeBountyService(this);
                this.leaderboardService = new com.wiredid.skytree.impl.SkytreeLeaderboardService(this);
                this.itemTransportService = new com.wiredid.skytree.impl.SkytreeItemTransportService(this);
                this.minionService = new com.wiredid.skytree.impl.SkytreeMinionService(this);
                this.islandShopService = new com.wiredid.skytree.impl.SkytreeIslandShopService(this);
                this.tagService = new com.wiredid.skytree.impl.SkytreeTagService(this);
                this.tagGUI = new com.wiredid.skytree.gui.TagGUI(this, tagService);
                this.crateService = new com.wiredid.skytree.impl.SkytreeCrateService(this);
                this.crateOpeningGUI = new com.wiredid.skytree.gui.CrateOpeningGUI(this, crateService);
                this.chatService = new com.wiredid.skytree.impl.SkytreeChatService(this);
                this.chatHistoryGUI = new com.wiredid.skytree.gui.ChatHistoryGUI(this);
                this.teamBankService = new com.wiredid.skytree.impl.SkytreeTeamBankService(this);
                this.teamBankGUI = new com.wiredid.skytree.gui.TeamBankGUI(this);
                this.performanceMonitorGUI = new com.wiredid.skytree.gui.PerformanceMonitorGUI();
                this.eventManager = new com.wiredid.skytree.system.EventManager(this);
                this.investmentService = new com.wiredid.skytree.impl.SkytreeInvestmentService(this);
                this.investmentGUI = new com.wiredid.skytree.gui.InvestmentGUI(this, investmentService);
                this.monetizationService = new com.wiredid.skytree.impl.SkytreeMonetizationService(this);
                this.customFishService = new com.wiredid.skytree.fishing.CustomFishService(this);
                this.baitService = new com.wiredid.skytree.impl.SkytreeBaitService(this);
                this.baitGUI = new com.wiredid.skytree.gui.BaitGUI(this);
                this.fictitiousOrderService = new com.wiredid.skytree.impl.FictitiousOrderService(this);
                this.economyManager = new EconomyManager(this);
                this.jobService = new JobService(this);

                getLogger().info("[OK] Core services initialized");
            } catch (Exception e) {
                getLogger().severe("[ERROR] Failed to initialize core services: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            // Initialize progression systems
            try {
                this.achievementSystem = new AchievementSystem(this);
                this.questSystem = new QuestSystem(this);
                this.islandQuestService = new SkytreeIslandQuestService(this, islandService);
                this.energySystem = new EnergySystem();
                getLogger().info("[OK] Progression systems initialized");
            } catch (Exception e) {
                getLogger().warning("[ERROR] Failed to initialize progression systems: " + e.getMessage());
                e.printStackTrace();
            }

            // Initialize machine processor and other services
            try {
                this.machineProcessor = new MachineProcessor(this, recipeService);
                this.thirstService = new SkytreeThirstService(this, persistenceService);
                this.scoreboardSystem = new ScoreboardSystem(this);
                this.warpGUI = new com.wiredid.skytree.gui.WarpGUI(this);
                this.confirmationGUI = new com.wiredid.skytree.gui.ConfirmationGUI(this);
                this.auctionHouseGUI = new com.wiredid.skytree.gui.AuctionHouseGUI(this);
                this.auctionPurchaseGUI = new com.wiredid.skytree.gui.AuctionPurchaseGUI(this);
                this.orderFulfillmentGUI = new com.wiredid.skytree.gui.OrderFulfillmentGUI(this);
                this.recipeBrowserGUI = new com.wiredid.skytree.gui.RecipeBrowserGUI(this);
                this.bountyGUI = new com.wiredid.skytree.gui.BountyGUI(this);
                this.shardShopGUI = new com.wiredid.skytree.gui.ShardShopGUI(this);
                this.leaderboardGUI = new com.wiredid.skytree.gui.LeaderboardGUI(this);
                this.homeGUI = new com.wiredid.skytree.gui.HomeGUI(this);
                this.playerSettingsGUI = new com.wiredid.skytree.gui.PlayerSettingsGUI(this);
                this.ordersGUI = new com.wiredid.skytree.gui.OrdersGUI(this);
                this.orderSetupGUI = new com.wiredid.skytree.gui.OrderSetupGUI(this);
                this.upgradeGUI = new com.wiredid.skytree.gui.UpgradeGUI(this);
                this.mainMenuGUI = new com.wiredid.skytree.gui.MainMenuGUI(this);
                this.adminLogsGUI = new com.wiredid.skytree.gui.AdminLogsGUI(this);
                this.adminDashboardGUI = new com.wiredid.skytree.gui.AdminDashboardGUI(this);
                this.playerLookupGUI = new com.wiredid.skytree.gui.PlayerLookupGUI(this);
                this.playerEditorGUI = new com.wiredid.skytree.gui.PlayerEditorGUI(this);
                this.jobGUI = new JobGUI(this, jobService);
                this.settingsMainGUI = new com.wiredid.skytree.gui.SettingsMainGUI(this);
                this.islandSettingsGUI = new com.wiredid.skytree.gui.IslandSettingsGUI(this);
                this.teamGUI = new com.wiredid.skytree.gui.TeamGUI(this);
                this.universalTabCompleter = new com.wiredid.skytree.command.UniversalTabCompleter(); // Init
                                                                                                      // TabCompleter

                // Set GUI relationships
                this.adminDashboardGUI.setGuis(playerLookupGUI, playerEditorGUI);

                this.rankOverviewGUI = new com.wiredid.skytree.gui.RankOverviewGUI(this);
                this.dailyRewardsSystem = new com.wiredid.skytree.system.DailyRewardsSystem(this);
                this.dailyRewardsGUI = new com.wiredid.skytree.gui.DailyRewardsGUI(this, dailyRewardsSystem);

                this.islandQuestGUI = new com.wiredid.skytree.gui.IslandQuestGUI(this);

                getLogger().info("[OK] Machine processor and utility services initialized");
            } catch (Exception e) {
                getLogger().severe("[ERROR] Failed to initialize machine processor: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            // Fishing System
            try {
                this.rodStorage = new com.wiredid.skytree.fishing.RodStorage(persistenceService);
                this.fishingService = new FishingService(this, persistenceService);
                // Initialize FishStorage
                this.fishStorage = new com.wiredid.skytree.fishing.FishStorage(getDataFolder());
                this.enchantService = new EnchantService(this, economyService, fishingService);

                this.fishStorageGUI = new com.wiredid.skytree.fishing.gui.FishStorageGUI(this, fishStorage,
                        economyService);
                this.fishPriceGuideGUI = new com.wiredid.skytree.fishing.gui.FishPriceGuideGUI(this, fishingService);
                this.fishShopGUI = new com.wiredid.skytree.fishing.gui.FishShopGUI(this, fishingService, rodStorage,
                        economyService);
                this.fishSellGUI = new com.wiredid.skytree.fishing.gui.FishSellGUI(this, fishingService,
                        economyService);

                // Global Custom Enchants Framework
                this.globalEnchantRegistry = new com.wiredid.skytree.system.EnchantRegistry(this);
                this.globalEnchantListener = new com.wiredid.skytree.listener.EnchantListener(this,
                        globalEnchantRegistry);

                // Register initial enchants
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.AbidingEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.HasteEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.TelepathyEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ExperienceEnchant());

                // Combat Batch 1 & 2
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.CombatEnchants.LifestealEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.ArmoredEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.MoltenEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.CombatEnchants.BlindnessEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.PoisonEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.WitherEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.VampireEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.ExecuteEnchant());

                // Utility Batch 1 & 2
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.UtilityEnchants.VeinMinerEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.UtilityEnchants.TrenchEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.UtilityEnchants.AutoReelEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.UtilityEnchants.AutoSmeltEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.UtilityEnchants.ReplenishEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.UtilityEnchants.LumberjackEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.UtilityEnchants.MagnetEnchant());

                // Armor Batch 2
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.ReflectEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.GearsEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.SpringsEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.OxygenEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.ArmorEnchants.NightVisionEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.FlightEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.DrillerEnchant());

                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.BleedEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.CombatEnchants.CriticalEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.CombatEnchants.DoubleStrikeEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.DisarmEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.IceColdEnchant());

                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.UtilityEnchants.ExperiencePlusEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.UtilityEnchants.FortunePlusEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.UtilityEnchants.TimberEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.UtilityEnchants.GrowthEnchant());

                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.ArmorEnchants.SaturationEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.ArmorEnchants.HealthBoostEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.ArmorEnchants.ResistanceEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.CactusEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.BurnEnchant());

                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.BowEnchants.DoubleShotEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.BowEnchants.ExplosiveEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.BowEnchants.LightningEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.BowEnchants.PullEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.BowEnchants.VenomEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.BowEnchants.TeleportEnchant());

                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.FearEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.CombatEnchants.LevitationEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.RageEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.CleaveEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.StunEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.CombatEnchants.ConfuseEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.CombatEnchants.BountyHunterEnchant());

                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.ArmorEnchants.InvisibilityEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.ArmorEnchants.ForceFieldEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.SonarEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.AngelEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.BerserkEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.HeavyEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.ArmorEnchants.GuardianEnchant());

                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.BowEnchants.WebEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.BowEnchants.SniperEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.BowEnchants.MultishotEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.BowEnchants.FlareEnchant());

                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.UtilityEnchants.AutoRepairEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.UtilityEnchants.DrillEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.UtilityEnchants.NautilusEnchant());
                globalEnchantRegistry.register(new com.wiredid.skytree.system.enchants.UtilityEnchants.GreedEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.UtilityEnchants.BlessedEnchant());

                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.CombatEnchants.DragonBreathEnchant());
                globalEnchantRegistry
                        .register(new com.wiredid.skytree.system.enchants.ArmorEnchants.ImmortalityEnchant());

                for (com.wiredid.skytree.fishing.FishingModels.RodEnchant re : com.wiredid.skytree.fishing.FishingModels.RodEnchant
                        .values()) {
                    globalEnchantRegistry.register(
                            new com.wiredid.skytree.system.enchants.FishingRodEnchants.GenericFishingEnchant(re));
                }

                getLogger().info("[OK] Fishing system and Global Enchants initialized");
            } catch (Exception e) {
                getLogger().warning("[ERROR] Failed to initialize fishing/enchant systems: " + e.getMessage());
                e.printStackTrace();
            }

            this.ordersChatListener = new com.wiredid.skytree.listener.OrdersChatListener(this);
            this.minionSkinGUI = new com.wiredid.skytree.gui.MinionSkinGUI(this, minionService);
            this.shopGUIListener = new com.wiredid.skytree.listener.ShopGUIListener(this, economyService, itemRegistry);
            this.sellGUIListener = new SellGUIListener(this, economyService);
            this.worthVisualSystem = new com.wiredid.skytree.system.WorthVisualSystem(this);
            this.worthTooltipPacketSystem = new com.wiredid.skytree.system.WorthTooltipPacketSystem(this);
            this.worthTooltipPacketSystem.enable();
            this.gachaGUIListener = new GachaGUIListener(this);

            // Register commands and listeners
            try {
                registerCommands();
                getLogger().info("[OK] Commands registered");
            } catch (Exception e) {
                getLogger().severe("[ERROR] Failed to register commands: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            try {
                registerListeners();
                getLogger().info("[OK] Listeners registered");
            } catch (Exception e) {
                getLogger().severe("[ERROR] Failed to register listeners: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            // Start energy transfer
            try {
                startEnergyTransfer();
                getLogger().info("[OK] Energy transfer started");
            } catch (Exception e) {
                getLogger().warning("[ERROR] Failed to start energy transfer: " + e.getMessage());
                e.printStackTrace();
            }

            // Start thirst system
            try {
                thirstService.startTask();
                getLogger().info("[OK] Thirst system started");
            } catch (Exception e) {
                getLogger().warning("[ERROR] Failed to start thirst system: " + e.getMessage());
                e.printStackTrace();
            }

            // Start passive shard income (10 shards per minute)
            try {
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                            shardService.addShards(player.getUniqueId(), 10);
                        }
                    }
                }.runTaskTimer(this, 1200L, 1200L); // 1200 ticks = 60 seconds = 1 minute
                getLogger().info("[OK] Passive shard income started (10 shards/min)");
            } catch (Exception e) {
                getLogger().warning("[ERROR] Failed to start passive shard income: " + e.getMessage());
                e.printStackTrace();
            }

            getLogger().info("========================================");
            getLogger().info("[OK] Skytree initialized successfully!");
            getLogger().info("========================================");

        } catch (Exception e) {
            getLogger().severe("========================================");
            getLogger().severe("[ERROR] CRITICAL ERROR: Plugin failed to load!");
            getLogger().severe("Error: " + e.getMessage());
            getLogger().severe("========================================");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initializeWorld() {
        String worldName = getConfig().getString("world.name", "skytree_world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            getLogger().info("Creating void world: " + worldName);
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new VoidChunkGenerator());
            creator.createWorld();
            getLogger().info("World created successfully!");
        }
    }

    private void registerCommands() {
        // Main command
        SkytreeCommand mainCmd = new SkytreeCommand(this);
        safeRegisterCommand("skytree", mainCmd, mainCmd);

        // Shop & Economy
        ShopCommand shopCmd = new ShopCommand(shopService);
        safeRegisterCommand("shop", shopCmd, shopCmd);

        SellCommand sellCmd = new SellCommand(this, sellGUIListener);
        safeRegisterCommand("sg", sellCmd, sellCmd);

        // Cosmetics
        GlowCommand glowCmd = new GlowCommand(this);
        safeRegisterCommand("glow", glowCmd, glowCmd);

        TagCommand tagCmd = new TagCommand(this);
        safeRegisterCommand("tags", tagCmd, tagCmd);
        safeRegisterCommand("sell", sellCmd, sellCmd);

        // Auction House
        com.wiredid.skytree.command.AuctionHouseCommand ahCmd = new com.wiredid.skytree.command.AuctionHouseCommand(
                this);
        safeRegisterCommand("ah", ahCmd, ahCmd);
        OrdersCommand ordersCmd = new OrdersCommand(this);
        safeRegisterCommand("orders", ordersCmd, ordersCmd);

        // Island Menu
        this.islandMenuGUI = new com.wiredid.skytree.listener.IslandMenuGUI(this);

        // Economy commands
        BalanceCommand balCmd = new BalanceCommand(economyService);
        safeRegisterCommand("bal", balCmd, balCmd);
        PayCommand payCmd = new PayCommand(economyService);
        safeRegisterCommand("pay", payCmd, payCmd);
        BaltopCommand baltopCmd = new BaltopCommand(economyService);
        safeRegisterCommand("baltop", baltopCmd, baltopCmd);

        // Chat shortcuts
        com.wiredid.skytree.command.ChatToggleCommand chatToggleCmd = new com.wiredid.skytree.command.ChatToggleCommand(
                this);
        safeRegisterCommand("ic", chatToggleCmd, null);
        safeRegisterCommand("lc", chatToggleCmd, null);
        safeRegisterCommand("gc", chatToggleCmd, null);
        safeRegisterCommand("chathist", chatToggleCmd, null);

        // Admin balance commands
        safeRegisterCommand("setbal", balCmd, balCmd);
        safeRegisterCommand("addbal", balCmd, balCmd);

        // Hub command
        HubCommand hubCmd = new HubCommand();
        safeRegisterCommand("hub", hubCmd, hubCmd);

        // Admin give command
        GiveCommand giveCmd = new GiveCommand((SkytreeItemRegistry) itemRegistry);
        safeRegisterCommand("skytree_give", giveCmd, giveCmd);

        // Crates command
        com.wiredid.skytree.command.CratesCommand cratesCmd = new com.wiredid.skytree.command.CratesCommand(this);
        safeRegisterCommand("crates", cratesCmd, cratesCmd);

        // Warp command
        WarpCommand warpCmd = new WarpCommand(this);
        safeRegisterCommand("warp", warpCmd, warpCmd);

        RTPCommand rtpCmd = new RTPCommand(this);
        safeRegisterCommand("rtp", rtpCmd, rtpCmd);

        // QoL Commands
        com.wiredid.skytree.command.QuestCommand questCmd = new com.wiredid.skytree.command.QuestCommand(this);
        safeRegisterCommand("quest", questCmd, questCmd);
        HomeCommand homeCmd = new HomeCommand(this);
        safeRegisterCommand("sethome", homeCmd, homeCmd);
        safeRegisterCommand("home", homeCmd, homeCmd);
        safeRegisterCommand("delhome", homeCmd, homeCmd);

        SettingsCommand settingsCmd = new SettingsCommand(this);
        safeRegisterCommand("settings", settingsCmd, settingsCmd);

        TpaService tpaService = new TpaService(this);
        TpaCommand tpaCommand = new TpaCommand(this, tpaService);
        safeRegisterCommand("tpa", tpaCommand, tpaCommand);
        safeRegisterCommand("tpaccept", tpaCommand, tpaCommand);
        safeRegisterCommand("tpdeny", tpaCommand, tpaCommand);

        // Guide Command
        KitCommand kitCmd = new KitCommand(this);
        safeRegisterCommand("kits", kitCmd, kitCmd);

        // Guide Command
        GuideCommand guideCmd = new GuideCommand(this, (SkytreeItemRegistry) itemRegistry);
        safeRegisterCommand("guide", guideCmd, guideCmd);

        com.wiredid.skytree.command.ItemsCommand itemsCmd = new com.wiredid.skytree.command.ItemsCommand(this, itemRegistry, recipeService, worthService);
        safeRegisterCommand("items", itemsCmd, itemsCmd);

        VaultCommand vaultCmd = new com.wiredid.skytree.command.VaultCommand(this);
        safeRegisterCommand("vault", vaultCmd, vaultCmd);
        SortCommand sortCmd = new com.wiredid.skytree.command.SortCommand(this);
        safeRegisterCommand("sort", sortCmd, sortCmd);
        // Bank Commands
        BankCommand bankCmd = new BankCommand(this, bankService);
        safeRegisterCommand("bank", bankCmd, bankCmd);
        BankAdminCommand bankAdminCmd = new BankAdminCommand(this, bankService);
        safeRegisterCommand("bankadmin", bankAdminCmd, bankAdminCmd);

        // History Command
        HistoryCommand histCmd = new HistoryCommand(this);
        safeRegisterCommand("history", histCmd, universalTabCompleter);
        safeRegisterCommand("transactions", histCmd, universalTabCompleter);

        // Admin shortcut
        AdminCommand adminShortCmd = new AdminCommand(this);
        safeRegisterCommand("admin", adminShortCmd, universalTabCompleter);

        // Rank Commands
        RankCommand rankCmd = new RankCommand(this);
        safeRegisterCommand("rank", rankCmd, rankCmd);
        safeRegisterCommand("ranks", rankCmd, rankCmd);

        RankUtilityCommands rankUtilCmd = new RankUtilityCommands(this);
        safeRegisterCommand("feed", rankUtilCmd, rankUtilCmd); // Uses built-in completer now
        safeRegisterCommand("heal", rankUtilCmd, rankUtilCmd);
        safeRegisterCommand("fly", rankUtilCmd, rankUtilCmd);
        safeRegisterCommand("nick", rankUtilCmd, rankUtilCmd);

        // Daily Command
        DailyCommand dailyCmd = new DailyCommand(this);
        safeRegisterCommand("daily", dailyCmd, universalTabCompleter);
        safeRegisterCommand("rewards", dailyCmd, universalTabCompleter);

        // Island Shop Command
        com.wiredid.skytree.command.IslandShopCommand islandShopCmd = new com.wiredid.skytree.command.IslandShopCommand(
                islandShopService);
        safeRegisterCommand("islandshop", islandShopCmd, null);

        // Chat System
        this.chatListener = new com.wiredid.skytree.listener.ChatListener(this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.PrivateMessageListener(this),
                this);

        ChannelCommand channelCmd = new ChannelCommand(this, chatListener);
        safeRegisterCommand("chat", channelCmd, universalTabCompleter);

        EnchantCommand ceCmd = new EnchantCommand(this, globalEnchantRegistry);
        safeRegisterCommand("ce", ceCmd, ceCmd);
        safeRegisterCommand("enchants", ceCmd, ceCmd);

        MinionCommand minionCmd = new MinionCommand(this, minionService);
        safeRegisterCommand("minion", minionCmd, minionCmd);

        TrashCommand trashCmd = new TrashCommand(this);
        safeRegisterCommand("trash", trashCmd, trashCmd);
        getServer().getPluginManager().registerEvents(trashCmd, this);

        // Jobs Command
        safeRegisterCommand("jobs", (sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                jobGUI.open((org.bukkit.entity.Player) sender);
            }
            return true;
        }, null);

        // Economy Admin Command
        EconomyAdminCommand econAdminCmd = new EconomyAdminCommand(this);
        safeRegisterCommand("econadmin", econAdminCmd, econAdminCmd);

        // Job Admin Command
        JobAdminCommand jobAdminCmd = new JobAdminCommand(this, jobService);
        safeRegisterCommand("jobadmin", jobAdminCmd, jobAdminCmd);

        // Bounty Command
        com.wiredid.skytree.command.BountyCommand bountyCmd = new com.wiredid.skytree.command.BountyCommand(this);
        safeRegisterCommand("bounty", bountyCmd, bountyCmd);

        // Shard Command
        ShardCommand shardCmd = new ShardCommand(this);
        safeRegisterCommand("shards", shardCmd, shardCmd);

        // Leaderboard Command
        LeaderboardCommand lbCmd = new com.wiredid.skytree.command.LeaderboardCommand(this);
        safeRegisterCommand("leaderboard", lbCmd, lbCmd);

        FishCommand fishCmd = new FishCommand(this, fishStorageGUI);
        safeRegisterCommand("fish", fishCmd, fishCmd);
        RodCommand rodCmd = new RodCommand(this, fishingService, rodStorage, enchantService);
        safeRegisterCommand("rod", rodCmd, rodCmd);

        safeRegisterCommand("bait", new com.wiredid.skytree.command.BaitCommand(this), null);

        // Main Menu Command
        MenuCommand menuCmd = new MenuCommand(this);
        safeRegisterCommand("menu", menuCmd, universalTabCompleter);
        safeRegisterCommand("profile", menuCmd, universalTabCompleter); // Alias

        this.tabSystem = new TabSystem(this);
        this.trustGUI = new com.wiredid.skytree.gui.TrustGUI(this);
        TrustCommand trustCmd = new TrustCommand(this);
        safeRegisterCommand("trust", trustCmd, trustCmd);
        safeRegisterCommand("untrust", trustCmd, trustCmd);
        safeRegisterCommand("trustlist", trustCmd, trustCmd);
    }

    private void safeRegisterCommand(String name, org.bukkit.command.CommandExecutor executor,
            org.bukkit.command.TabCompleter tabCompleter) {
        org.bukkit.command.PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().warning("Could not register command /" + name + ": Missing from plugin.yml!");
            return;
        }
        cmd.setExecutor(executor);
        if (tabCompleter != null) {
            cmd.setTabCompleter(tabCompleter);
        }
    }

    private void registerListeners() {
        if (listenersRegistered) {
            getLogger().warning("Attempted to register listeners twice! Skipping.");
            return;
        }
        listenersRegistered = true;
        getLogger().info("Registering all plugin listeners...");

        // Mechanics & Tech
        getServer().getPluginManager().registerEvents(
                new com.wiredid.skytree.listener.MechanicsListener(this, itemRegistry, recipeService, multiblockService,
                        machineProcessor, worthService, jobService),
                this);
        getServer().getPluginManager().registerEvents(
                new com.wiredid.skytree.listener.CustomCraftingListener(this, itemRegistry),
                this);
        getServer().getPluginManager().registerEvents(new PipeListener(pipeService, itemRegistry), this);
        getServer().getPluginManager().registerEvents(new StorageGUIListener(storageService), this);

        // Core Player GUIs
        if (mainMenuGUI != null)
            getServer().getPluginManager().registerEvents(mainMenuGUI, this);

        // Use fields for GUI listeners
        getServer().getPluginManager().registerEvents(shopGUIListener, this);
        getServer().getPluginManager().registerEvents(sellGUIListener, this);

        getServer().getPluginManager().registerEvents(
                new com.wiredid.skytree.listener.IslandGUIListener(this, islandService, economyService,
                        persistenceService),
                this);
        getServer().getPluginManager().registerEvents(new QuestGUIListener(this, questSystem), this);
        getServer().getPluginManager().registerEvents(new QuestEventListener(this, questSystem), this);
        getServer().getPluginManager()
                .registerEvents(new com.wiredid.skytree.listener.IslandQuestListener(islandQuestService), this);
        getServer().getPluginManager().registerEvents(islandQuestGUI, this);
        getServer().getPluginManager().registerEvents(new QuantityGUIListener(this, economyService, itemRegistry),
                this);
        getServer().getPluginManager().registerEvents(confirmationGUI, this);
        getServer().getPluginManager().registerEvents(warpGUI, this);

        // Market & Auction House
        getServer().getPluginManager().registerEvents(auctionHouseGUI, this);
        getServer().getPluginManager().registerEvents(auctionPurchaseGUI, this);
        getServer().getPluginManager().registerEvents(ordersGUI, this);
        getServer().getPluginManager().registerEvents(orderSetupGUI, this);
        getServer().getPluginManager().registerEvents(orderFulfillmentGUI, this);
        getServer().getPluginManager().registerEvents(bountyGUI, this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.BountyListener(this), this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.BountyChatListener(this), this);

        // Social & Teams
        getServer().getPluginManager().registerEvents(teamBankGUI, this);
        getServer().getPluginManager()
                .registerEvents(new com.wiredid.skytree.listener.BankGUIListener(this, bankService), this);
        getServer().getPluginManager().registerEvents(chatHistoryGUI, this);

        // Admin & Systems
        getServer().getPluginManager().registerEvents(adminDashboardGUI, this);
        getServer().getPluginManager().registerEvents(adminLogsGUI, this);
        getServer().getPluginManager().registerEvents(playerLookupGUI, this);
        getServer().getPluginManager().registerEvents(playerEditorGUI, this);
        getServer().getPluginManager().registerEvents(rankOverviewGUI, this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.RankListener(this), this);
        getServer().getPluginManager().registerEvents(performanceMonitorGUI, this);
        getServer().getPluginManager().registerEvents(upgradeGUI, this);
        getServer().getPluginManager().registerEvents(investmentGUI, this);
        getServer().getPluginManager().registerEvents(dailyRewardsGUI, this);
        getServer().getPluginManager().registerEvents(tabSystem, this);
        getServer().getPluginManager().registerEvents(trustGUI, this);

        // Farming & Machines
        getServer().getPluginManager().registerEvents(new EssenceGrowingListener(this, itemRegistry), this);
        getServer().getPluginManager().registerEvents(new CustomCropService(this, itemRegistry), this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.MachineGUIListener(
                machineService, recipeService, itemRegistry, machineProcessor), this);
        getServer().getPluginManager().registerEvents(
                new com.wiredid.skytree.listener.MinionListener(this, minionService, itemRegistry), this);
        getServer().getPluginManager()
                .registerEvents(new com.wiredid.skytree.listener.MinionGUIListener(this, minionService), this);
        getServer().getPluginManager()
                .registerEvents(new ItemTransportListener(this, itemRegistry, itemTransportService), this);

        // Fishing & Enchants
        getServer().getPluginManager().registerEvents(new FishingListener(fishingService, fishStorage), this);
        getServer().getPluginManager().registerEvents(fishStorageGUI, this);
        getServer().getPluginManager().registerEvents(fishPriceGuideGUI, this);
        getServer().getPluginManager().registerEvents(fishShopGUI, this);
        getServer().getPluginManager().registerEvents(fishSellGUI, this);
        getServer().getPluginManager().registerEvents(new CustomAnvilListener(globalEnchantRegistry), this);
        getServer().getPluginManager().registerEvents(new LegendaryEffectListener(this), this);
        getServer().getPluginManager().registerEvents(globalEnchantListener, this);

        // Island Logic & Protection
        getServer().getPluginManager().registerEvents(new IslandProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.IslandBorderListener(this),
                this);
        getServer().getPluginManager()
                .registerEvents(new com.wiredid.skytree.listener.IslandLogListener(islandService, actionLogger), this);
        getServer().getPluginManager()
                .registerEvents(new com.wiredid.skytree.listener.PortalListener(this, islandService), this);

        // Multi-Inventory & Safety Features
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.WorldInventoryListener(this),
                this);
        this.islandSafetyListener = new com.wiredid.skytree.listener.IslandSafetyListener(this);
        getServer().getPluginManager().registerEvents(islandSafetyListener, this);

        // Custom Ore Generator
        getServer().getPluginManager()
                .registerEvents(new com.wiredid.skytree.listener.CustomOreGeneratorListener(this), this);

        // Machine GUIs (previously self-registering)
        getServer().getPluginManager()
                .registerEvents(new com.wiredid.skytree.gui.AutoCrafterGUI(this, machineProcessor), this);
        getServer().getPluginManager()
                .registerEvents(new com.wiredid.skytree.gui.MachineUpgradeGUI(this, machineProcessor), this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.gui.MinionStorageGUI(this, minionService),
                this);

        com.wiredid.skytree.gui.IslandShopGUI islandShopGUI = new com.wiredid.skytree.gui.IslandShopGUI(this,
                economyService);
        getServer().getPluginManager().registerEvents(
                new com.wiredid.skytree.listener.IslandShopListener(islandShopService, islandShopGUI), this);
        getServer().getPluginManager().registerEvents(
                new com.wiredid.skytree.listener.IslandShopGUIListener(this, islandShopService, economyService), this);

        // Utilities & Misc
        com.wiredid.skytree.listener.CatalogListener catalogListener =
                new com.wiredid.skytree.listener.CatalogListener(this, itemRegistry, worthService);
        getServer().getPluginManager().registerEvents(catalogListener, this);
        getServer().getPluginManager().registerEvents(new GuideListener(this, itemRegistry, catalogListener), this);
        getServer().getPluginManager().registerEvents(recipeBrowserGUI, this);
        getServer().getPluginManager().registerEvents(shardShopGUI, this);
        getServer().getPluginManager().registerEvents(leaderboardGUI, this);
        getServer().getPluginManager().registerEvents(homeGUI, this);
        getServer().getPluginManager().registerEvents(playerSettingsGUI, this);
        getServer().getPluginManager().registerEvents(settingsMainGUI, this);
        getServer().getPluginManager().registerEvents(jobGUI, this);
        getServer().getPluginManager().registerEvents(islandSettingsGUI, this);
        getServer().getPluginManager().registerEvents(ordersChatListener, this);
        getServer().getPluginManager().registerEvents(minionSkinGUI, this);
        getServer().getPluginManager().registerEvents(tagGUI, this);
        getServer().getPluginManager().registerEvents(crateOpeningGUI, this);
        getServer().getPluginManager().registerEvents(baitGUI, this);
        getServer().getPluginManager().registerEvents(new ThirstListener(thirstService), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.SpawnerListener(this), this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.ItemStackListener(this), this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.VaultListener(this), this);
        getServer().getPluginManager().registerEvents(new JobListener(this, jobService), this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.TrashFilterListener(this), this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.CrystalDamageListener(this),
                this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.EventListener(eventManager),
                this);
        getServer().getPluginManager().registerEvents(new RecipeUnlockListener(mythicItemManager), this);
        getServer().getPluginManager().registerEvents(
                new MythicItemEffectListener(this, (com.wiredid.skytree.impl.SkytreeItemRegistry) itemRegistry), this);

        if (gachaGUIListener != null)
            getServer().getPluginManager().registerEvents(gachaGUIListener, this);

        // Register Worth System Listeners
        if (worthVisualSystem != null) {
            getServer().getPluginManager().registerEvents(worthVisualSystem, this);
        }
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.listener.WorthListener(this, worthService), this);

        getServer().getPluginManager().registerEvents(chatListener, this);
        getServer().getPluginManager()
                .registerEvents(new com.wiredid.skytree.listener.GiveGUIListener(this, itemRegistry), this);
        getServer().getPluginManager().registerEvents(islandMenuGUI, this);

        // Kit GUI
        org.bukkit.command.PluginCommand kitCmd = getCommand("kits");
        if (kitCmd != null && kitCmd.getExecutor() instanceof com.wiredid.skytree.command.KitCommand) {
            getServer().getPluginManager().registerEvents(
                    new com.wiredid.skytree.gui.KitGUIListener(this, economyService,
                            (com.wiredid.skytree.command.KitCommand) kitCmd.getExecutor()),
                    this);
        }

        // Add Scoreboard logic as anonymous listener
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                if (scoreboardSystem != null) {
                    if (persistenceService.loadPlayerData(e.getPlayer().getUniqueId()).getSettings()
                            .getOrDefault("scoreboard", true)) {
                        scoreboardSystem.setupScoreboard(e.getPlayer());
                    } else {
                        scoreboardSystem.removeScoreboard(e.getPlayer());
                    }
                }
            }

            @org.bukkit.event.EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
                if (scoreboardSystem != null)
                    scoreboardSystem.removeScoreboard(e.getPlayer());
            }
        }, this);

        // Legacy & missing
        getServer().getPluginManager()
                .registerEvents(new com.wiredid.skytree.listener.SellWandListener(this, sellGUIListener), this);
        getServer().getPluginManager()
                .registerEvents(new com.wiredid.skytree.gui.BulkBuyGUI(this, economyService, itemRegistry), this);
        getServer().getPluginManager()
                .registerEvents(new com.wiredid.skytree.gui.BulkSellGUI(this, economyService, itemRegistry), this);
        getServer().getPluginManager().registerEvents(new com.wiredid.skytree.gui.MaterialSelectorGUI(this), this);
    }

    private void startEnergyTransfer() {
        // Energy transfer every tick (20 times per second)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            energySystem.transferEnergy();
        }, 1L, 1L);
    }

    // Accessors
    public com.wiredid.skytree.gui.SettingsMainGUI getSettingsMainGUI() {
        return settingsMainGUI;
    }

    public com.wiredid.skytree.gui.IslandSettingsGUI getIslandSettingsGUI() {
        return islandSettingsGUI;
    }

    // Core Service Getters
    public IslandService getIslandService() {
        return islandService;
    }

    public IslandSettingsService getIslandSettingsService() {
        return settingsService;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public ShopService getShopService() {
        return shopService;
    }

    public com.wiredid.skytree.api.ShardService getShardService() {
        return shardService;
    }

    public com.wiredid.skytree.gui.ConfirmationGUI getConfirmationGUI() {
        return confirmationGUI;
    }

    public com.wiredid.skytree.api.PlaytimeService getPlaytimeService() {
        return playtimeService;
    }

    public com.wiredid.skytree.api.BountyService getBountyService() {
        return bountyService;
    }

    public com.wiredid.skytree.api.LeaderboardService getLeaderboardService() {
        return leaderboardService;
    }

    public com.wiredid.skytree.gui.BountyGUI getBountyGUI() {
        return bountyGUI;
    }

    public com.wiredid.skytree.gui.ShardShopGUI getShardShopGUI() {
        return shardShopGUI;
    }

    public com.wiredid.skytree.gui.LeaderboardGUI getLeaderboardGUI() {
        return leaderboardGUI;
    }

    public com.wiredid.skytree.gui.HomeGUI getHomeGUI() {
        return homeGUI;
    }

    public com.wiredid.skytree.listener.IslandSafetyListener getIslandSafetyListener() {
        return islandSafetyListener;
    }

    public com.wiredid.skytree.gui.PlayerSettingsGUI getPlayerSettingsGUI() {
        return playerSettingsGUI;
    }

    public AuctionHouseService getAuctionHouseService() {
        return auctionHouseService;
    }

    public PersistenceService getPersistenceService() {
        return persistenceService;
    }

    public com.wiredid.skytree.api.WorthService getWorthService() {
        return worthService;
    }

    public com.wiredid.skytree.system.WorthVisualSystem getWorthVisualSystem() {
        return worthVisualSystem;
    }

    public com.wiredid.skytree.system.WorthTooltipPacketSystem getWorthTooltipPacketSystem() {
        return worthTooltipPacketSystem;
    }

    public MachineService getMachineService() {
        return machineService;
    }

    public IslandSettingsService getSettingsService() {
        return settingsService;
    }

    public RecipeService getRecipeService() {
        return recipeService;
    }

    public MultiblockService getMultiblockService() {
        return multiblockService;
    }

    public PipeService getPipeService() {
        return pipeService;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public AchievementSystem getAchievementSystem() {
        return achievementSystem;
    }

    public QuestSystem getQuestSystem() {
        return questSystem;
    }

    public EnergySystem getEnergySystem() {
        return energySystem;
    }

    public MachineProcessor getMachineProcessor() {
        return machineProcessor;
    }

    public ScoreboardSystem getScoreboardSystem() {
        return scoreboardSystem;
    }

    public com.wiredid.skytree.gui.WarpGUI getWarpGUI() {
        return warpGUI;
    }

    public com.wiredid.skytree.gui.RecipeBrowserGUI getRecipeBrowserGUI() {
        return recipeBrowserGUI;
    }

    public com.wiredid.skytree.gui.AuctionHouseGUI getAuctionHouseGUI() {
        return auctionHouseGUI;
    }

    public com.wiredid.skytree.gui.AuctionPurchaseGUI getAuctionPurchaseGUI() {
        return auctionPurchaseGUI;
    }

    public com.wiredid.skytree.gui.OrdersGUI getOrdersGUI() {
        return ordersGUI;
    }

    public com.wiredid.skytree.gui.OrderFulfillmentGUI getOrderFulfillmentGUI() {
        return orderFulfillmentGUI;
    }

    public com.wiredid.skytree.gui.OrderSetupGUI getOrderSetupGUI() {
        return orderSetupGUI;
    }

    public MythicItemManager getMythicItemManager() {
        return mythicItemManager;
    }

    public RankService getRankService() {
        return rankService;
    }

    public AdminService getAdminService() {
        return adminService;
    }

    public GachaService getGachaService() {
        return gachaService;
    }

    public ThirstService getThirstService() {
        return thirstService;
    }

    public FishingService getFishingService() {
        return fishingService;
    }

    public EnchantService getEnchantService() {
        return enchantService;
    }

    public com.wiredid.skytree.fishing.gui.FishPriceGuideGUI getFishPriceGuideGUI() {
        return fishPriceGuideGUI;
    }

    public com.wiredid.skytree.fishing.gui.FishShopGUI getFishShopGUI() {
        return fishShopGUI;
    }

    public com.wiredid.skytree.fishing.gui.FishSellGUI getFishSellGUI() {
        return fishSellGUI;
    }

    public com.wiredid.skytree.listener.IslandMenuGUI getIslandMenuGUI() {
        return islandMenuGUI;
    }

    public com.wiredid.skytree.gui.TeamGUI getTeamGUI() {
        return teamGUI;
    }

    @Override
    public void onDisable() {
        if (machineProcessor != null) {
            machineProcessor.saveMachines();
        }
        if (itemTransportService instanceof com.wiredid.skytree.impl.SkytreeItemTransportService) {
            ((com.wiredid.skytree.impl.SkytreeItemTransportService) itemTransportService).saveNow();
        }
        if (thirstService != null) {
            thirstService.stopTask();
        }
        if (fishStorage != null) {
            fishStorage.saveNow();
        }
        if (persistenceService != null) {
            persistenceService.shutdown();
        }
        if (worthTooltipPacketSystem != null) {
            worthTooltipPacketSystem.disable();
        }
        if (minionService instanceof com.wiredid.skytree.impl.SkytreeMinionService) {
            ((com.wiredid.skytree.impl.SkytreeMinionService) minionService).onDisable();
        }
        if (economyManager != null) {
            economyManager.onDisable();
        }
        if (jobService != null) {
            jobService.onDisable();
        }
        getLogger().info("Skytree disabled.");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new VoidChunkGenerator();
    }

    public void startBountyCreation(org.bukkit.entity.Player player) {
        waitingForBountyItem.add(player.getUniqueId());
        player.sendMessage("§e§l[Bounty] §7Type the item name or ID in chat. §8(60s timeout)");

        org.bukkit.scheduler.BukkitTask task = getServer().getScheduler().runTaskLater(this, () -> {
            if (waitingForBountyItem.contains(player.getUniqueId())) {
                waitingForBountyItem.remove(player.getUniqueId());
                bountyTimeouts.remove(player.getUniqueId());
                player.sendMessage("§c§l[Bounty] §7Creation timed out.");
            }
        }, getConfig().getLong("chat.timeout_seconds", 60) * 20L);

        org.bukkit.scheduler.BukkitTask old = bountyTimeouts.put(player.getUniqueId(), task);
        if (old != null)
            old.cancel();
    }

    public boolean isWaitingForBountyItem(org.bukkit.entity.Player player) {
        return waitingForBountyItem.contains(player.getUniqueId());
    }

    public void stopBountyCreation(org.bukkit.entity.Player player) {
        waitingForBountyItem.remove(player.getUniqueId());
        org.bukkit.scheduler.BukkitTask task = bountyTimeouts.remove(player.getUniqueId());
        if (task != null)
            task.cancel();
    }

    public com.wiredid.skytree.api.CrateService getCrateService() {
        return crateService;
    }

    public com.wiredid.skytree.gui.CrateOpeningGUI getCrateOpeningGUI() {
        return crateOpeningGUI;
    }

    public com.wiredid.skytree.gui.ChatHistoryGUI getChatHistoryGUI() {
        return chatHistoryGUI;
    }

    public com.wiredid.skytree.listener.ChatListener getChatListener() {
        return chatListener;
    }

    public com.wiredid.skytree.api.TeamBankService getTeamBankService() {
        return teamBankService;
    }

    public com.wiredid.skytree.gui.TeamBankGUI getTeamBankGUI() {
        return teamBankGUI;
    }

    public com.wiredid.skytree.gui.PerformanceMonitorGUI getPerformanceMonitorGUI() {
        return performanceMonitorGUI;
    }

    public com.wiredid.skytree.system.EventManager getEventManager() {
        return eventManager;
    }

    public com.wiredid.skytree.api.ChatService getChatService() {
        return chatService;
    }

    public com.wiredid.skytree.api.InvestmentService getInvestmentService() {
        return investmentService;
    }

    public com.wiredid.skytree.api.MonetizationService getMonetizationService() {
        return monetizationService;
    }

    public com.wiredid.skytree.gui.InvestmentGUI getInvestmentGUI() {
        return investmentGUI;
    }

    public com.wiredid.skytree.gui.TrustGUI getTrustGUI() {
        return trustGUI;
    }

    public IslandShopService getIslandShopService() {
        return islandShopService;
    }
}
