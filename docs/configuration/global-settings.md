# Global Settings

## Language

```yaml
language: en
```

Specifies which language file to use. The language file must be located in the `plugins/HeadBlocks/language/` folder and named `messages_xx.yml` where `xx` corresponds to the language code.

## Metrics

```yaml
metrics: true
```

Allows gathering anonymous usage statistics for the plugin via bStats. You can safely disable this option if needed.

{% hint style="warning" %}
Reloading the plugin has no effect on this property. To change it, restart the server.
{% endhint %}

## Heads

```yaml
heads:
  - 'default:Base64String'
  - 'hdb:Id'
  - 'player:PlayerName'
```

You can use multiple textures for HeadBlocks. Three types are available:

- **default**: Visit [Minecraft-Heads](https://minecraft-heads.com/), find the head you want, scroll to the bottom, and in the **Other** section, copy the **Value**.
- **hdb**: Requires [HeadDatabase](https://www.spigotmc.org/resources/head-database.14280/). Use the head ID from the plugin.
- **player**: Use a player's name to display their head texture.

{% hint style="warning" %}
Skulls and BetterHeads plugins are not supported due to API incompatibility.
{% endhint %}

When using `/hb give <playerName>`, you can specify which HeadBlock to give:

- Add `*` at the end to give all HeadBlocks
- Add a `number` to give the head at that position in the config

## Head Themes

```yaml
headsTheme:
  enabled: false
  selected: "Christmas"
  theme:
    Easter:
      - 'default:<base64>'
    Halloween:
      - 'default:<base64>'
    Christmas:
      - 'default:<base64>'
    Custom:
      - ''
```

Predefined themed head texture lists. When enabled, this **overrides** the `heads` configuration above.

Each theme supports the same texture types as `heads` (default, hdb, player). You can add your own themes by following the indentation format.

{% hint style="warning" %}
Enabling a theme will not update already-placed heads.
{% endhint %}

## Progress Bar

```yaml
progressBar:
  totalBars: 100
  symbol: '|'
  notCompletedColor: '&7'
  completedColor: '&a'
```

Configures the progress bar displayed with the `%progress%` placeholder:

- **totalBars**: number of symbols displayed
- **symbol**: any character (`|`, `▮`, etc.)
- **notCompletedColor**: color for incomplete portion
- **completedColor**: color for completed portion

{% hint style="info" %}
Hex colors (format: `{#ffffff}`) and centering (use: `{center}`) are supported.
{% endhint %}

## Reset Player Data

```yaml
shouldResetPlayerData: true
```

When disabled, player progress data will **not** be deleted when a head is removed (via command or sneak+click). It is not recommended to disable this as unused data will accumulate in the database.

## Internal Task

```yaml
internalTask:
  delay: 20
  hologramParticlePlayerViewDistance: 16
```

Controls the plugin's internal task scheduler:

- **delay**: how often (in ticks) the plugin checks for nearby players around heads
- **hologramParticlePlayerViewDistance**: maximum distance at which players can see holograms and particles
