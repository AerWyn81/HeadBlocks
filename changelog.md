# HeadBlocks v3.5.0

## What's New

### ✨ New Features

- **Hide more than heads.** The `heads` list now accepts blocks (`block:LANTERN`), items (`item:DIAMOND:1001`), item frames (`frame:FILLED_MAP`), floating texts (`text:&6Find me!`) and mobs without AI (`mob:CAT`). Options go between brackets, e.g. `mob:ZOMBIE[head=DIAMOND_HELMET,baby=true]`. See the [documentation](https://aerwyn81.gitbook.io/headblocks/configuration-config.yml/rendering).
- **Contents of other plugins**, when installed: custom items and furniture from **Nexo**, **ItemsAdder** and **Oraxen**, mobs from **MythicMobs**, 3D models from **ModelEngine** and **BetterModel**, and NPCs from **Citizens**, **FancyNpcs** and **ZNPCsPlus**.
- **Rendering modes.** A new `rendering` section (`mode`, `scale`, `glow`) in `config.yml`, overridable per hunt: heads and blocks are placed as real blocks (`BLOCK`) or as floating, scalable display entities (`DISPLAY`) that spin smoothly.
- `/hb hunt rendering <hunt> <block|display>` converts the heads already placed in a hunt, keeping their content, position and player progress. `/hb hunt info` shows the rendering of the hunt.
- **Catalog.** `/hb give` opens a menu to take the items: filter by type, pick the hunt the items are linked to, shift + click to take a stack. `/hb give <player>` opens it for that player, and can be run from the console.

### 🚀 Improvements

- Heads rendered as entities are hidden natively for players who found them with `hideFoundHeads`: no PacketEvents needed, and no invisible collision left behind.
- Entities are never saved in the world: they are spawned when their chunk loads and respawned if something removes them, so uninstalling the plugin leaves nothing behind.
- Placed block heads are protected: they don't burn, melt, decay, pop off when their support is removed or get pulled by a sticky piston, and clicking one only claims it (a chest doesn't open, a lever doesn't toggle).
- Item frames are placed on the face you click, including floors and ceilings.
- `/hb move` also moves heads rendered as entities.
- A head moved to another hunt takes the rendering of its new hunt.
- Holograms are raised above tall contents such as mobs.
- Spin settings of a hunt are now applied to its heads.
- Faster head lookups on servers with many heads.

### ⚠️ Breaking Changes

- `/hb give <player> <number|*> [hunt]` is replaced by the catalog: the head number, `*` and hunt arguments no longer exist. Scripts or commands giving heads by number must be updated.

---

Thank you for using HeadBlocks ❤️

If you find a bug or have a question, don't hesitate to :

- open an issue in [**Github**](https://github.com/AerWyn81/HeadBlocks/issues)
- or in the [**Discord**](https://discord.gg/f3d848XsQt)
- or in the [**Spigot discussion**](https://www.spigotmc.org/threads/headblocks-christmas-event-1-20-easter-eggs-multi-server-support-fully-translatable-free.533826/)
