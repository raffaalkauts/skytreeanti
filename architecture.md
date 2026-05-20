# Skytree Architecture

Dokumen ini menjelaskan struktur internal project Skytree supaya AI atau developer lain bisa cepat memahami modul mana yang bertanggung jawab atas bagian tertentu.

## 1. Gambaran Umum

Skytree adalah plugin Minecraft Paper yang dibangun dengan arsitektur modular:

- `SkytreePlugin` sebagai bootstrap utama
- package `api/` untuk kontrak service
- package `impl/` untuk implementasi business logic
- package `listener/` untuk event handling
- package `command/` untuk command executor dan tab completer
- package `gui/` untuk inventory GUI
- package `system/` untuk sistem runtime lintas fitur
- package domain khusus seperti `fishing/`, `banking/`, `machine/`, `model/`, `world/`

Prinsip desain yang terlihat di codebase ini:

- feature-oriented, bukan layered monolith
- banyak subsistem berdiri sendiri, tapi dihubungkan lewat service
- data state disimpan lokal via YAML / resource files
- UI in-game dipakai sebagai front-end utama

## 2. Bootstrap Dan Lifecycle

Entry point utama:

- [SkytreePlugin.java](./src/main/java/com/wiredid/skytree/SkytreePlugin.java)

Urutan startup yang penting:

1. `saveDefaultConfig()`
2. initialize world / void generator
3. initialize persistence service
4. initialize economy, rank, admin service
5. initialize item registry dan recipe service
6. load mythic items
7. initialize gacha
8. initialize service lain
9. register command
10. register listener
11. register GUI dan system runtime

Urutan shutdown juga penting karena beberapa service punya cleanup sendiri.

## 3. Lapisan Arsitektur

### 3.1 API Layer

Folder:

- [src/main/java/com/wiredid/skytree/api](./src/main/java/com/wiredid/skytree/api)

Fungsi:

- mendefinisikan kontrak service
- mengurangi coupling antar modul
- memudahkan ganti implementasi tanpa ubah caller

Contoh interface / kontrak penting:

- `IslandService`
- `EconomyService`
- `ShopService`
- `AuctionHouseService`
- `BountyService`
- `BaitService`
- `ChatService`
- `GachaService`
- `IslandQuestService`
- `MachineService`
- `MinionService`
- `ShardService`
- `TagService`
- `RecipeService`
- `LeaderboardService`
- `PersistenceService`

Beberapa class di `api/` ternyata bukan interface murni, jadi jangan asumsi nama folder selalu berarti interface.

### 3.2 Implementation Layer

Folder:

- [src/main/java/com/wiredid/skytree/impl](./src/main/java/com/wiredid/skytree/impl)

Fungsi:

- berisi logic bisnis inti
- menyimpan state runtime dan integrasi antar service
- menangani load / save data

Contoh implementasi penting:

- `SkytreeIslandService`
- `SkytreeEconomyService`
- `SkytreeShopService`
- `SkytreeAuctionHouseService`
- `SkytreeBountyService`
- `SkytreeChatService`
- `SkytreeGachaService`
- `SkytreeMachineService`
- `SkytreeMinionService`
- `SkytreeRecipeService`
- `SkytreeShardService`
- `SkytreeTagService`
- `YamlPersistenceService`
- `MythicItemManager`

### 3.3 Listener Layer

Folder:

- [src/main/java/com/wiredid/skytree/listener](./src/main/java/com/wiredid/skytree/listener)

Fungsi:

- handle event Bukkit/Paper
- bridge event game ke service layer
- menjaga aturan gameplay

Listener biasanya terbagi menjadi:

- proteksi island
- GUI click handler
- item / inventory interaction
- economy / shop / bank interaction
- chat / bounty / private message
- machine / minion / pipe / transport
- fishing / enchanting
- progression / quest / reward

### 3.4 Command Layer

Folder:

- [src/main/java/com/wiredid/skytree/command](./src/main/java/com/wiredid/skytree/command)

Fungsi:

- command executor
- tab completion
- bridge command ke service / GUI

Command registration dikendalikan dari `SkytreePlugin`.

### 3.5 GUI Layer

Folder:

- [src/main/java/com/wiredid/skytree/gui](./src/main/java/com/wiredid/skytree/gui)

Fungsi:

- semua inventory UI
- pemilihan quantity
- browser, editor, dashboard, confirmation screen
- holder / listener GUI terkait

GUI ini bukan hanya tampilan, tapi sering ikut menyimpan state interaksi user.

### 3.6 System Layer

Folder:

- [src/main/java/com/wiredid/skytree/system](./src/main/java/com/wiredid/skytree/system)

Fungsi:

- mekanik global dan runtime systems
- scoreboard
- tab list
- quest scheduler / tracker
- daily rewards
- achievements
- energy
- visual worth systems
- enchant registry
- event manager

### 3.7 Domain-Specific Subsystems

#### Fishing

Folder:

- [src/main/java/com/wiredid/skytree/fishing](./src/main/java/com/wiredid/skytree/fishing)

Isi:

- `FishingService`
- `EnchantService`
- `FishStorage`
- `RodStorage`
- `CustomFishService`
- GUI fishing

#### Banking

Folder:

- [src/main/java/com/wiredid/skytree/banking](./src/main/java/com/wiredid/skytree/banking)

Isi:

- service bank
- persistence
- model akun / jurnal / transaction result
- util bank / rate limiter

#### Machine

Folder:

- [src/main/java/com/wiredid/skytree/machine](./src/main/java/com/wiredid/skytree/machine)

Isi:

- `MachineProcessor`

Machine logic biasanya bergantung ke item registry, storage, transport, dan GUI.

#### World

Folder:

- [src/main/java/com/wiredid/skytree/world](./src/main/java/com/wiredid/skytree/world)

Isi:

- `VoidChunkGenerator`

## 4. Data Model

Folder:

- [src/main/java/com/wiredid/skytree/model](./src/main/java/com/wiredid/skytree/model)

Fungsi:

- menyimpan enum dan data object
- memisahkan struktur data dari logic

Model yang penting:

- `Island`
- `IslandMember`
- `IslandRole`
- `IslandPermission`
- `IslandMetrics`
- `PlayerData`
- `Rank`
- `QuestPoint`
- `Bounty`
- `Investment`
- `InvestmentType`
- `MinionData`
- `MinionType`
- `MinionSkin`
- `IslandShop`
- `IslandWarps`
- `ShopTransaction`
- `ShardTransaction`
- `ChatMessage`
- `ChatReaction`
- `ActionLog`
- `TrustLevel`

## 5. Dependency Graph Praktis

Pola dependency yang paling umum:

- command -> service / GUI
- listener -> service / GUI / utility
- GUI -> service / model
- system -> service / model / listener
- impl -> api + model + util
- plugin bootstrap -> semua layer

Aturan praktis:

- `api/` dipanggil dari banyak tempat
- `impl/` sebaiknya tidak ikut bergantung pada class GUI kecuali memang untuk wiring
- `listener/` sebaiknya tipis, logic berat pindah ke service
- `SkytreePlugin` jangan terlalu banyak business logic baru, cukup wiring

## 6. Resource Files

Folder resource:

- [src/main/resources](./src/main/resources)

File penting:

- `config.yml`
- `plugin.yml`
- `items.yml`
- `shop.yml`
- `shard_shop.yml`
- `quests.yml`
- `island_quests.yml`
- `daily_rewards.yml`
- `custom_enchants.yml`
- `custom_fish.yml`
- `crates.yml`
- `baits.yml`
- `kits.yml`
- `trust_levels.yml`
- `tags.yml`
- `tab.yml`
- `chat_config.yml`
- `machine_recipes.yml`
- `machine_upgrades.yml`
- `minion_types.yml`
- `investments.yml`
- `worth.yml`
- `mythic_items.json`

## 7. Source Of Truth

Kalau ada konflik informasi, prioritas source of truth adalah:

1. source code di `src/main/java`
2. resource file di `src/main/resources`
3. build config di `pom.xml`
4. log build dan artifact hasil build

Jangan jadikan file `target/` sebagai referensi utama untuk desain.

## 8. Area Yang Paling Sering Berinteraksi

### Island

Berhubungan dengan:

- world generation
- protection
- warp / home
- trust / permissions
- quest
- leaderboards

### Economy

Berhubungan dengan:

- shop
- auction house
- orders
- bank
- bounty
- ranks
- shards
- daily reward

### Item Registry

Berhubungan dengan:

- recipe service
- machine
- minion
- fishing
- custom enchants
- GUI item selection

### Quest / Progression

Berhubungan dengan:

- island quest
- achievement
- daily rewards
- scoreboard / tab
- rewards

## 9. Change Strategy

Jika mau menambah fitur, pilih lokasi perubahan seperti ini:

- perubahan data model -> `model/`
- perubahan contract -> `api/`
- perubahan business logic -> `impl/`
- perubahan event gameplay -> `listener/`
- perubahan tampilan -> `gui/`
- perubahan command routing -> `command/`
- perubahan mekanik global -> `system/`
- perubahan world behavior -> `world/`

Kalau bug melibatkan lebih dari satu layer, fix harus ditaruh di layer paling bawah yang masih punya konteks cukup.

## 10. Hal Yang Perlu Diwaspadai

- ada banyak listener dan GUI yang di-register di `SkytreePlugin`, jadi duplikasi register mudah terjadi
- beberapa subsystem punya state runtime sendiri, jadi perlu cleanup saat `onDisable`
- beberapa service saling bergantung ke item registry dan persistence
- file config dan data lama bisa tidak kompatibel setelah schema berubah
- README, build metadata, dan source tidak selalu sinkron

