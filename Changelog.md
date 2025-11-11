# HeadBlocks v2.8.2

Thank you for using HeadBlocks ❤

If you find a bug or have a question, don't hesitate to :

- open an issue in [**Github**](https://github.com/AerWyn81/HeadBlocks/issues)
- or in the [**Discord**](https://discord.gg/f3d848XsQt)
- or in the [**discussion
  **](https://www.spigotmc.org/threads/headblocks-christmas-event-1-20-easter-eggs-multi-server-support-fully-translatable-free.533826/)

## What's New

### ✨ New Features

- **Hide Found Heads**: Added an option to visually hide heads already discovered by players (requires PacketEvents)
- **Rewards Interface**: New GUI to manage and view configured rewards for each head
- **Per-Head Reset**: Ability to reset a player's progress for a specific head via `/reset` and `/resetall` commands

### 🚀 Improvements

- **Cache Optimization**: Complete overhaul of the caching system for Redis and Memory, including player, leaderboard,
  and
  heads caches
- **Redis Performance**: Replaced lists with sets for storing player heads, simplifying operations and improving
  performance
- **Hologram Management**: Overhauled hologram system with placeholder support. Removed support for
  CMI/FancyHolograms & DecentHolograms, replaced by "Advanced hologram" type
- **Asynchronous Particles**: Optimized particle spawning using Bukkit scheduler to reduce server load

### 🐛 Bug Fixes

- **Error Handling**: Fixed error spam on startup when there's an issue loading the database.

### 🔧 Technical

- **Dependency Removal**: Removed unnecessary dependencies to simplify plugin compilation.
- **Dependency Management**: Centralized dependency versions using Gradle catalog (`libs.versions.toml`)
- **Project Structure**: Simplified Gradle structure by removing unnecessary `core` module
