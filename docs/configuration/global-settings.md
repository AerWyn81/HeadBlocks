# Global Settings

Settings at the top level of `config.yml` that control general plugin behavior.

## language

```yaml
language: en
```

Specifies which language file to use. The file must be in `plugins/HeadBlocks/language/` and named `messages_xx.yml` where `xx` is the language code.

## metrics

```yaml
metrics: true
```

Allows gathering anonymous usage statistics via bStats. You can safely disable this.

{% hint style="warning" %}
Reloading the plugin has no effect on this property. Restart the server to apply changes.
{% endhint %}

## heads

```yaml
heads:
  - 'default:Base64String'
  - 'hdb:Id'
  - 'player:PlayerName'
```

Head textures used when placing HeadBlocks. Three formats are available:

| Format             | Description                                                                                                    |
|--------------------|----------------------------------------------------------------------------------------------------------------|
| `default:<base64>` | Base64 encoded texture. Get the **Value** from [Minecraft-Heads](https://minecraft-heads.com/) (Other section) |
| `hdb:<id>`         | Head ID from [HeadDatabase](https://www.spigotmc.org/resources/head-database.14280/) plugin                    |
| `player:<name>`    | A player's name — uses their skin head texture                                                                 |

{% hint style="warning" %}
Skulls and BetterHeads plugins are not supported due to API incompatibility.
{% endhint %}

When using `/hb give <playerName>`:

- Add `*` to give all HeadBlocks
- Add a `number` to give the head at that position in the config

## headsTheme

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

Predefined themed texture lists. When enabled, **overrides** the `heads` config above. Each theme uses the same texture formats as `heads`.

{% hint style="warning" %}
Will not update already-placed heads.
{% endhint %}

## progressBar

```yaml
progressBar:
  totalBars: 100
  symbol: '|'
  notCompletedColor: '&7'
  completedColor: '&a'
```

Configures the progress bar displayed with the `%progress%` placeholder.

| Option                | Description                     |
|-----------------------|---------------------------------|
| **totalBars**         | Number of symbols displayed     |
| **symbol**            | Any character (`\|`, `▮`, etc.) |
| **notCompletedColor** | Color for incomplete portion    |
| **completedColor**    | Color for completed portion     |

{% hint style="info" %}
Hex colors (`{#ffffff}`) and centering (`{center}`) are supported.
{% endhint %}

## shouldResetPlayerData

```yaml
shouldResetPlayerData: true
```

When `true`, player progress data is deleted when a head is removed (via command or sneak+click). Disabling this will cause unused data to accumulate in the database.

## hideFoundHeads

```yaml
hideFoundHeads: false
```

When enabled, heads a player has already found are hidden from their view using [PacketEvents](https://www.spigotmc.org/resources/packetevents-api.80279/).

{% hint style="warning" %}
**Known limitation**: The server still maintains the physical block. Players will encounter an invisible collision box (forcefield effect) where hidden heads are located.
{% endhint %}

{% hint style="warning" %}
Requires PacketEvents and a server restart. Remember to disable the 'Found' hologram or leave the placeholder empty in advanced hologram mode.
{% endhint %}

## preventCommandsOnTieredRewardsLevel

```yaml
preventCommandsOnTieredRewardsLevel: false
```

When enabled, `headClick` commands are **not** executed if the player's current count matches a [tieredRewards](rewards.md) milestone. Only the milestone commands run.

## preventMessagesOnTieredRewardsLevel

```yaml
preventMessagesOnTieredRewardsLevel: false
```

When enabled, `headClick` messages are **not** sent if the player's current count matches a [tieredRewards](rewards.md) milestone. Only the milestone message is shown.

## externalInteractions

```yaml
externalInteractions:
  piston: true
  water: true
  explosion: true
```

Controls protection against external block interactions that could destroy heads.

| Option        | Description                                   |
|---------------|-----------------------------------------------|
| **piston**    | Prevent pistons from pushing/destroying heads |
| **water**     | Prevent liquid flow from destroying heads     |
| **explosion** | Prevent explosions from destroying heads      |

{% hint style="info" %}
Disabling these options can improve performance, but heads will be vulnerable to the corresponding interactions.
{% endhint %}

## placeholders

```yaml
placeholders:
  leaderboard:
    prefix: ""
    suffix: ""
    nickname: false
```

Customizes the `%headblocks_leaderboard_<position>_custom%` placeholder. Supports PlaceholderAPI.

| Option       | Description                             |
|--------------|-----------------------------------------|
| **prefix**   | Text displayed before the player name   |
| **suffix**   | Text displayed after the player name    |
| **nickname** | Use player nickname instead of username |

{% hint style="warning" %}
Players must reconnect or the server must restart for changes to take effect.
{% endhint %}

See [Placeholders Reference](../reference/placeholders.md) for the full list of available placeholders.

## internalTask

```yaml
internalTask:
  delay: 20
  hologramParticlePlayerViewDistance: 16
```

Controls the plugin's internal task scheduler.

| Option                                 | Description                                                            |
|----------------------------------------|------------------------------------------------------------------------|
| **delay**                              | How often (in ticks) the plugin checks for nearby players around heads |
| **hologramParticlePlayerViewDistance** | Maximum distance at which players can see holograms and particles      |

{% hint style="info" %}
Increasing the delay or reducing the view distance can improve performance.
{% endhint %}

## gui

```yaml
gui:
  borderIcon:
    type: GRAY_STAINED_GLASS_PANE
  previousIcon:
    type: ARROW
  nextIcon:
    type: ARROW
  backIcon:
    type: SPRUCE_DOOR
  closeIcon:
    type: BARRIER
```

Customizes the icons used in all plugin GUIs.

| Icon             | Default                   | Description           |
|------------------|---------------------------|-----------------------|
| **borderIcon**   | `GRAY_STAINED_GLASS_PANE` | Border/filler slots   |
| **previousIcon** | `ARROW`                   | Previous page button  |
| **nextIcon**     | `ARROW`                   | Next page button      |
| **backIcon**     | `SPRUCE_DOOR`             | Back to previous menu |
| **closeIcon**    | `BARRIER`                 | Close menu button     |

Values must be valid [Material](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html) names.
