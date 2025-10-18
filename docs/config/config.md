# Config.yml

Each configuration is explained in the detail below:

#### Language

```
language: en
```

This setting tells the configuration which language file to use. For this, the language file must be in the
folder `plugins/HeadBlocks/language/` and must be named as follows: `messages_xx.yml` where `xx` corresponds to the
above setting. By default, `en` is used.

#### Metrics
You can safely disable this option if needed.  
However, it allows me to have information about the use of my plugin.  
I will not publish this information, it is exclusively for me  
_Metrics by bStats_
```
metrics: true
```

!> Reload the plugin has no effect on this property, to change, please restart the server


#### Heads

```
heads:
  - 'default:Base64String'
  - 'hdb:Id'
  - 'player:PlayerName'
```

You can use multiple texture on a HeadBlock. Two types are possible:
- **default**: To get the Base64String, I advise you to
visit [Minecraft-Heads](https://minecraft-heads.com/) to get this information. Find the head you want to use and in the
same page, at the bottom, in **Other** section, copy the **Value** and paste it in place of `base64String`.
- **hdb**: Plugin [HeadDatabase](https://www.spigotmc.org/resources/head-database.14280/) is needed. Now, you can
retrieve the ID of the head with the plugin and use it in place of `Id`.
- **player**: Retrieve the player name and the head will have the texture of the player head.

!> Note: Skulls and BetterHeads plugins cannot be supported because their API's is not working properly.

When using the command `/hb give <playerName>`, you can specify what HeadBlock you want:
- add a `*` at the end of the command and you will get all HeadBlocks
- add a `number` at the end of the command and will get the head in order in the config

!> Note: If you have only one head configured, just use the simple command /hb give

#### Heads theme
```
# When enabling this, that will override above heads configuration
# If you want some idea for default heads textures or customize yours, you can define that below
# This configuration will not be updated on change, so if you want to add some another themes, simply follow the correct indentation and add your
# Same configurations than above head textures
# /!\ Will not update heads already placed /!\
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

It is possible to float particles over the head if the player has found or not found the head if **enabled**:

- enabled: if particle found/not found is enabled
- delay: time between each particle display (the longer the time, the less lag it can cause) (20 ticks - 1 second)
- playerViewDistance: the range at which players can see the particles
- type: particle type (you can see the list [here](reference/particles.md))
- colors: only work for REDSTONE particle type
  - can be a list of colors (format: 'r, g, b')
- amount: number of particles displayed

#### Reset player data

```
shouldResetPlayerData: true
```

By disabling this configuration, when a head is deleted (from the command or sneak), players' data will not be deleted.
It is not recommended deactivating it because the data will never be deleted (not used atm).

#### Prevent messages if tieredRewards contains the current number

```
preventMessagesOnTieredRewardsLevel: false
```

By activating this configuration, messages on `headClick` configuration section will not be sent to the player if the number of heads the player has is part of a tieredRewards.
Example:
> I want it to show the normal "You found a head" messages for rewards 1-14.  
> And then for reward 15 only show the "Congrats you found 15 heads!" which is what is configured with TieredRewards

#### Prevent commands if tieredRewards contains the current number

```
preventCommandsOnTieredRewardsLevel: false
```

By activating this configuration, if the number of heads the player has is part of a tieredRewards, the commands when
clicking the head will not be executed.  
Example:
> I have 40 heads and the headClick command is "eco give %player% 500"  
> At tiered rewards I have (at 10 heads): "eco give %player% 2000"  
> Instead of giving both, that will just give 2000 and not the 500 + 2000

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

It is possible to set up reward levels. When the player reaches the level, that will send a message containing several
lines and execute a list of commands.   
You can also define a number of slots required to execute the command with `slotsRequired`,
and you can randomize the commands executed.

?> Hex colors, placeholders and centering (use: {center}) are supported

!> It is not yet possible to trigger a level with rewards if the player has already passed it (by doing the
configuration later for example). Make sure that you don't have to add levels afterwards or they won't be triggered.

## Headclick

_The following configuration is divided into several parts. This is the biggest one and it is the one that allows to
customize the interactions when a player clicks on the head_

#### Headclick - Message

```
headClick:
  messages:
    - '&aFirstLineMessage'
    - '{#aa41d5}SecondLineMessage'
    - '&aProgression of &e%player%&7: %progress% &e%current%&7/&e%max%'
```

As shown above, it is possible to send a message with several lines to a player when he clicks on the head. This message
can be colored and can contain the placeholders of the plugin
or [PlaceHolderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/).

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

It is also possible to send a title when the head is clicked, if `enabled`, to the player highly configurable:

- firstLine: first line of the title
- subTitle: second line of the title
- fadeIn: how long it takes to display in ticks (20 ticks -> 1 second)
- stay: how long it stays displayed in ticks
- fadeOut: how long it takes to disappear in ticks

?> Hex colors (format: {#ffffff}), placeholders and centering (use: {center}) are supported

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

This configuration, if `enabled`, allows to make an explosion of particles, like those with fireworks when the head is
clicked with several options:

- colors/fadeColors:
  - if empty, that will generate a random list of colors
  - else a list of colors (format: 'red, green, blue')
- flicker: is the firework flickering
- power:
  - set to 0: the firework will explode on the head
  - set to more: the firework will explode in height according to the power (1 and 2 is vanilla)

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

If `enabled`, you can define [particles](reference/particles.md) which will be displayed when the player will click on
the head that it has already:
- type: particle type (you can see the list [here](reference/particles.md))
- colors: only work for REDSTONE particle type
  - can be a list of colors (format: 'r, g, b')
- amount: number of particles displayed

#### Headclick - Sound

```
headClick:
  sounds:
    alreadyOwn: block_note_block_didgeridoo
    notOwn: block_note_block_bell
```

When clicking, it is possible to send two different [sounds](reference/sounds.md) to the player depending on whether he
already has the head or not. Leave empty for no sound.

#### Headclick - Commands

```
headClick:
  commands:
    - "give %player% diamond"
  randomizeCommands: false
  slotsRequired: -1
```

It is possible to execute a command list when a player clicks on the head. 
You can also define a number of slots required to execute the command with `slotsRequired`,
and you can randomize the commands executed.

?> Hex colors (format: {#ffffff}), placeholders, and centering (use: {center}) are supported

#### Headclick - Pushback

```
headClick:
  pushBack:
    enabled: false
    power: 1
```

Allows setting the pushback power on clicking the head when it has already been found.

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
```

It is possible to add holograms over the head and show it if the player has found the head or not. Can be configurable by:
- plugin: 
  - DEFAULT - use TextDisplay
  - DECENT - DecentHolograms
  - CMI - CMI
  - FH - FancyHolograms
- heightAboveHead: height from the top of head and bottom of the last line of the hologram (support decimals)
- enabled: set enabled or disabled found or noFound hologram
- lines: lines displayed in the hologram

?> Hex colors (format: {#ffffff}) supported

!> Placeholders aren't supported

#### Spin mode
```
spin:
  enabled: false
  speed: 20
  linked: true
````
By activating spin mode, the heads turn on themselves.  
If `linked` mode is enabled, heads will rotate identically,
according to the delay configured in the section below `internalTask.delay`.  
If `linked` mode is disabled, heads will rotate with a delay (five ticks between) at a configurable speed according to the value defined in `speed`.

#### Hint

```
hint:
  sound:
    frequency: 5
    volume: 1
    sound: BLOCK_AMETHYST_BLOCK_CHIME
```

Option to set a sound when a head is not found to give the player a hint.  
To make a head emit a sound, it must be activated in the `/hb options`, Hint tab.  
It is possible to change the `frequency` of the sound (the smaller the value,
the more frequent the sound will be; the larger the value, the less frequent the sound will be).
It is possible to increase the `volume` and `type of sound`.

#### Internal Task

```
internalTask:
  delay: 20 # delay in ticks (1sec = 20 ticks)
  hologramParticlePlayerViewDistance: 16
```

This section affects the internal timer of the plugin. You can modify these values to improve the performance, but it may affect the plugin's operation.
- delay: in ticks when plugin will check for player around a head
- hologramParticlePlayerViewDistance: view distance for hologram and particles displayed to the player

#### Other configurations

With the `/hb options` command, you can set additional parameters for the heads easily with a GUI. You have two choices:
- **Order:**  
  Order allows you to define an order in which the player will have to click on the heads to complete the event. In order to have clear instructions for your players, once the order is defined, you have to go in the configuration file `locations.yml` and name the heads. The name will appear in the chat when a player clicks on a head if the order is incorrect. It is also possible to put several heads at the same order level. **The order is defined as follows: the order in the GUI is authoritative, the smaller the value, the higher the priority.**


- **ClickCounter:**  
  The clickCounter allows defining a maximum number of clicks on the head.
  It is global.
  If the number is reached, the head is no longer clickable and the rewards are not distributed.
  If you increase the number, it will be possible for your players to click on it again.