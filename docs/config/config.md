# Config.yml

Each configuration is explained in detail below:

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
  settings:
    hostname: localhost
    database: ''
    username: ''
    password: ''
    port: 3306
    ssl: false
```

By default, all data is stored locally in SQLite in the file `plugins/HeadBlocks/headblocks.db`. If **enabled**, it is
possible to connect a remote database in MySQL:

- hostname: `localhost` or `url` to the remote database
- database: database name
- username: user used for connection with read/write access
- password: password used for connection
- port: 3306 port used for connection (_default is 3306_)
- ssl: enable ssl requests

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

#### Prevent commands if tieredRewards contains the current number

```
preventCommandsOnTieredRewardsLevel: false
```

By activating this configuration, if the number of heads the player has is part of a tieredRewards, the commands when
clicking the head will not be executed.  
Example:
> I have 40 heads and the headClick command is "eco give %player% 500"  
At tiered rewards I have (at 10 heads): "eco give %player% 2000"  
Instead of giving both, that will just give 2000 and not the 500 + 2000

#### Prevent head click messages when any reward is given

```
disableHeadMessagesAfterRewarding: false
```

Used to prevent any headclick.messages when any reward is given to the player (concerns: headclick.commands, rewards and tieredRewards) 

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
```

It is possible to set up reward levels. When the player reaches the level, that will send a message containing several
lines and execute a list of commands. You can also define a number of slots required to receive the reward with `slotsRequired` per level.

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

?> Hex colors (format: {#ffffff}), placeholders and centering (use: {center}) are supported

#### Holograms

```
holograms:
  plugin: DEFAULT (or DECENT for DecentHolograms or HD for HolographicDisplays or CMI for CMI)
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
- plugin: you can specify some other plugin to handle holograms like DecentHolograms or CMI
- heightAboveHead: height from the top of head and bottom of the last line of the hologram (support decimals)
- enabled: set enabled or disabled found or noFound hologram
- lines: lines displayed in the hologram

?> Hex colors (format: {#ffffff}) supported

!> Placeholders aren't supported

#### Internal Task

```
internalTask:
  delay: 20 # delay in ticks (1sec = 20 ticks)
  hologramParticlePlayerViewDistance: 16
```

This section affects the internal timer of the plugin. You can modify these values to improve the performance but it may affect the plugin's operation.
- delay: in ticks when plugin will check for player around a head
- hologramParticlePlayerViewDistance: view distance for hologram and particles displayed to the player

#### Other configurations

With the `/hb options` command, you can set additional parameters for the heads easily with a GUI. You have two choices:
- **Order:**  
  Order allows you to define an order in which the player will have to click on the heads in order to complete the event. In order to have clear instructions for your players, once the order is defined, you have to go in the configuration file `locations.yml` and name the heads. The name will appear in the chat when a player clicks on a head if the order is incorrect. It is also possible to put several heads at the same order level. **The order is defined as follows: the order in the GUI is authoritative, the smaller the value, the higher the priority.**


- **ClickCounter:**  
  The clickCounter allows to define a maximum number of clicks on the head. It is global. If the number is reached, the head is no longer clickable and the rewards are not distributed. If you increase the number, it will be possible for your players to click on it again.