# Skytree Documentation Index

Peta masuk cepat untuk AI atau developer yang akan melanjutkan project ini.

## 1. Dokumen Utama

- [prd.md](./prd.md)
- [architecture.md](./architecture.md)
- [debugging-guide.md](./debugging-guide.md)
- [feature-map.md](./feature-map.md)
- [known-issues.md](./known-issues.md)

## 2. Urutan Baca Yang Disarankan

1. [prd.md](./prd.md) untuk memahami tujuan produk, ruang lingkup, dan sumber kebenaran.
2. [architecture.md](./architecture.md) untuk peta modul dan dependency antar layer.
3. [feature-map.md](./feature-map.md) untuk mencari lokasi implementasi fitur.
4. [known-issues.md](./known-issues.md) untuk melihat risiko dan issue yang sudah teridentifikasi.
5. [debugging-guide.md](./debugging-guide.md) untuk langkah diagnosis saat ada bug baru.

## 3. Fokus Cepat Berdasarkan Tugas

### Kalau mau debug

- mulai dari [debugging-guide.md](./debugging-guide.md)
- cek [known-issues.md](./known-issues.md)
- buka file source yang disebut di [architecture.md](./architecture.md)

### Kalau mau tambah fitur

- cari subsystem terkait di [feature-map.md](./feature-map.md)
- cek kontrak dan implementasi di [architecture.md](./architecture.md)
- cek resource file yang relevan di `src/main/resources`

### Kalau mau cari konteks produk

- baca [prd.md](./prd.md)

## 4. Source Of Truth

Prioritas referensi saat ada konflik:

1. `src/main/java`
2. `src/main/resources`
3. `pom.xml`
4. artifact build dan log

Jangan jadikan output `target/` sebagai sumber utama desain atau perilaku terbaru.

## 5. File Kunci Untuk Mulai Investigasi

- `src/main/java/com/wiredid/skytree/SkytreePlugin.java`
- `src/main/resources/plugin.yml`
- `src/main/resources/config.yml`
- `src/main/java/com/wiredid/skytree/impl/`
- `src/main/java/com/wiredid/skytree/listener/`
- `src/main/java/com/wiredid/skytree/gui/`
- `src/main/java/com/wiredid/skytree/system/`

## 6. Catatan Praktis

- Project ini besar dan saling terhubung.
- Banyak fitur dihubungkan dari `SkytreePlugin`, jadi perubahan di sana punya efek luas.
- Kalau sebuah fitur terasa "hilang", cek dulu apakah command-nya benar-benar terdaftar di `plugin.yml`.
- Kalau sebuah fitur cuma rusak setelah restart, cek persistence dan shutdown cleanup.

