# HeadBlocks v3.6.0

## What's New

### ✨ New Features

- **Fixed position hunts.** A new behavior where heads appear on spots you define, a few at a time. When someone finds one, it disappears for everybody and a new one appears on another free spot: players race to collect heads until they reach the goal. See the [documentation](https://aerwyn81.gitbook.io/headblocks) (Hunt Files, Fixed position).
  - Choose how many heads are present at once, how many a player must find, and an optional limit of total spawns.
  - The hunt ends per player, or the first player to reach the goal wins and the hunt closes.
  - Heads reappear instantly or after a random delay, can all be drawn again at a regular interval (optionally starting a new round with reset progress), and appear as soon as the hunt starts or only on demand.
  - Heads are picked from templates with a weight, each with its own content (heads, blocks, items, mobs, contents of other plugins) and its own rewards.
  - Heads that appeared survive restarts and reloads, and wait for their world if it is loaded later by another plugin.
- **Behavior menu**: a **Fixed position** entry opens a configuration menu with the templates, their weights and their rewards.
- **`/hb spawn <hunt>`** manages a fixed position hunt: `point add|remove|list|show` to place the spots in game, `config` to change its settings, `reroll [reset]` to draw the heads again and `clear` to remove them.

### 🚀 Improvements

- Progress, timed runs, areas, the previous hunt requirement and the per-hunt placeholders use the number of heads to find of a hunt, which is the goal for a fixed position hunt.
- Heads that appeared are protected from the per-head commands (`rename`, `move`, hunt transfer, per-head rewards). Breaking one as an admin makes it appear elsewhere.

### 🐛 Bug Fixes

- `%headblocks_current%`, `%headblocks_left%` and `%current%` only count placed heads, so they stay consistent with `%headblocks_max%`.

### ⚠️ Breaking Changes

- The database is migrated to version 6 on the first start (a column is added to the heads table). SQLite databases are backed up before the migration.
- The ordered behavior cannot be combined with the fixed position behavior: a hunt file combining both ignores the ordered behavior.

---

Thank you for using HeadBlocks ❤️

If you find a bug or have a question, don't hesitate to :

- open an issue in [**Github**](https://github.com/AerWyn81/HeadBlocks/issues)
- or in the [**Discord**](https://discord.gg/f3d848XsQt)
- or in the [**Spigot discussion**](https://www.spigotmc.org/threads/headblocks-christmas-event-1-20-easter-eggs-multi-server-support-fully-translatable-free.533826/)
