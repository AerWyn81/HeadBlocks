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

Each file defines one hunt. The filename must match the hunt ID (e.g., `christmas.yml` for hunt ID `christmas`).

## Full Example

```yaml
id: christmas
displayName: "Christmas Hunt"
state: ACTIVE
priority: 1
icon: PLAYER_HEAD

behaviors:
  scheduled:
    start:
      date: "12/01/2026"
      time: "00:00"
    end:
      date: "12/31/2026"
      time: "23:59"

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

### Free

No constraints. Players can click heads at any time. This is the default behavior.

```yaml
behaviors:
  free:
```

### Ordered

Players must find heads in a specific order. Configure the order via `/hb options order`.

```yaml
behaviors:
  ordered:
```

- Heads with `orderIndex <= 0` are always clickable
- Clicking a head while lower-order heads are unfound shows the `Messages.OrderClickError` message

### Scheduled

The hunt is only active within a date/time range.

```yaml
behaviors:
  scheduled:
    start:
      date: "12/01/2026"
      time: "00:00"    # optional, defaults to 00:00
    end:
      date: "12/31/2026"
      time: "23:59"    # optional, defaults to 00:00
```

- **date**: required, format `MM/dd/yyyy`
- **time**: optional, format `HH:mm` — if omitted, defaults to `00:00`
- Both `start` and `end` are optional — omit one to leave that bound open

### Timed

Players race against the clock. A pressure plate starts the timer.

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

- **startPlate**: location of the pressure plate that starts the timed run
- **repeatable**: if `true`, players can replay after completion (progress is reset)
- Players can leave a run with `/hb leave`

{% hint style="info" %}
Behaviors can be combined. For example, `scheduled` + `ordered` means the hunt is date-restricted and heads must be found in order.
{% endhint %}

## Per-Hunt Configuration

Each setting under `config:` overrides the global `config.yml` value for this hunt only. **Any field left out inherits from `config.yml` at runtime.** This means hunt files stay lightweight — only override what you need.

See [Head Click](head-click.md), [Holograms](holograms.md), [Effects](effects.md), and [Rewards](rewards.md) for the available options.

## Default Hunt

The `default` hunt is special:

- Created automatically on first start
- Cannot be deleted
- Newly placed heads go into `default` unless another hunt is selected
- When a hunt is deleted with `--keepHeads`, its heads and progress are reassigned to `default` (or to a specified `--fallback` hunt)
- When a hunt is deleted without `--keepHeads`, its heads are physically removed and player progress is reset

## Backward Compatibility

If only the `default` hunt exists, the plugin behaves identically to previous versions. All commands work as before. No configuration changes needed.
