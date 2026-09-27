# HeadBlocks v3.6.0

## What's New

### ✨ New Features

- **Spawning heads hunts.** A new behavior where heads appear a few at a time. When someone finds one, it disappears for everybody and a new one appears elsewhere: players race to collect heads until they reach the goal. See the [documentation](https://aerwyn81.gitbook.io/headblocks) (Hunt Files, Spawning heads).
  - Choose how many heads are present at once, how many a player must find, and an optional limit of total spawns.
  - The hunt ends per player, or the first player to reach the goal wins and the hunt closes.
  - Heads reappear instantly or after a random delay, can all be drawn again at a regular interval (optionally starting a new round with reset progress), and appear as soon as the hunt starts or only on demand.
  - Heads are picked from templates with a weight, each with its own content (heads, blocks, items, mobs, contents of other plugins) and its own rewards.
  - Heads that appeared survive restarts and reloads, and wait for their world if it is loaded later by another plugin.
  - Heads appear on spots you place in game, or anywhere in the hunt's area (cuboid or WorldGuard region): on the surface or also in caves, on the blocks you allow or anywhere but the blocks you forbid. Only loaded chunks are used, and the area placement needs an area requirement.
- **Templates of spawn hunts** go further:
    - a score in **points** per template, with a points leaderboard in `/hb hunt top`, the score in `/hb progress` and `%headblocks_hunt_<hunt>_score%`;
  - **one random reward** instead of all of them, and a **chance** to carry rewards at all;
  - **traps**: a chance that the head breaks without counting and runs console commands, then moves elsewhere;
  - their own **particles**, colors included.
- **Spawn hunt options**: optional progress reset each time the hunt is enabled again (a new round after a `FIRST_WINS` winner), broadcast when heads appear, a log file of every spawn, find and trap (`spawns/<hunt>.log`), and admin debug traces with a clickable teleport.
- **Behavior menu**: a **Spawning heads** entry opens a configuration menu with every setting above, the templates, their weights and their rewards.
- **`/hb spawn <hunt>`** manages a spawn hunt: `point add|remove|list|show` to place the spots in game, `config` to change its settings, `add [n]` to make extra heads appear now, `heads` to list the heads present (click to teleport), `reroll [reset]` to draw the heads again and `clear` to remove them.
- New placeholders: `%headblocks_hunt_<hunt>_spawned%` (heads that appeared so far), `_active%` (heads present) and `_score%` (points of the player), with `_formatted` variants, plus a points leaderboard: `_scoreposition%` and `_scoretop_<pos>_<name|score>%`.

### 🚀 Improvements

- Progress, timed runs, areas, the previous hunt requirement and the per-hunt placeholders use the number of heads to find of a hunt, which is the goal for a spawn hunt.
- Heads that appeared are protected from the per-head commands (`rename`, `move`, hunt transfer, per-head rewards). Breaking one as an admin makes it appear elsewhere.

### 🐛 Bug Fixes

- `%headblocks_current%`, `%headblocks_left%` and `%current%` only count placed heads, so they stay consistent with `%headblocks_max%`.
- A database created before version 4 skipped a migration step on upgrade.
- The progress cache of a player could be partial for a hunt after finding a head.

### ⚠️ Breaking Changes

- The database is migrated to version 7 on the first start (two columns are added to the heads table). SQLite databases are backed up before the migration.
- The ordered behavior cannot be combined with spawning heads: a hunt file combining both ignores the ordered behavior.

---

Thank you for using HeadBlocks ❤️

If you find a bug or have a question, don't hesitate to :

- open an issue in [**Github**](https://github.com/AerWyn81/HeadBlocks/issues)
- or in the [**Discord**](https://discord.gg/f3d848XsQt)
- or in the [**Spigot discussion**](https://www.spigotmc.org/threads/headblocks-christmas-event-1-20-easter-eggs-multi-server-support-fully-translatable-free.533826/)
