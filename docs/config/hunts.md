# Hunt Files

Each hunt is stored as a separate YAML file in the `plugins/HeadBlocks/hunts/` directory. On first start, a `default.yml` file is automatically generated from your existing `config.yml` settings.

## File Structure

```
plugins/HeadBlocks/
  hunts/
    default.yml
    christmas.yml
    halloween.yml
    ...
```

Each file defines one hunt. The file name must match the hunt ID (e.g., `christmas.yml` for hunt ID `christmas`).

## Full Example

```yaml
id: christmas
displayName: "Christmas Hunt"
state: ACTIVE
priority: 1
icon: PLAYER_HEAD

behaviors:
  scheduled:
    start: "2026-12-01T00:00"
    end: "2026-12-31T23:59"

config:
  headClick:
    messages:
      - "&aYou found a Christmas head!"
      - "&7Progress: %progress% &e%current%&7/&e%max%"
    title:
      enabled: true
      firstLine: "&c&lChristmas Hunt"
      subTitle: "&aHead found!"
      fadeIn: 10
      stay: 40
      fadeOut: 10
    sound:
      found: block_note_block_bell
      alreadyOwn: block_note_block_didgeridoo
    firework:
      enabled: true
    commands:
      - "give %player% diamond"
    eject:
      enabled: false
      power: 1

  holograms:
    found:
      enabled: true
      lines:
        - "&a&lFound"
    notFound:
      enabled: true
      lines:
        - "&c&lNot found"

  hints:
    distance: 20
    frequency: 15

  spin:
    enabled: true
    speed: 20
    linked: true

  particles:
    found:
      enabled: false
      type: REDSTONE
      amount: 1
    notFound:
      enabled: true
      type: REDSTONE
      amount: 1

  tieredRewards:
    5:
      messages:
        - "%prefix% &aYou found &e5 &aChristmas heads!"
      commands:
        - "give %player% diamond 5"
    10:
      messages:
        - "%prefix% &6%player% &afound all &e10 &aChristmas heads!"
      commands:
        - "give %player% diamond 10"
      broadcast:
        - "%prefix% &6%player% &acompleted the Christmas Hunt!"
```

## Hunt Identity

| Field         | Description                                                      | Required |
|---------------|------------------------------------------------------------------|----------|
| `id`          | Unique identifier (lowercase, alphanumeric + hyphens)            | Yes      |
| `displayName` | Name shown to players in messages and GUIs                       | Yes      |
| `state`       | Hunt state: `ACTIVE`, `INACTIVE`, or `ARCHIVED`                  | Yes      |
| `priority`    | Integer priority for display conflicts (lower = higher priority) | Yes      |
| `icon`        | Material type for GUIs (default: `PLAYER_HEAD`)                  | No       |

## States

| State        | Description                                                                                |
|--------------|--------------------------------------------------------------------------------------------|
| **ACTIVE**   | Players can interact with the hunt's heads. Holograms, particles, and hints are displayed. |
| **INACTIVE** | Players cannot interact with heads. Display elements are suppressed.                       |
| **ARCHIVED** | Preserved but not active. Functionally similar to INACTIVE.                                |

Use `/hb hunt enable <name>` and `/hb hunt disable <name>` to change state at runtime.

## Behaviors

Behaviors control how players can interact with a hunt's heads. They are evaluated as a chain — if any behavior denies a click, the entire chain denies it.

```yaml
behaviors:
  free:
  scheduled:
    start: "2026-12-01T00:00"
    end: "2026-12-31T23:59"
```

### Free

No constraints. Players can click heads at any time. This is the default behavior when no behavior is explicitly configured.

```yaml
behaviors:
  free:
```

### Ordered

Players must find heads in a specific order. Heads with a lower `orderIndex` must be found before heads with a higher one. Configure the order via `/hb options order`.

```yaml
behaviors:
  ordered:
```

- Heads with `orderIndex <= 0` are always clickable
- If a player tries to click a head while there are unfound heads with a lower order, the click is denied with the `Messages.OrderClickError` message

### Scheduled

The hunt is only active within a date range. Outside the range, clicks are denied with a configurable message.

```yaml
behaviors:
  scheduled:
    start: "2026-12-01T00:00"
    end: "2026-12-31T23:59"
```

- Before `start`: click denied with `Hunt.Behavior.ScheduledNotStarted` message
- Between `start` and `end`: click allowed
- After `end`: click denied with `Hunt.Behavior.ScheduledEnded` message

### Timed

Players race against the clock to find all heads. A pressure plate acts as the start trigger — stepping on it begins the timer. An action bar displays elapsed time and progress.

```yaml
behaviors:
  timed:
    startPlate:
      world: default
      x: 100
      y: 64
      z: 200
    repeatable: true
```

- `startPlate`: the location of the pressure plate that starts the timed run. Can be configured via the GUI (`/hb hunt create` or the timed config GUI).
- `repeatable`: if `true`, players can replay the hunt after completion (their progress is reset). Defaults to `true`.
- Players must step on the plate to start — clicking a head without an active run shows `Messages.TimedNotStarted`
- On completion, the time is saved and the `Messages.TimedCompleted` message is shown
- Players can leave a run with `/hb leave`

?> Behaviors can be combined. For example, `scheduled` + `ordered` means the hunt is date-restricted and heads must be found in order.

## Per-Hunt Configuration

Each setting under `config:` overrides the global `config.yml` value for this hunt only. **Any field left out inherits from `config.yml` at runtime.** This means hunt files stay lightweight — only override what you need.

### headClick

Customize messages, titles, sounds, fireworks, commands, and pushback when a player clicks a head in this hunt.

```yaml
config:
  headClick:
    messages:
      - "&aYou found a head!"
    title:
      enabled: false
      firstLine: ""
      subTitle: ""
      fadeIn: 0
      stay: 50
      fadeOut: 0
    sound:
      found: block_note_block_bell
      alreadyOwn: block_note_block_didgeridoo
    firework:
      enabled: false
    commands:
      - "give %player% diamond"
    eject:
      enabled: false
      power: 1
```

### holograms

Configure holograms displayed above heads for this hunt.

```yaml
config:
  holograms:
    found:
      enabled: true
      lines:
        - "&a&lFound"
    notFound:
      enabled: true
      lines:
        - "&c&lNot found"
```

### hints

Override hint distance and frequency for this hunt.

```yaml
config:
  hints:
    distance: 16
    frequency: 20
```

### spin

Override head rotation for this hunt.

```yaml
config:
  spin:
    enabled: false
    speed: 20
    linked: true
```

### particles

Override particle effects for this hunt.

```yaml
config:
  particles:
    found:
      enabled: false
      type: REDSTONE
      amount: 1
    notFound:
      enabled: true
      type: REDSTONE
      amount: 1
```

### tieredRewards

Define milestone rewards specific to this hunt. Triggers when a player reaches a head count within this hunt.

```yaml
config:
  tieredRewards:
    5:
      messages:
        - "%prefix% &aYou found &e5 &aheads!"
      commands:
        - "give %player% diamond 5"
      broadcast:
        - "%prefix% &6%player% &afound 5 heads!"
      slotsRequired: 2
      randomizeCommands: false
```

## Priority and Display Conflicts

Each head belongs to exactly one hunt. The priority determines display order in GUIs and messages when multiple hunts exist.

## Default Hunt

The `default` hunt is special:

- It is created automatically on first start
- It cannot be deleted
- Newly placed heads go into `default` unless another hunt is selected
- When a hunt is deleted with `--keepHeads`, its heads and player progress are reassigned to `default` (or to a specified `--fallback` hunt)
- When a hunt is deleted without `--keepHeads` (default mode), its heads are physically removed from the world and player progress is reset

## Backward Compatibility

If only the `default` hunt exists, the plugin behaves identically to previous versions. All commands (`/hb progress`, `/hb top`, `/hb reset`) work as before. No configuration changes are needed.
