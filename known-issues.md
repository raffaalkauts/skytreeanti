tam# Skytree Known Issues

Daftar ini berisi masalah yang sudah terlihat dari source, logika startup, atau sinkronisasi metadata. Ini bukan semua bug yang ada, tapi titik risiko yang paling nyata untuk lanjut debugging.

## 1. Confirmed Issues

### 1.1 Missing command registration for `sell`

- Status: confirmed
- Impact: alias `/sell` can be skipped at startup because it is registered in code but not declared in `plugin.yml`
- Evidence:
  - `SkytreePlugin` calls `safeRegisterCommand("sell", ...)`
  - `plugin.yml` does not declare a `sell` command entry

Why it matters:

- startup will log a warning
- players may expect `/sell` to exist because it is a common alias, but the plugin.yml gate prevents registration

### 1.2 Version metadata is not fully in sync

- Status: confirmed
- Impact: docs and build metadata point to different release states
- Evidence:
  - `README.md` mentions version 3.2.1 and several WIP items
  - `pom.xml` and `plugin.yml` indicate version 4.0
  - `SkytreePlugin` class comment still mentions `1.0.0-beta`

Why it matters:

- AI or developer following the wrong doc may assume a feature is current when it is not
- this increases the chance of chasing obsolete behavior

### 1.3 Gacha is initialized but command is disabled

- Status: confirmed
- Impact: gacha logic exists, but the user-facing command path is intentionally inactive
- Evidence:
  - `SkytreePlugin` logs `Gacha system initialized (Command disabled)`

Why it matters:

- feature may appear "missing" to users even though backend classes are present
- debugging gacha requires checking whether the command or GUI is actually wired, not just whether service exists

## 2. Startup Risk Areas

### 2.1 Several subsystems fail open instead of failing the plugin

- Status: confirmed design risk
- Impact: plugin can load while some features are silently unavailable
- Evidence:
  - mythic items load errors are logged but do not abort startup
  - gacha errors are logged as warning but do not abort startup
  - progression system init errors are logged but do not always stop enable
  - fishing / enchant errors are also warning-level

Why it matters:

- a server can look "healthy" while a subsystem is broken
- debugging must check logs from startup, not just whether the plugin enabled

### 2.2 Listener and command wiring is centralized and large

- Status: confirmed design risk
- Impact: a wiring regression in `SkytreePlugin` can break many unrelated features at once
- Evidence:
  - `registerCommands()` and `registerListeners()` contain a long list of manual registrations

Why it matters:

- one missing constructor argument or null field can break a whole cluster of features
- changes here need regression checks across command, GUI, and event layers

## 3. Fragile Logic Areas

### 3.1 Item matching relies on similarity heuristics

- Status: confirmed risk
- Impact: sell / shop / storage / auction / recipe flows can mis-detect items after metadata changes
- Evidence:
  - `StorageService.extractItem(...)` uses `isSimilar(template)`
  - `SkytreeAuctionHouseService` uses worth-aware similarity checks
  - `ShopGUIListener` uses worth-aware comparison
  - several comments mention legacy or simplified matching assumptions

Why it matters:

- any change to lore, NBT, display name, or custom item format can break matching
- bugs here often look like "item disappeared" or "wrong item accepted"

### 3.2 Several systems still depend on fallback assumptions

- Status: confirmed risk
- Impact: behavior may be correct only for the current config shape or item format
- Evidence:
  - `MachineProcessor` contains fallback logic for legacy config keys and container positioning assumptions
  - `SkytreeIslandQuestService` comments show assumptions about island ownership and tier templates
  - `SkytreeRecipeService` has comments about placeholder mappings and simplifying assumptions
  - `Fishing` and `Bounty` related classes also contain assumption-based matching comments

Why it matters:

- these paths may work in the current test setup but break when config or data format evolves

### 3.3 Some systems are still partly legacy-compatible

- Status: confirmed risk
- Impact: mixed old/new behavior can produce inconsistent results across players or restarts
- Evidence:
  - legacy key fallback exists in machine config handling
  - legacy lore / string parsing remains in enchant and component utilities
  - several GUI and command classes include fallback behavior for older data

Why it matters:

- debugging requires knowing whether the bug is caused by new logic or a legacy fallback path

## 4. Operational Risks

### 4.1 Shutdown cleanup is partial

- Status: confirmed risk
- Impact: some runtime tasks or cached state may survive longer than intended if not explicitly handled
- Evidence:
  - `onDisable()` saves machines, item transport, fish storage, persistence, worth tooltip system, and minion service
  - not every subsystem has a visible cleanup path in the main shutdown block

Why it matters:

- if a bug appears after reload/restart, check whether the relevant subsystem has a save/stop method and whether it is called

### 4.2 Repository contains many build artifacts and logs

- Status: confirmed
- Impact: it is easy to inspect stale output instead of current source
- Evidence:
  - `target/`, jar files, logs, and build outputs are present in the repo workspace

Why it matters:

- AI should prefer `src/main/java` and `src/main/resources` over anything under `target/`

## 5. Priority Order For Fixes

If the next task is debugging, prioritize in this order:

1. command registration mismatch
2. startup soft-fail subsystem
3. item matching and template comparison
4. config fallback and legacy compatibility
5. cleanup and shutdown behavior
6. metadata / documentation drift

