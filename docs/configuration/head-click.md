# Head Click

This section covers all interactions when a player clicks on a head. All options are under the `headClick` key in `config.yml`.

## Messages

```yaml
headClick:
  messages:
    - '&aFirstLineMessage'
    - '{#aa41d5}SecondLineMessage'
    - '&aProgression of &e%player%&7: %progress% &e%current%&7/&e%max%'
```

Send multi-line messages to players when they click on a head. Supports color codes, plugin placeholders, and [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) placeholders.

## Title

```yaml
headClick:
  title:
    enabled: false
    firstLine: ''
    subTitle: ''
    fadeIn: 0
    stay: 50
    fadeOut: 0
```

Display a title on the player's screen when they click a head:

- **firstLine**: main title text
- **subTitle**: subtitle text
- **fadeIn / stay / fadeOut**: duration in ticks (20 ticks = 1 second)

{% hint style="info" %}
Hex colors (`{#ffffff}`), placeholders, and text centering (`{center}`) are supported.
{% endhint %}

## Firework

```yaml
headClick:
  firework:
    enabled: false
    colors:
      - '121, 51, 32'
      - '10, 154, 15'
    fadeColors:
      - '121, 51, 32'
      - '10, 154, 15'
    flicker: true
    power: 0
```

Create a firework explosion when a head is clicked:

- **colors / fadeColors**: list of RGB colors (format: `'R, G, B'`). Leave empty for random colors.
- **flicker**: whether the particles flicker
- **power**: `0` = explodes at head location, `1+` = explodes at height based on power

## Particles

```yaml
headClick:
  particles:
    enabled: false
    alreadyOwn:
      type: VILLAGER_ANGRY
      colors:
        - '121, 51, 32'
        - '10, 154, 15'
      amount: 1
```

Particles displayed when a player clicks a head they've already found:

- **type**: particle type (see [Particles Reference](../reference/particles.md))
- **colors**: only works for `REDSTONE` type (format: `'R, G, B'`)
- **amount**: number of particles

## Sound

```yaml
headClick:
  sounds:
    alreadyOwn: block_note_block_didgeridoo
    notOwn: block_note_block_bell
```

Play different [sounds](../reference/sounds.md) depending on whether the player has already found the head. Leave empty for no sound.

## Commands

```yaml
headClick:
  commands:
    - "give %player% diamond"
  randomizeCommands: false
  slotsRequired: -1
```

Execute commands when a player clicks on a head:

- **slotsRequired**: ensure the player has enough inventory space (`-1` = no check)
- **randomizeCommands**: execute commands in random order

## Pushback

```yaml
headClick:
  pushBack:
    enabled: false
    power: 1
```

Pushes the player back when they click on a head they've already found. The `power` value controls the strength of the pushback.
