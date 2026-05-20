# Skytree Plugin - Minecraft Version Compatibility

## Supported Versions
This plugin is compatible with **Minecraft 1.19 - 1.21.x** (Paper/Spigot)

## Configuration

### Build Configuration
- **Paper API**: 1.21.3-R0.1-SNAPSHOT (latest)
- **API Version**: 1.19 (for backward compatibility)
- **Java**: 17+

### Why This Works
- The plugin uses only stable Paper/Bukkit APIs that haven't changed significantly between 1.19-1.21
- `api-version: 1.19` in `plugin.yml` tells Paper to maintain compatibility with 1.19+ servers
- Building against Paper 1.21.3 API ensures the plugin works on the latest version
- The code avoids version-specific features that would break compatibility

## Features Used (All Compatible 1.19-1.21)
✅ `ChunkGenerator` - Stable API across versions  
✅ `BlockPhysicsEvent` - Unchanged event  
✅ `CommandExecutor` - Standard command API  
✅ `Location` and `World` - Core APIs  
✅ `Material` enum - All materials used exist in 1.19+  

## Testing Recommendations
Test the plugin on:
- Paper 1.19.4 (minimum)
- Paper 1.20.1
- Paper 1.20.4
- Paper 1.21.3 (latest)

All should work without issues due to API stability.
