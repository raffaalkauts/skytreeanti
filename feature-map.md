# Skytree Feature Map

Dokumen ini memetakan fitur per subsystem supaya AI lain bisa cepat mencari lokasi implementasi saat diminta menambah atau memperbaiki sesuatu.

## 1. Core Platform

### Bootstrap

- Entry point: `SkytreePlugin`
- Tugas: initialize config, world, service, command, listener, GUI, dan runtime systems

### World

- Package: `world/`
- Fitur:
  - void world generator
  - skyblock world bootstrap

## 2. Island System

### Fitur

- create island
- home / sethome / delhome
- protection
- trust / untrust / trust list
- island settings
- island warp
- portal / border / safety
- island log
- island quest
- island shop

### Lokasi utama

- `api/IslandService`
- `impl/SkytreeIslandService`
- `api/IslandSettingsService`
- `impl/SkytreeIslandSettingsService`
- `api/IslandShopService`
- `impl/SkytreeIslandShopService`
- `api/IslandQuestService`
- `impl/SkytreeIslandQuestService`
- `listener/IslandProtectionListener`
- `listener/IslandBorderListener`
- `listener/IslandLogListener`
- `listener/IslandSafetyListener`
- `listener/PortalListener`
- `listener/IslandGUIListener`
- `gui/IslandSettingsGUI`
- `gui/IslandQuestGUI`
- `gui/IslandShopGUI`
- `gui/HomeGUI`
- `gui/WarpGUI`

## 3. Economy And Market

### Fitur

- balance
- transfer / pay
- baltop
- shop global
- bulk buy / bulk sell
- sell GUI
- auction house
- buy orders
- quantity selection
- worth display
- transaction history

### Lokasi utama

- `api/EconomyService`
- `impl/SkytreeEconomyService`
- `api/WorthService`
- `impl/SkytreeWorthService`
- `api/ShopService`
- `impl/SkytreeShopService`
- `api/AuctionHouseService`
- `impl/SkytreeAuctionHouseService`
- `gui/ShopGUIListener`
- `listener/ShopGUIListener`
- `listener/SellGUIListener`
- `listener/SellWandListener`
- `gui/BulkBuyGUI`
- `gui/BulkSellGUI`
- `gui/QuantitySelectionGUI`
- `gui/AuctionHouseGUI`
- `gui/AuctionPurchaseGUI`
- `gui/OrdersGUI`
- `gui/OrderSetupGUI`
- `gui/OrderFulfillmentGUI`
- `gui/ConfirmationGUI`
- `gui/TransactionHistoryGUI`

## 4. Banking

### Fitur

- deposit
- withdraw
- transfer
- history
- stats
- admin audit / reconcile

### Lokasi utama

- `banking/BankService`
- `banking/impl/BankServiceImpl`
- `banking/persistence/BankPersistenceService`
- `banking/model/*`
- `banking/util/BankUtil`
- `banking/util/RateLimiter`
- `command/BankCommand`
- `command/BankAdminCommand`
- `gui/BankMainGUI`
- `gui/BankDepositGUI`
- `gui/BankWithdrawGUI`
- `gui/BankHistoryGUI`
- `listener/BankGUIListener`

## 5. Fishing

### Fitur

- rod management
- fish storage
- fish selling
- fish shop
- price guide
- rod enchant
- active rod GUI
- custom fish

### Lokasi utama

- `fishing/FishingService`
- `fishing/EnchantService`
- `fishing/FishStorage`
- `fishing/RodStorage`
- `fishing/CustomFishService`
- `fishing/FishingListener`
- `fishing/gui/FishStorageGUI`
- `fishing/gui/FishSellGUI`
- `fishing/gui/FishShopGUI`
- `fishing/gui/FishPriceGuideGUI`
- `fishing/gui/EnchantGUI`
- `fishing/gui/ActiveRodGUI`
- `fishing/gui/RodExchangeGUI`
- `command/FishCommand`
- `command/RodCommand`

## 6. Machines And Automation

### Fitur

- machine processor
- sieve
- barrel
- crucible
- compressor
- auto crafter
- advanced furnace
- machine upgrades
- machine recipes
- item transport / pipe
- storage controller

### Lokasi utama

- `machine/MachineProcessor`
- `api/MachineService`
- `impl/SkytreeMachineService`
- `api/MultiblockService`
- `api/PipeService`
- `api/ItemTransportService`
- `impl/SkytreeItemTransportService`
- `listener/MachineListener`
- `listener/MachineGUIListener`
- `listener/ItemTransportListener`
- `listener/PipeListener`
- `gui/SieveGUI`
- `gui/BarrelGUI`
- `gui/CrucibleGUI`
- `gui/CompressorGUI`
- `gui/AutoCrafterGUI`
- `gui/AdvancedFurnaceGUI`
- `gui/MachineUpgradeGUI`
- `gui/CobbleGenGUI`
- `gui/StorageGUI`
- `gui/StorageGUIListener`

## 7. Minion And Passive Production

### Fitur

- minion management
- minion storage
- minion skin
- collection / cleanup

### Lokasi utama

- `api/MinionService`
- `impl/SkytreeMinionService`
- `listener/MinionListener`
- `listener/MinionGUIListener`
- `gui/MinionGUI`
- `gui/MinionStorageGUI`
- `gui/MinionSkinGUI`
- `gui/MinionInventoryHolder`
- `minion/MinionTask`
- `command/MinionCommand`

## 8. Progression And Rewards

### Fitur

- quests
- island quests
- achievements
- daily rewards
- ranks
- tags
- leaderboard
- playtime
- shards
- crates
- gacha
- investments

### Lokasi utama

- `system/QuestSystem`
- `system/AchievementSystem`
- `system/DailyRewardsSystem`
- `system/ScoreboardSystem`
- `system/TabSystem`
- `system/EnchantRegistry`
- `impl/SkytreeIslandQuestService`
- `impl/SkytreeLeaderboardService`
- `impl/SkytreeShardService`
- `impl/SkytreeTagService`
- `impl/SkytreeCrateService`
- `impl/SkytreeGachaService`
- `impl/SkytreeInvestmentService`
- `impl/SkytreePlaytimeService`
- `gui/QuestGUI`
- `gui/IslandQuestGUI`
- `gui/DailyRewardsGUI`
- `gui/RankOverviewGUI`
- `gui/LeaderboardGUI`
- `gui/TagGUI`
- `gui/ShardShopGUI`
- `gui/CrateOpeningGUI`
- `gui/GachaGUI`
- `gui/InvestmentGUI`
- `gui/AchievementGUI`
- `command/QuestCommand`
- `command/DailyCommand`
- `command/RankCommand`
- `command/LeaderboardCommand`
- `command/ShardCommand`
- `command/CratesCommand`
- `command/TagCommand`

## 9. Chat And Social

### Fitur

- local chat
- global chat
- island chat
- private message
- chat history
- reactions / chat metadata

### Lokasi utama

- `api/ChatService`
- `impl/SkytreeChatService`
- `listener/ChatListener`
- `listener/PrivateMessageListener`
- `listener/OrdersChatListener`
- `gui/ChatHistoryGUI`
- `command/ChatToggleCommand`
- `command/ChannelCommand`
- `command/HistoryCommand`

## 10. Admin And Tools

### Fitur

- admin dashboard
- admin logs
- player lookup
- player editor
- performance monitor
- guide / help
- give custom items
- sort inventory
- trash filter
- RTP
- hub teleport
- settings

### Lokasi utama

- `api/AdminService`
- `impl/AdminServiceImpl`
- `gui/AdminDashboardGUI`
- `gui/AdminLogsGUI`
- `gui/PlayerLookupGUI`
- `gui/PlayerEditorGUI`
- `gui/PerformanceMonitorGUI`
- `gui/SettingsMainGUI`
- `gui/PlayerSettingsGUI`
- `gui/MainMenuGUI`
- `gui/SkytreeGuide`
- `command/AdminCommand`
- `command/GiveCommand`
- `command/SortCommand`
- `command/TrashCommand`
- `command/RTPCommand`
- `command/HubCommand`
- `command/SettingsCommand`
- `command/GuideCommand`
- `listener/GuideListener`
- `listener/GiveGUIListener`
- `listener/TrashFilterListener`
- `listener/WorthDisplayListener`

## 11. Custom Items And Recipes

### Fitur

- item registry
- custom recipes
- mythic item manager
- custom enchant registry
- recipe unlock
- custom anvil behavior

### Lokasi utama

- `api/ItemRegistry`
- `impl/SkytreeItemRegistry`
- `api/RecipeService`
- `impl/SkytreeRecipeService`
- `impl/MythicItemManager`
- `system/EnchantRegistry`
- `listener/RecipeUnlockListener`
- `listener/CustomAnvilListener`
- `listener/MythicItemEffectListener`
- `system/enchants/*`

## 12. Content Files

Resource files yang biasanya perlu diubah saat menambah fitur:

- `config.yml`
- `plugin.yml`
- `items.yml`
- `shop.yml`
- `worth.yml`
- `quests.yml`
- `island_quests.yml`
- `custom_enchants.yml`
- `custom_fish.yml`
- `crates.yml`
- `baits.yml`
- `machine_recipes.yml`
- `machine_upgrades.yml`
- `minion_types.yml`
- `daily_rewards.yml`
- `investments.yml`
- `tags.yml`
- `tab.yml`
- `chat_config.yml`

## 13. Modifikasi Rule Of Thumb

- kalau fitur menyimpan state, cari service dan model dulu
- kalau fitur memunculkan UI, cari GUI dan listener dulu
- kalau fitur bergantung pada item, cari item registry dan recipe dulu
- kalau fitur lintas sistem, cek wiring di `SkytreePlugin`
