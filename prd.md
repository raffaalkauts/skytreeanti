# Skytree PRD

Dokumen ini dibuat sebagai pegangan untuk AI atau developer berikutnya yang akan melanjutkan debugging, refactor, atau penambahan fitur pada project Skytree.

## 1. Ringkasan Produk

Skytree adalah plugin Minecraft Paper/Spigot untuk server SkyBlock / SkyFactory-like. Fokus utamanya:

- membuat dunia void / skyblock dari nol
- mengelola island pemain
- menyediakan ekonomi custom
- menyajikan banyak GUI in-game
- menyediakan item, machine, quest, enchant, fishing, bank, auction house, bounty, shard, rank, tag, dan sistem progression lain

Target runtime saat ini:

- PaperMC 1.21
- Java 21

## 2. Tujuan Produk

Tujuan utama project ini adalah menjadi plugin inti untuk server SkyBlock yang terasa "lengkap" tanpa bergantung berat pada plugin eksternal.

Yang ingin dicapai:

- gameplay skyblock dari awal sampai endgame
- sistem ekonomi custom yang konsisten
- UI/GUI in-game untuk hampir semua aksi utama
- data persistence berbasis YAML lokal
- sistem modular agar fitur bisa ditambah tanpa merusak sistem lain

## 3. Ruang Lingkup Fitur

### Fitur inti yang sudah jelas ada

- island creation, home, delete, protection, settings
- economy dan transaksi
- shop global
- auction house dan buy orders
- bank
- quest system
- daily rewards
- ranks
- tags
- bounty
- shards
- chat channels
- fishing subsystem
- minion subsystem
- machine subsystem
- item registry / custom items
- crate / gacha / monetization hooks
- leaderboard
- scoreboard dan tab list
- admin dashboard / logs / player editor

### Fitur mekanik khas skyblock

- void world generator
- template island awal
- crook / silkworm mechanic
- hammer progression
- sieve / barrel / crucible / compressor style gameplay
- custom enchants
- custom fish
- custom recipes
- item transport / pipe / storage systems

### Fitur yang tampak masih belum sepenuhnya final

- beberapa command dan area masih diberi label WIP di dokumentasi lama
- ada komponen yang diinisialisasi tetapi command-nya dimatikan atau belum dipakai penuh
- beberapa sistem terlihat masih dalam tahap iterasi dari log dan komentar source

## 4. Sumber Kebenaran Project

Kalau ingin memahami project ini, urutan sumber yang paling penting adalah:

1. `src/main/java/com/wiredid/skytree/SkytreePlugin.java`
2. `src/main/resources/plugin.yml`
3. `src/main/resources/config.yml`
4. service implementation di `src/main/java/com/wiredid/skytree/impl/`
5. listener di `src/main/java/com/wiredid/skytree/listener/`
6. GUI di `src/main/java/com/wiredid/skytree/gui/`
7. sistem domain khusus di package seperti `fishing`, `banking`, `machine`, `system`, `model`

Yang bukan sumber kebenaran utama:

- file di `target/`
- jar hasil build
- log build lama
- catatan compile error yang tidak sedang dipakai sebagai referensi desain

## 5. Arsitektur Tingkat Tinggi

Project ini memakai pola modular berbasis service.

### Lapisan utama

- `api/`
  - interface kontrak untuk service
  - contoh: `IslandService`, `EconomyService`, `ShopService`, `AuctionHouseService`, `QuestService` sejenisnya
- `impl/`
  - implementasi konkret service
  - banyak logic bisnis inti ada di sini
- `listener/`
  - event handler Bukkit/Paper
  - menangani interaksi block, inventory, combat, chat, GUI click, join/quit, dan sebagainya
- `command/`
  - command executor dan tab completer
- `gui/`
  - semua inventory GUI dan holder/helper terkait
- `model/`
  - data class dan enum domain
- `system/`
  - sistem global / runtime logic seperti scoreboard, tab, quest, daily rewards, enchants, energy
- `fishing/`
  - subsystem fishing lengkap
- `banking/`
  - subsystem bank dengan persistence sendiri
- `machine/`
  - processor untuk machine
- `world/`
  - chunk generator dunia void

### Main class

`SkytreePlugin` bertindak sebagai bootstrap:

- load config default
- initialize world
- initialize persistence
- initialize economy / rank / admin service
- initialize item registry dan recipe service
- initialize subsystem lain
- register listeners dan command
- menyiapkan GUI instance dan helper object

## 6. Alur Runtime

Saat plugin aktif, alur yang penting biasanya seperti ini:

1. server load plugin
2. world void diinisialisasi
3. persistence YAML disiapkan
4. ekonomi dan service inti dibuat
5. item registry dan recipe load
6. custom items / mythic items load
7. subsystem tambahan disiapkan
8. command dan listener didaftarkan
9. pemain join
10. data pemain dimuat
11. scoreboard / tab / visual system berjalan
12. aksi pemain memicu listener, GUI, atau service layer

## 7. Domain Utama

### Island

Island adalah pusat progression.

Fungsi umum:

- create island
- teleport home
- delete island
- trust / untrust player
- settings
- warp
- quest terkait island
- protection
- level / stats / leaderboard

### Economy

Ekonomi custom dipakai sebagai mata uang utama.

Komponen:

- balance player
- transfer antar player
- transaction history
- admin adjustment
- integration dengan shop, bank, auction, bounty, ranks, shard, dan reward systems

### Items dan Recipes

Project ini punya item registry besar.

Kategori yang terlihat:

- tools
- custom components
- machine parts
- dust / ore piece / pebble
- fish-related items
- gacha / crate items
- cosmetic / special items

### Fishing

Fishing adalah subsystem mandiri dengan:

- rod storage
- enchant service
- fish storage
- fish shop
- fish sell
- price guide
- active rod GUI

### Banking

Banking punya layer sendiri, bukan cuma saldo biasa.

Fungsi:

- deposit
- withdraw
- transfer
- history
- stats
- admin reconcile / audit / rate setup

### Machines

Machine system mencakup process logic untuk device seperti:

- sieve
- barrel
- crucible
- auto crafter
- furnace / advanced furnace style machine

### Progression

Ada beberapa sistem progression paralel:

- quest
- achievement
- daily rewards
- ranks
- tags
- leaderboard
- playtime
- shards
- crates / gacha
- bounty

## 8. Command Surface

Command utama yang terlihat di `plugin.yml`:

- `/skytree` atau alias `/is`, `/island`, `/st`
- `/shop`
- `/quest`
- `/bal`
- `/baltop`
- `/pay`
- `/hub`
- `/warp`
- `/home`
- `/sethome`
- `/delhome`
- `/tpa`, `/tpaccept`, `/tpdeny`
- `/guide`
- `/rod`
- `/fish`
- `/sg`
- `/vault`
- `/sort`
- `/trash`
- `/bank`
- `/bankadmin`
- `/minion`
- `/rtp`
- `/kits`
- `/ah`
- `/orders`
- `/settings`
- `/bounty`
- `/shards`
- `/leaderboard`
- `/trust`, `/untrust`, `/trustlist`
- `/ic`, `/lc`, `/gc`, `/chathist`
- `/crates`
- `/history`
- `/transactions`
- `/admin`
- `/rank`
- `/feed`
- `/heal`
- `/fly`
- `/nick`
- `/daily`
- `/islandshop`
- `/chat`
- `/ce`
- `/menu`
- `/profile`
- `/glow`
- `/tags`
- `/bait`

## 9. Permissions Penting

Yang paling penting untuk debugging permission:

- `skytree.use`
- `skytree.admin`
- `skytree.bypass`
- `skytree.give`
- `skytree.island.create`
- `skytree.bank.admin`
- rank-specific permissions seperti `skytree.rank.feed`, `skytree.rank.heal`, `skytree.rank.fly`, `skytree.rank.nick`, `skytree.rank.glow`

## 10. Konfigurasi dan Data

### Resource files

File resource yang penting:

- `config.yml`
- `shop.yml`
- `shard_shop.yml`
- `quests.yml`
- `daily_rewards.yml`
- `custom_enchants.yml`
- `custom_fish.yml`
- `crates.yml`
- `items.yml`
- `kits.yml`
- `trust_levels.yml`
- `tags.yml`
- `tab.yml`
- `chat_config.yml`
- `baits.yml`
- `island_quests.yml`
- `machine_recipes.yml`
- `machine_upgrades.yml`
- `minion_types.yml`
- `investments.yml`
- `worth.yml`
- `mythic_items.json`
- `plugin.yml`

### Persistence

Project ini memakai persistence lokal berbasis YAML, jadi state penting kemungkinan disimpan di file / service sendiri, bukan database eksternal.

Implikasi untuk debugging:

- cek file resource dan save/load logic bersamaan
- pastikan perubahan schema kompatibel dengan data lama
- hati-hati dengan default config yang mungkin tertimpa saat saveDefaultConfig()

## 11. Catatan Build

Project ini pakai Maven.

Command build utama:

- `mvn clean package`

Artifact final biasanya berada di:

- `target/Skytree-v4.0.jar`

Catatan:

- ada beberapa file build output lama dan jar lama di repo workspace
- untuk memahami behavior terbaru, utamakan source dan build result terbaru, bukan artifact historis

## 12. Catatan Penting untuk AI Penerus

Kalau lanjut debugging atau nambah fitur, ikuti urutan ini:

1. cek `SkytreePlugin` untuk memahami wiring
2. cek `plugin.yml` untuk command dan permission
3. cek service interface di `api/`
4. cek implementasi di `impl/`
5. cek listener terkait event yang bermasalah
6. cek GUI jika bug-nya berasal dari klik inventory
7. cek resource YAML/JSON jika masalahnya data / item / balance / recipe

Hal yang sering jadi sumber bug di project seperti ini:

- mismatch antara command yang dideklarasikan dan executor yang didaftarkan
- config key berubah tapi loader belum ikut di-update
- item comparison terlalu ketat atau terlalu longgar
- state player tidak tersimpan saat disable atau quit
- listener register dobel
- GUI holder / inventory title mismatch
- data lama tidak kompatibel dengan format baru
- sistem ekonomi / bank / auction saling bergantung

## 13. Risiko dan Inkonistensi yang Sudah Terlihat

- README dan metadata build tidak sepenuhnya sinkron
  - `README.md` menyebut versi 3.2.1 dan beberapa fitur masih WIP
  - `pom.xml` dan `plugin.yml` menunjukkan versi 4.0
- ada sistem yang sudah diinisialisasi tetapi belum tentu aktif penuh, contohnya gacha disebut "command disabled"
- repo berisi banyak artifact build dan log, jadi mudah salah baca kalau AI mengambil referensi dari folder output
- beberapa komentar source menunjukkan fitur masih iteratif atau belum final

## 14. Prioritas Jika Mau Melanjutkan Development

Urutan kerja yang paling aman:

1. rapikan source of truth dokumentasi
2. verifikasi command dan listener yang benar-benar aktif
3. audit config key dan format data save/load
4. stabilkan sistem economy, island, dan item registry
5. baru lanjut perluasan fitur seperti machine, quest, dan progression tambahan

## 15. Definisi Singkat Produk

Skytree adalah plugin SkyBlock besar dengan fokus pada progression, ekonomi, island management, dan banyak sistem pendukung yang saling terhubung. Untuk debugging, jangan anggap ini plugin kecil satu fitur; ini lebih dekat ke platform gameplay modular.
