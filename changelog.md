# HeadBlocks v3.4.0

## What's New

### ✨ New Features

- **Hunt requirements.** A **Requirements** menu in `/hb hunt create` to stack the conditions to claim a head: area, previous hunt, permission, playtime and PlaceholderAPI, combined with `ALL` or `ANY`. See the [documentation](https://github.com/AerWyn81/HeadBlocks/blob/master/docs/configuration/hunts.md#requirements).
- `/hb give <player> <number> [hunt]` links the given head to a hunt. It is always placed in that hunt, whatever the hunt selected with `/hb hunt select`.
- `/hb hunt create` without a name creates a hunt named with the lowest free number (`1`, `2`, ...).

### 🚀 Improvements

- The bounded zone became the **area** requirement, with every option it had. Existing hunt files are migrated on load.
- The `headblocks.zone.bypass` permission is now `headblocks.area.bypass`. The old node keeps working.
- Hunt files no longer keep the keys of a behavior or a requirement that was removed.
- Player movement checks are skipped when no active hunt has an area to enforce.

### 🐛 Bug Fixes

- Fixed heads being placed in a deleted hunt when it was still selected. The selection now falls back to the default hunt.
- Fixed a possible error when refreshing HeadBlocks menus while the player had no inventory open.

---

Thank you for using HeadBlocks ❤️

If you find a bug or have a question, don't hesitate to :

- open an issue in [**Github**](https://github.com/AerWyn81/HeadBlocks/issues)
- or in the [**Discord**](https://discord.gg/f3d848XsQt)
- or in the [**Spigot discussion**](https://www.spigotmc.org/threads/headblocks-christmas-event-1-20-easter-eggs-multi-server-support-fully-translatable-free.533826/)