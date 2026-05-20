# Build Instructions

## Requirements
- Java 21 (JDK 21)
- Maven 3.x

## Building

```bash
mvn clean package
```

The compiled JAR will be output to: `target/Skytree-v3.2.0.jar`

## Running

1. Copy the JAR to your PaperMC 1.21 server's `plugins/` folder
2. Start/restart the server
3. The plugin will:
   - Generate default `config.yml`
   - Create void world (`skytree_world`)
   - Register 50+ custom items
   - Enable all commands

## Testing

To test the plugin:
1. Join the server
2. Run `/is create` - You should be teleported to a tree on a floating island
3. Check balance: `/bal` - Should show 100.00 BTC
4. Open shop: `/shop` - GUI should open
5. Break leaves - Get saplings and occasionally Silkworms
6. Try crafting or use admin commands to get custom items

## Development

### Project Structure
```
src/main/java/com/wiredid/skytree/
├── SkytreePlugin.java           # Main plugin class
├── api/                         # Service interfaces
│   ├── EconomyService.java
│   ├── IslandService.java
│   ├── ItemRegistry.java
│   ├── ShopService.java
│   └── ...
├── impl/                        # Service implementations
│   ├── SkytreeEconomyService.java
│   ├── SkytreeIslandService.java
│   ├── SkytreeItemRegistry.java
│   └── ...
├── model/                       # Data models
│   ├── Island.java
│   ├── IslandMember.java
│   └── IslandRole.java
├── command/                     # Command handlers
│   ├── SkytreeCommand.java
│   ├── BalanceCommand.java
│   └── ...
├── listener/                    # Event listeners
│   ├── IslandProtectionListener.java
│   ├── MechanicsListener.java
│   └── ...
└── world/                       # World generation
    └── VoidChunkGenerator.java
```

### Adding Custom Items

Edit `SkytreeItemRegistry.java` and add items in the `registerAllItems()` method:

```java
registerItem("item_id", Material.MATERIAL, "§Display Name", 
    "§7Lore line 1", "§7Lore line 2");
```

## Troubleshooting

**Plugin won't load:**
- Ensure you're using PaperMC 1.21 (not Spigot/Bukkit)
- Check Java version: `java -version` (must be 21)

**World not generating:**
- Check `config.yml` for world name
- Ensure no conflicts with existing worlds

**Items not working:**
- Items use PersistentDataContainer - they must be obtained via `/skytree give` or the shop
- Check console for "Registered X custom items!" message
