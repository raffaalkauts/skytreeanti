# Skytree Debugging Guide

Panduan ini dipakai untuk diagnosis bug atau regresi fitur di Skytree. Fokusnya adalah langkah yang cepat, berurutan, dan aman.

## 1. Prinsip Utama

- mulai dari source of truth, bukan dari artifact build
- tentukan dulu bug masuk ke layer mana
- jangan perbaiki gejala di GUI kalau akar masalah ada di service atau persistence
- cek dampak ke island, economy, item registry, dan config karena sistem ini saling terhubung

## 2. Urutan Triage

Saat ada bug report, pakai urutan ini:

1. pastikan versi plugin dan file yang dipakai benar
2. cek apakah bug terjadi saat startup, runtime, command, GUI, atau save/load
3. cari package yang terlibat
4. baca `SkytreePlugin` untuk wiring awal
5. baca service implementation terkait
6. baca listener atau GUI yang memicu bug
7. cek resource file yang dipakai subsystem tersebut
8. lihat log error dan stack trace
9. verifikasi dengan data player / item / config yang nyata

## 3. Klasifikasi Bug

### 3.1 Startup / Enable Bug

Gejala:

- plugin gagal enable
- dunia tidak terbentuk
- command tidak terdaftar
- listener tidak aktif

Yang dicek:

- `SkytreePlugin.onEnable()`
- urutan inisialisasi service
- resource file yang dibaca saat startup
- exception yang ditangkap lalu di-log

### 3.2 Command Bug

Gejala:

- command tidak merespons
- tab completion rusak
- alias tidak jalan
- permission ditolak padahal harusnya boleh

Yang dicek:

- `src/main/resources/plugin.yml`
- registration di `SkytreePlugin.registerCommands()`
- class di `command/`
- permission node

### 3.3 GUI Bug

Gejala:

- GUI tidak kebuka
- klik tidak bereaksi
- item title / slot salah
- duplikasi item atau inventory corruption

Yang dicek:

- class di `gui/`
- listener di `listener/` yang handle inventory click
- holder / inventory title / slot mapping
- data service yang dipanggil dari GUI

### 3.4 Persistence Bug

Gejala:

- balance hilang
- island data reset
- quest progress tidak tersimpan
- bank/account tidak konsisten
- minion/machine state hilang setelah restart

Yang dicek:

- `YamlPersistenceService`
- model yang dipersist
- file resource yang disave / di-load
- lifecycle `onEnable` dan `onDisable`

### 3.5 Gameplay Rule Bug

Gejala:

- proteksi island salah
- block bisa dibreak padahal harusnya tidak
- PvP / build / trust rule salah
- item mechanic tidak jalan

Yang dicek:

- listener terkait proteksi / interaction
- service island
- permission / trust / role
- data world / island owner / member

### 3.6 Economy Bug

Gejala:

- saldo salah
- transaksi ganda
- shop harga aneh
- auction / bank / bounty tidak sinkron

Yang dicek:

- `EconomyService` dan implementasinya
- `WorthService`
- `ShopService`
- `BankService`
- `AuctionHouseService`
- log transaksi

## 4. Checklist Debug Cepat

### Jika bug ada di command

- cek command ada di `plugin.yml`
- cek executor didaftarkan di `registerCommands()`
- cek alias cocok
- cek permission yang dipakai
- cek parameter parsing
- cek message error yang dikirim ke player

### Jika bug ada di GUI

- cek GUI class mana yang dibuka
- cek event click / drag / close
- cek slot index
- cek apakah item yang dipasang ke slot sudah benar
- cek listener apakah mem-block double click / shift click
- cek apakah state GUI diambil dari service yang benar

### Jika bug ada di island

- cek owner island
- cek member / trust level
- cek protection rule
- cek world generator dan home teleport
- cek data island di persistence

### Jika bug ada di item / custom item

- cek `SkytreeItemRegistry`
- cek `MythicItemManager`
- cek `RecipeService`
- cek NBT / metadata / similarity check
- cek resource JSON / YAML yang mendefinisikan item

### Jika bug ada di bank / economy

- cek transaction flow dari input sampai persist
- cek rounding / integer conversion
- cek duplicate submit
- cek history record
- cek service yang melakukan reload data

### Jika bug ada di machine / minion

- cek tick handler atau processor
- cek storage state
- cek item transport
- cek chunk / world loading
- cek cleanup saat plugin disable

## 5. Folder Yang Biasanya Perlu Dibuka

- [src/main/java/com/wiredid/skytree/SkytreePlugin.java](./src/main/java/com/wiredid/skytree/SkytreePlugin.java)
- [src/main/resources/plugin.yml](./src/main/resources/plugin.yml)
- [src/main/resources/config.yml](./src/main/resources/config.yml)
- [src/main/java/com/wiredid/skytree/api](./src/main/java/com/wiredid/skytree/api)
- [src/main/java/com/wiredid/skytree/impl](./src/main/java/com/wiredid/skytree/impl)
- [src/main/java/com/wiredid/skytree/listener](./src/main/java/com/wiredid/skytree/listener)
- [src/main/java/com/wiredid/skytree/gui](./src/main/java/com/wiredid/skytree/gui)
- [src/main/java/com/wiredid/skytree/system](./src/main/java/com/wiredid/skytree/system)
- [src/main/resources](./src/main/resources)

## 6. Cara Cari Akar Masalah

Gunakan pendekatan ini:

1. identifikasi trigger
2. identifikasi layer
3. identifikasi data yang dipakai
4. identifikasi service yang memproses
5. identifikasi output yang salah
6. bandingkan dengan alur yang diharapkan

Contoh:

- kalau player klik GUI lalu saldo salah, jangan langsung edit GUI
- cek dulu apakah GUI hanya memanggil service dengan amount yang salah
- kalau service sudah benar, cek listener click mapping
- kalau listener benar, cek data item atau config harga

## 7. Query Cepat Yang Berguna

Saat menelusuri bug, cari pola seperti:

- `registerEvents(`
- `setExecutor(`
- `setTabCompleter(`
- `saveDefaultConfig()`
- `reload()`
- `load()`
- `save()`
- `onEnable()`
- `onDisable()`
- `getConfig().get`
- `plugin.yml`
- `isSimilar`
- `clone()`
- `HashMap`
- `HashSet`

## 8. Bug Pattern Yang Sering Muncul

### 8.1 Double Registration

Tanda:

- listener jalan dua kali
- event diproses ganda
- command dieksekusi dua kali

Biasanya sebab:

- register dipanggil lebih dari sekali
- plugin enable ulang tanpa cleanup

### 8.2 Config Drift

Tanda:

- key config tidak terbaca
- default fallback aneh
- fitur seperti mati sendiri

Biasanya sebab:

- key berubah di source tapi resource belum ikut berubah
- `saveDefaultConfig()` menimpa asumsi lama

### 8.3 Item Comparison Bug

Tanda:

- item yang harusnya cocok dianggap beda
- item custom tidak terdeteksi
- sell / shop / recipe gagal match

Biasanya sebab:

- compare terlalu ketat
- metadata / lore / NBT ikut mempengaruhi
- item template belum dinormalisasi

### 8.4 State Not Persisted

Tanda:

- data hilang setelah restart
- perubahan ada di runtime tapi tidak tersimpan

Biasanya sebab:

- `save()` tidak dipanggil
- object mutable tidak ditulis balik ke storage
- cleanup di `onDisable()` belum lengkap

### 8.5 Wrong Event Priority

Tanda:

- action kadang jalan, kadang tidak
- plugin lain menimpa perilaku ini

Biasanya sebab:

- priority listener tidak cocok
- event dibatalkan terlalu awal atau terlalu lambat

## 9. Debugging Berdasarkan Area

### Island

Periksa:

- owner dan member
- trust level
- protection flags
- teleport destination
- world generator

### Economy

Periksa:

- input amount
- fee / tax / rounding
- negative balance handling
- transaction history
- sync antar service

### Fishing

Periksa:

- rod storage
- enchant service
- item NBT
- GUI reward / sell flow

### Bank

Periksa:

- deposit / withdraw path
- account lookup
- journal entry creation
- reconciliation logic

### Machine / Minion

Periksa:

- chunk loaded / unloaded
- tick timing
- storage input-output
- upgrade rules

### Quest / Rewards

Periksa:

- template config
- progression state
- reward claim duplication
- time-based reset

## 10. Validasi Sebelum Mengubah Kode

Sebelum commit fix, pastikan:

- bug bisa direproduksi
- penyebab sudah ditemukan di layer yang tepat
- fix tidak merusak subsystem lain
- data lama masih bisa dibaca
- command / GUI / listener yang terkait tetap konsisten

## 11. Validasi Setelah Mengubah Kode

Setelah fix, cek:

- startup masih berhasil
- command tetap terdaftar
- GUI masih terbuka
- data tersimpan setelah relog / restart
- log tidak menunjukkan exception baru
- feature terkait tidak regress

## 12. Catatan Praktis

- kalau bug menyentuh `SkytreePlugin`, hati-hati karena itu titik wiring semua sistem
- kalau bug menyentuh `plugin.yml`, cek semua command yang terkait, bukan cuma satu
- kalau bug menyentuh `config.yml`, pastikan default dan runtime key sama
- kalau bug menyentuh `target/`, itu biasanya gejala build lama, bukan sumber utama

## 13. Rekomendasi Untuk AI Penerus

Saat diminta debug, paling efektif:

1. tentukan file / subsystem yang terlibat
2. cek source code asal bug
3. buat perubahan minimum
4. verifikasi dengan build atau log jika memungkinkan
5. update dokumentasi kalau perilaku penting berubah

