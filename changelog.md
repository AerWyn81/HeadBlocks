# HeadBlocks v3.0.1

## What's New

> **Headline:** HeadBlocks now ships a complete **multi-hunt system**. A server is no longer limited to a single global head collection — you can create, configure and run as many independent hunts as you want, each with its own heads, behavior, rewards and progression. Most of the features below build directly on top of this new foundation.

### ✨ New Features

- **Multi-hunt system**: create, rename, delete, list and switch between multiple hunts. Each hunt owns its own heads, rewards, holograms, hints, order configuration and player progression.
- **Hunt behaviors** (cumulative — a hunt can combine several, e.g. `Ordered` + `Timed`):
    - `Free` — classic, head-by-head exploration
    - `Ordered` — heads must be found in a specific sequence
    - `Timed` — time-limited challenge with countdown, leaderboard, and an optional repeatable mode (new `/hb leave` command to abandon a running run)
    - `Scheduled` — hunt opens/closes automatically with three sub-modes:
        - `Range` — open between two dates (`MM/dd/yyyy HH:mm`)
        - `Recurring` — periodic recurrence with a configurable unit (day/week/month/year)
        - `Slots` — one or several recurring time-of-day windows
- **`/hb hunt schedule` command**: full CLI to configure scheduled mode, dates, recurrences and slots.
- **Hunt sub-menu in `/hb options`**: opening Hints / Order / Rewards now first prompts for the target hunt.
- **Multi-hunt Redis sync**: hunt creation, state and configuration are now propagated across servers in addition to player progression — full multi-server multi-hunt support.
- **HeadDB support**: [HeadDB](https://www.spigotmc.org/resources/headdb.114941/) joins HeadDatabase as a supported head provider, configurable in `config.yml`.
- **Minecraft 1.21.11 / 26.1 support**: tested and added to the supported platform versions.
- **Colored console output**: log messages now use ANSI color codes for better readability.

### 🚀 Improvements

- **`/hb stats` and `/hb list`** revamped to surface per-hunt breakdowns and clearer information.
- **Per-hunt storage of head locations**: heads are now persisted directly inside `hunts/<id>.yml` instead of the global `locations.yml`. Legacy data is automatically migrated on startup.
- **Performance with large head counts**: hologram handling and head lookups have been significantly optimized for servers with many heads.
- **No more hard dependency on XSeries**: only the modules actually used (`XSound`) are shaded, slashing the bundled code size by ~3 500 lines.
- **Improved GUI flow** for selecting a hunt when configuring Timed / Scheduled behavior.
- **New & updated placeholders** for Timed and Scheduled hunts (state, remaining time, next opening, repeatability, etc.).
- **Documentation moved to GitBook** with a full v3.0 multi-hunt section.
- **Dependencies** updated (Jedis, Gradle wrapper, …).

### 🧱 Stabilization & Code Quality

A large part of this release was spent making the new multi-hunt foundation solid:

- **Massive test coverage**: thousands of new unit and integration tests across commands, services, storages, behaviors, hooks and events.
- **Refactor from static singletons to dependency injection**: a new `ServiceRegistry` wires services explicitly, making the codebase testable and removing hidden coupling.
- **Reliability fixes**: try-with-resources on streams, `ThreadLocalRandom` instead of shared `Random`, deduplicated event/database code, safer SQLite/MySQL handling.
- **Static analysis**: Qodana and SonarQube/SonarCloud integration with a long list of resolved findings.
- **Removed click-counter feature** and its leftover GUI/DB columns (was unused and a source of bugs).

### 🐛 Bug Fixes

- Fixed `/hb removeall` leaving stale references in hunts.
- Fixed `/hb reset` and `/hb resetall` on ordered/timed hunts.
- Fixed head moves not being persisted correctly.
- Fixed Redis hunt transfer issue across servers.
- Fixed heads being loaded twice on startup.
- Fixed several edge cases in ordered hunt progression.

---

Thank you for using HeadBlocks ❤️

If you find a bug or have a question, don't hesitate to :

- open an issue in [**Github**](https://github.com/AerWyn81/HeadBlocks/issues)
- or in the [**Discord**](https://discord.gg/f3d848XsQt)
- or in the [**Spigot discussion**](https://www.spigotmc.org/threads/headblocks-christmas-event-1-20-easter-eggs-multi-server-support-fully-translatable-free.533826/)