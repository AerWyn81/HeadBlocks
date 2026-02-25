# HeadBlocks v2.8.5

## What's New

### ✨ New Features

- Added `/hb debug resync` commands for database/locations synchronization
  - `resync database`: removes heads from database not present in locations.yml
  - `resync locations`: restores head blocks in the world from locations.yml
  - Supports `--force` flag to override safety checks
  - Multi-server detection with safety warnings
- Replaced default database connection handler by HikariCP connection pooling for MySQL and SQLite
  - Added configurable pool settings (`maxConnections`, `minIdleConnections`, `connectionTimeout`, `idleTimeout`, `maxLifetime`)

### 🚀 Improvements

- The `/hb removeall` command now runs asynchronously to avoid server freezes on large head counts
- Enhanced head visibility handling logic and optimized caching by introducing chunk-based tracking
- Updated to Minecraft 1.21.11

### 🐛 Bug Fixes

- Fixed command parsing in RewardService to skip blank commands and added safety checks for empty command lists
- Fixed deprecated Particle enum usage for 1.21+ compatibility

---

Thank you for using HeadBlocks ❤️

If you find a bug or have a question, don't hesitate to :

- open an issue in [**Github**](https://github.com/AerWyn81/HeadBlocks/issues)
- or in the [**Discord**](https://discord.gg/f3d848XsQt)
- or in the [**discussion
  **](https://www.spigotmc.org/threads/headblocks-christmas-event-1-20-easter-eggs-multi-server-support-fully-translatable-free.533826/)