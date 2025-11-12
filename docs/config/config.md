# Config.yml

Each configuration option is explained in detail below:

#### Language

```
language: en
```

This setting specifies which language file to use. The language file must be located in the
`plugins/HeadBlocks/language/` folder and named `messages_xx.yml` where `xx` corresponds to the language code. By
default, `en` is used.

#### Metrics

You can safely disable this option if needed.
However, it allows me to gather usage statistics for the plugin.
This information is private and for development purposes only.
_Metrics by bStats_
```
metrics: true
```

!> Reloading the plugin has no effect on this property. To change it, restart the server.


#### Heads

```
heads:
  - 'default:Base64String'
  - 'hdb:Id'
  - 'player:PlayerName'
```

You can use multiple textures for HeadBlocks. Three types are available:

- **default**: To get the Base64String, visit [Minecraft-Heads](https://minecraft-heads.com/). Find the head you want,
  scroll to the bottom of the page, and in the **Other** section, copy the **Value** and paste it in place of
  `base64String`.
- **hdb**: Requires the [HeadDatabase](https://www.spigotmc.org/resources/head-database.14280/) plugin. Retrieve the
  head ID from the plugin and use it in place of `Id`.
- **player**: Use a player's name to display their head texture.

!> Note: Skulls and BetterHeads plugins are not supported due to API incompatibility.

When using the command `/hb give <playerName>`, you can specify which HeadBlock to give:

- Add `*` at the end to give all HeadBlocks
- Add a `number` to give the head at that position in the config

!> Note: If you have only one head configured, simply use `/hb give`

#### Heads Theme
```
# Enabling this will override the above heads configuration
# If you want ideas for default head textures or to customize your own, define them below
# This configuration will not auto-update, so to add more themes, follow the correct indentation
# Same configuration format as above head textures
# /!\ Will not update already-placed heads /!\
headsTheme:
  enabled: false
  selected: "Christmas"
  theme:
    Easter:
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWIzZTAzNjM5OTIyYTUxZjI2OGU4NTZmYmIwZGQ0YzE5ZDIwNjg3OGIxY2U2YjVhZGRjNmI5ZjhmNDJmYWRjZCJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTRkYWU1MDc2NTcyZmMzMWNmYzdmY2RhMGZkNWI3ZWJmNGFlMWQ1NmE5YjYyNWJkNTU2ZGYxZjA5NWU5YTc3ZiJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDdmODUxOWNjNWE2MjQzZDg1ODk0OWE2YzU3MzlkY2U4NWE3NjE5YWQwZTcxYzUzNzAxYzgwZDQ3NjA0NmEwMCJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjc5ODNmYjQ5NWQzODJmOWQ4NzgyNjBkZTk5ZTRlNmNlY2Y2MThiNjdhM2I1YzIwNTFlM2ZhZDJiNjliIn19fQ=='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzA0OTY2MmEzMzY1MzNlZDY0YTExODI2NjVmZTZiMWU2YjUwYWM2ZGI3ZTk2YWE2NmY4NDcxNjhmYjkzNzI0OCJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWViMzM1MTgyZGI1ZjNiZTgwZmNjZjZlYWJlNTk5ZjQxMDdkNGZmMGU5ZjQ0ZjM0MTc0Y2VmYTZlMmI1NzY4In19fQ=='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTYyODUzMWViNWYwNTY5ZWRhZTE2YzhhNDNlYjIyZWVjZjdjMTUyMzViODM1YWUxNDE0YzI2OWNhZDEyY2E3In19fQ=='
    Halloween:
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDhhMzhiZjkyZDA0OGEzNzNjMGM3Y2Q5YjgzYTQ4YTdmOTgwZWU2NTU1ZTg1NDg3N2IzMmZlZjI2YTE5MzEzNiJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjYyZWE5N2NlN2FhNzlkNGY5YzI3ODA1N2FmYTFhMWRlZTIzNjlhNDBhZjFlYTBjNTU5Zjk2OTNiNWZiZWJjYiJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzFlMDlmNWZjZmU0MDNlYjVkZTllMThiNWMyODQzN2JlMDllOWJkY2ZiMzRhYzRiZmM3NWU4ODFhZDlhMGRkNiJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Y4NGJkYjRiNmFmZDg0ZmZkNmNmNDExYzgzNGQ4NDU4YzdlNTEzMTYwZDc5MjM4OThkNDljNmFmNzJmMjJjMSJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDBlMThiZTI3OWMxNzM2ODQwZmIxOTFmNTY0YzVhZDkzYzQxYTUwN2YyYzJkNzQwYjM0ZmVlMjEyZWVmMDQxNCJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjQyNDYwNjI4ODcyNzYzN2RjMTFmOTA4NTMyZWZlMGE0ZTI5ODBmZmM1NmNlMThjNmE1ZTljYjlkYWZlMGE0MiJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjNjNDJiN2EyZjU5ZDA3NWQyZmY1NTE2NGVmZWE2N2JhMzM3NjViZGNmMzNjYWZmNWFlODI5MGU0NzYwYmRlMyJ9fX0='
    Christmas:
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWUzYWQwMzllOTAzZTMwZjkwZGFhNjhjZWJmYzVjZWU3MmI1ZWQ4NGQ2MDQ0MzgyNDA5YzY3ZjM3NGQxNzMyYiJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTA0MGIwMzg3NjU4MDM1MGRiZjgxMzMzYWVhNjk2YTZkMmYzZjdkNTE1NmZiMGNlMjU3NzEyODNkZjYwOWE5ZiJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjRiNzhjYjhmODMyMmU3NDM0MjQyNjlhODZlZDc0MzcyY2JjMjUyYzYzMzNhYmI5NWY2ODQ5NjQzNWFhMWU2OSJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTFmYTQ3YTc2OGI3OWU2MWQ1ZjQzZWE4N2I0Y2U4ZGNlYTFjNDM5ZmU3Njg3MWFlOTk4MTMwMDMwNzRlYzNmYiJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTQwZGE5OWVhNDcxODkwN2YxNzE5MGViMTUzNTJjMGRhMGRlMGNlZTE4NmQ0ZmFiZjYxNThmODE5MjZhNTA0ZiJ9fX0='
      - 'default:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmRiMjRkYzI2MjYzMTY2M2JhMWUzZTMzOTg2NDUwMTNkYzVjZDIzMzFlYzliOWYzZWIyNjg1NWEwYjEwNGJhYSJ9fX0='
    Custom:
      - ''
```

If you wish to have a predefined list or create your own, you can define heads by theme by activating this configuration.  
Each theme can handle a list of textures, like `heads` configuration:
- **default**: To get the Base64String, I advise you to
  visit [Minecraft-Heads](https://minecraft-heads.com/) to get this information. Find the head you want to use and in the
  same page, at the bottom, in **Other** section, copy the **Value** and paste it in place of `base64String`.
- **hdb**: Plugin [HeadDatabase](https://www.spigotmc.org/resources/head-database.14280/) is needed. Now, you can
  retrieve the ID of the head with the plugin and use it in place of `Id`.
- **player**: Retrieve the player name and the head will have the texture of the player head.

Be careful, if you activate this configuration, it will overload the previous "heads" configuration.

#### Multi-server

```
redis:
  enable: false
  settings:
    hostname: localhost
    database: 0
    password: ''
    port: 6379
```

For a multi-server configuration, it is highly recommended having a Redis database. This configuration, once `enabled`,
allows you to configure:

- hostname: `localhost` or `url` to the remote redis database
- database: redis database number (0 - 15)
- password: password used for connection
- port: port used for connection (_default is 6379_)

!> Note: To make Redis work, you must configure a connection to an SQL database.

#### MySQL database

```
database:
  enable: false
  type: MySQL
  settings:
    hostname: localhost
    database: ''
    username: ''
    password: ''
    port: 3306
    ssl: false
    prefix: ''
```

By default, all data is stored locally in SQLite in the file `plugins/HeadBlocks/headblocks.db` but you can use a remote database. First enable this configuration and specify a database type, currently supported: `MySQL` or `MariaDB`.
Now you can configure all the required parameter below to connect.

- hostname: `localhost` or `url` to the remote database
- database: database name
- username: user used for connection with read/write access
- password: password used for connection
- port: 3306 port used for connection (_default is 3306_)
- ssl: enable ssl requests
- prefix: prefix for HeadBlocks tables (example: srv1_) (advice: use underscore to separate the prefix)

!> By switching SQLite to MySQL, the data is not migrated.

#### Progress bar

```
progressBar:
  totalBars: 100
  symbol: '|'
  notCompletedColor: '&7'
  completedColor: '&a'
```

Allows to configure the progress bar displayed in any message of the plugin with the `%progress%` placeholder:

- totalBars: the number of symbol that will be displayed
- symbol: any character that fit with your need (|, ▮, ...)
- notCompletedColor: color of symbol not completed
- completedColor: color of symbol completed

?> Hex colors (format: {#ffffff}) and centering (use: {center}) are supported

#### Particles (floating on the head)

!> Particles are not supported below Minecraft server version 1.13

```
particles:
  delay: 20
  playerViewDistance: 16
  notFound:
    enabled: true
    type: REDSTONE
    colors:
      - '121, 51, 32'
      - '10, 154, 15'
    amount: 1
  found:
    enabled: false
    type: REDSTONE
    # Colors is only for REDSTONE type
    # RGB format: 'red,green,blue'
    # Support multiple colors (add dash)
    colors:
    - "255,0,0"
    amount: 3
```

Particles can float above heads based on whether the player has found them or not when **enabled**:

- enabled: whether particle effect is enabled for found/not found heads
- delay: time between each particle display in ticks (longer delays reduce server load) (20 ticks = 1 second)
- playerViewDistance: distance at which players can see the particles
- type: particle type (see full list [here](reference/particles.md))
- colors: only works for REDSTONE particle type
  - can be a list of colors (format: 'r, g, b')
- amount: number of particles displayed per effect

#### Reset player data

```
shouldResetPlayerData: true
```

When this configuration is disabled, player progress data will not be deleted when a head is removed (via command or
sneak+click).
It is not recommended to disable this setting as unused data will accumulate in the database.

#### Hide found heads

```
hideFoundHeads: false
```

!> **Known Limitation**: Since this feature only hides the block visually on the client side, the server still maintains
the physical block. This means players will encounter an **invisible collision box** (forcefield effect) where hidden
heads are located. This is a technical limitation that cannot be avoided with the current approach, as block collision
is calculated server-side and cannot be disabled for specific players.

When enabled, heads that a player has already found will be hidden from their view using PacketEvents.
This feature requires the [PacketEvents](https://www.spigotmc.org/resources/packetevents-api.80279/) plugin to be
installed.
Once a player finds a head, it will disappear from their view, allowing them to focus only on heads they haven't found
yet.

!> Note: Remember to disable the 'Found' hologram or leave the placeholder empty in advanced hologram mode to prevent
holograms from appearing above hidden heads.

!> This feature requires PacketEvents to be installed and a server restart to apply changes.

#### Prevent messages if tieredRewards contains the current number

```
preventMessagesOnTieredRewardsLevel: false
```

When enabled, messages from the `headClick` configuration section will not be sent to players if their current head
count matches a tieredRewards milestone.
Example:
> Show the normal "You found a head" message for heads 1-14.
> For head 15, only show the "Congrats you found 15 heads!" message configured in TieredRewards.

#### Prevent commands if tieredRewards contains the current number

```
preventCommandsOnTieredRewardsLevel: false
```

When enabled, if a player's head count matches a tieredRewards milestone, the commands from `headClick` will not be
executed.
Example:
> The player has found 10 heads and the headClick command is "eco give %player% 500"
> In tieredRewards for 10 heads: "eco give %player% 2000"
> Only the tiered reward (2000) will be given, not both (500 + 2000)

#### TieredRewards

```
tieredRewards:
  1:
    messages:
      - ''
      - '%prefix% &e%player% &afound one head!'
      - ''
    commands:
      - 'give %player% diamond'
  10:
    messages:
      - ''
      - '%prefix% &e%player% &afound 10 heads!'
      - ''
    commands:
      - 'give %player% diamond 10'
  100:
    messages:
      - ''
      - '%prefix% &e%player% &afound &6100 heads&a!!!'
      - ''
    commands:
      - 'give %player% diamond 100'
    broadcast:
      - ''
      - '%prefix% &6%player% &afound &6100 heads&a!!!'
      - ''
    slotsRequired: 2
    randomizeCommands: false
```

You can configure milestone rewards that trigger when a player reaches a specific head count.
When triggered, the plugin will send multi-line messages and execute a list of commands.
You can also specify `slotsRequired` to ensure the player has enough inventory space,
and enable `randomizeCommands` to execute commands in random order.

?> Hex colors, placeholders, and text centering (use: {center}) are supported.

!> Note: Milestone rewards cannot be triggered retroactively if a player has already passed the threshold (for example,
if you add a new milestone later). Plan your milestones before players progress too far.

## Headclick

_The following configuration section is the largest and allows you to customize all interactions when a player clicks on
a head._

#### Headclick - Message

```
headClick:
  messages:
    - '&aFirstLineMessage'
    - '{#aa41d5}SecondLineMessage'
    - '&aProgression of &e%player%&7: %progress% &e%current%&7/&e%max%'
```

You can send multi-line messages to players when they click on a head. Messages support color codes and can include
plugin placeholders or [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) placeholders.

#### Headclick - Title

```
headClick:
  title:
    enabled: false
    firstLine: ''
    subTitle: ''
    fadeIn: 0
    stay: 50
    fadeOut: 0
```

When enabled, you can display a highly configurable title to the player when they click on a head:

- firstLine: main title text
- subTitle: subtitle text
- fadeIn: fade-in duration in ticks (20 ticks = 1 second)
- stay: display duration in ticks
- fadeOut: fade-out duration in ticks

?> Hex colors (format: {#ffffff}), placeholders, and text centering (use: {center}) are supported.

#### Headclick - Firework

```
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

When enabled, this creates a firework particle explosion when a head is clicked:

- colors/fadeColors:
  - if empty: generates a random color palette
  - if specified: list of colors (format: 'red, green, blue')
- flicker: whether the firework particles flicker
- power:
  - 0: firework explodes at head location
  - 1+: firework explodes at height based on power value (1 and 2 are vanilla heights)

#### Headclick - Particles

```
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

When enabled, you can configure [particles](reference/particles.md) that display when a player clicks on a head they've
already found:

- type: particle type (see full list [here](reference/particles.md))
- colors: only works for REDSTONE particle type
  - can be a list of colors (format: 'r, g, b')
- amount: number of particles displayed

#### Headclick - Sound

```
headClick:
  sounds:
    alreadyOwn: block_note_block_didgeridoo
    notOwn: block_note_block_bell
```

You can play different [sounds](reference/sounds.md) depending on whether the player has already found the head or not.
Leave empty for no sound.

#### Headclick - Commands

```
headClick:
  commands:
    - "give %player% diamond"
  randomizeCommands: false
  slotsRequired: -1
```

You can execute a list of commands when a player clicks on a head.
Use `slotsRequired` to ensure the player has enough inventory space before executing commands,
and enable `randomizeCommands` to execute commands in random order.

?> Hex colors (format: {#ffffff}), placeholders, and text centering (use: {center}) are supported.

#### Headclick - Pushback

```
headClick:
  pushBack:
    enabled: false
    power: 1
```

When enabled, pushes the player back when they click on a head they've already found. The `power` value controls the
strength of the pushback.

#### Holograms

```
holograms:
  plugin: DEFAULT
  heightAboveHead: 0.4
  found:
    enabled: true
    lines:
      - "&a&lFound"
  notFound:
    enabled: true
    lines:
      - "&c&lNot found"
  advanced:
    foundPlaceholder: "&a&lFound"
    notFoundPlaceholder: "&c&lNot found"
    lines:
      - "%state% &7(%current%/%max%)"
```

You can display holograms above heads showing whether a player has found them or not. Configuration options:

- plugin:
  - **DEFAULT**: uses Minecraft TextDisplay (simple text, no placeholders)
  - **ADVANCED**: supports placeholders (requires PacketEvents)
- heightAboveHead: distance between top of head and bottom of hologram (supports decimals)
- enabled: enable or disable hologram for found/not found heads
- lines: text lines displayed in the hologram
- advanced:
  - Define an internal `%state%` placeholder that automatically converts to `foundPlaceholder` if the player has found
    the head, or `notFoundPlaceholder` if they haven't.
  - Advanced mode automatically hides holograms when not in the player's field of vision.

?> Hex colors (format: {#ffffff}) are supported.

?> Placeholders are supported.

#### Spin mode
```
spin:
  enabled: false
  speed: 20
  linked: true
````

When enabled, heads will rotate continuously.

- If `linked` mode is enabled: all heads rotate synchronously according to the `internalTask.delay` setting.
- If `linked` mode is disabled: each head rotates independently with a 5-tick offset between heads, at the speed defined
  in `speed`.

#### Hint

```
hint:
  distance: 16
  frequency: 20
  sound:
    volume: 1
    sound: BLOCK_AMETHYST_BLOCK_CHIME
  actionBarMessage: "%prefix% &aPssst, a head is near &7(%arrow%) !"
```

Configure audio and visual hints to help players locate unfound heads.
To enable hints for a specific head, activate it in `/hb options`, Hint tab.

- `distance`: how close a player must be to receive hints
- `frequency`: how often hints appear in ticks (lower values = more frequent)
- `sound`: sound settings including volume and sound type
- `actionBarMessage`: message shown to nearby players (supports `%arrow%` placeholder for direction)

#### Internal Task

```
internalTask:
  delay: 20 # delay in ticks (1sec = 20 ticks)
  hologramParticlePlayerViewDistance: 16
```

This section controls the plugin's internal task scheduler. Modifying these values can improve performance but may
affect plugin behavior.

- delay: how often (in ticks) the plugin checks for nearby players around heads
- hologramParticlePlayerViewDistance: maximum distance at which players can see holograms and particles

#### Other configurations

Use the `/hb options` command to configure additional head parameters through an intuitive GUI:

- **Order:**
  Define a specific sequence in which players must click heads. For clear player instructions, name your heads in the
  `locations.yml` file after defining the order. The name appears in chat when a player clicks heads out of order.
  Multiple heads can share the same order position. **Lower order values have higher priority.**

- **ClickCounter:**
  Set a global maximum click limit for each head. Once the limit is reached, the head becomes unclickable and stops
  distributing rewards. Increasing the limit allows players to click the head again.

- **Hint:**
  Enable proximity-based audio hints for specific heads. Players near an unfound head will hear a sound. Configure sound
  settings in config.yml.

- **Rewards:**
  Configure per-head rewards including messages, commands, and broadcasts. Each head can have multiple rewards that
  trigger when a player finds it.