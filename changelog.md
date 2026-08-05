# HeadBlocks v3.1.1

## What's New

### ✨ New Features

- Added Folia support.
- The plugin now ships as two jars. Paper, Purpur and Folia servers use `HeadBlocks-<version>.jar`
  (Modrinth, Hangar, GitHub); Spigot servers use `HeadBlocks-<version>-spigot.jar` (SpigotMC). The two are not interchangeable — download the one matching your server.

### 🚀 Improvements

- Added support for Minecraft 26.2.

### 🐛 Bug Fixes

- Fixed a per-head reward loading error after `/hb reload`.
- Fixed hunt files not being rewritten when heads were removed in bulk, which made removed heads reappear after a restart. Hunt files are now written atomically.
- Fixed a head rotation task leaking on every `/hb move`, which made moved heads spin twice as fast.
- Fixed heads in an unloaded world scheduling a rotation task that errored on every tick.

---

Thank you for using HeadBlocks ❤️

If you find a bug or have a question, don't hesitate to :

- open an issue in [**Github**](https://github.com/AerWyn81/HeadBlocks/issues)
- or in the [**Discord**](https://discord.gg/f3d848XsQt)
- or in the [**Spigot discussion**](https://www.spigotmc.org/threads/headblocks-christmas-event-1-20-easter-eggs-multi-server-support-fully-translatable-free.533826/)