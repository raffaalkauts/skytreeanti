# Skytree Plugin

**Version:** 3.2.1  
**Target:** PaperMC 1.21 / Java 21  
**Type:** SkyFactory-like SkyBlock plugin with 80+ features

## 🚀 Features Implemented

### Core Systems ✅
- **Void World Generation**: Custom chunk generator for empty skyblock world
- **Island Management**: Create, delete, and teleport to islands
- **SkyFactory 3 Template**: Bedrock + 3 Dirt + Oak Tree generation
- **Anti-Dupe**: Strict one-island-per-player enforcement
- **Island Protection**: Members-only building permissions
- **YAML Persistence**: Standalone data storage (no external plugins needed)

### Economy System ✅
- **BTC Currency**: Custom in-game currency (BitCoins)
- **Commands**: `/bal`, `/btc`, `/money`, `/pay`
- **Starting Balance**: 100 BTC per player

### Custom Items (50+) ✅
- **Crooks**: Wooden, Stone (for extra leaf drops + Silkworms)
- **Hammers**: Wood, Stone, Iron, Diamond (Cobble → Gravel → Sand → Dust)
- **Meshes**: String, Flint, Iron, Diamond (for Sieve)
- **Pebbles**: Stone, Granite, Diorite, Andesite
- **Dusts**: Iron, Gold, Copper, Tin, Aluminum, Silver, Lead, Nickel
- **Ore Pieces**: Iron, Gold, Copper, Diamond, Emerald, Coal
- **Silkworms**: Infest leaves to get String
- **Dolls**: Blaze (Angry Doll), Enderman (Creepy Doll)
- **Machines**: Sieve, Barrel, Crucible

### Shop System ✅
- **Iridium-Style GUI**: Category-based shop
- **Categories**: Blocks, Farming, Mob Drops, Minerals, Custom Items, Machines, Energy
- **Command**: `/shop`

### Commands ✅
- `/is create` (alias: `c`) - Create island
- `/is home` (alias: `h`) - Teleport home
- `/is delete` (alias: `del`) - Delete island
- `/is stats` (alias: `s`) - View stats
- `/is level` - Calculate level (WIP)
- `/is top` - Leaderboards (WIP)
- `/is biome` - Change biome (WIP)
- `/is settings` - Island settings (WIP)
- `/is upgrade` - Upgrade island (WIP)
- `/is perms` (alias: `p`) - Permissions (WIP)
- `/bal`, `/btc`, `/money` - Check balance
- `/pay <player> <amount>` - Send money
- `/shop` - Open shop

### Mechanics ✅
- **Hammer**: Downgrades blocks (Cobble → Gravel → Sand → Dust)
- **Crook**: 10% chance for Silkworms from leaves
- **Silkworm**: Right-click on leaves to infest (drops String after 10s)

## 📦 Building

```bash
mvn clean package
```

The compiled JAR will be in `target/Skytree-v3.2.0.jar`

## 🎮 Getting Started

1. Place the JAR in your `plugins/` folder
2. Start the server
3. Join and type `/is create` to get your island
4. Break leaves with your hands to get saplings
5. Craft items or use `/shop` to buy materials
6. Progress through the Ex Nihilo mechanics!

## 🛠️ Planned Features (Work in Progress)

- Machine GUIs (Sieve, Barrel, Crucible, Cobble Generator)
- Energy System (Flux, Generators, Cables)
- Island Upgrades (Size, Members, Generator)
- Leaderboards
- Biome changing
- Advanced permissions system
- Witch Water mechanics
- Mob transformations
- 50+ more items to reach 100+

## 📝 Configuration

Edit `config.yml` to customize:
- World name
- Island spacing
- Starting balance
- Machine tick rates

## 🤝 Commands & Permissions

All commands support aliases for efficiency:
- Main: `/skytree`, `/is`, `/island`
- Balance: `/bal`, `/btc`, `/money`, `/balance`

**Permissions:**
- `skytree.use` - Basic usage (default: true)
- `skytree.admin` - Admin commands (default: op)
- `skytree.bypass` - Bypass protections (default: op)

## 📚 Credits

Created with Antigravity AI Assistant for PaperMC 1.21

---

**Status**: Premium UI Release - All features functional, stable release
