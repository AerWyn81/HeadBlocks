# Config.yml

Each configuration is explained in detail below:

#### Language

```
language: en
```

This setting tells the configuration which language file to use. For this, the language file must be in the
folder `plugins/HeadBlocks/language/` and must be named as follows: `messages_xx.yml` where `xx` corresponds to the
above setting. By default, `en` is used.

#### Heads

```
heads:
  - 'default:Base64String'
  - 'hdb:Id'
```

You can use multiple texture on a HeadBlock. Two types are possible:
- **default**: to get the Base64String, I advise you to
visit [Minecraft-Heads](https://minecraft-heads.com/) to get this information. Find the head you want to use and in the
same page, at the bottom, in **Other** section, copy the **Value** and paste it in place of `base64String`.
- **hdb**: Plugin [HeadDatabase](https://www.spigotmc.org/resources/head-database.14280/) is needed. Now, you can
retrieve the ID of the head with the plugin and use it in place of `Id`.

When using the command `/hb give <yourName>`, you can specify what HeadBlock you want:
- add a `*` at the end of the command and you will get all HeadBlocks
- add a `number` at the end of the command and will get the head in order in the config

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

For a multi-server configuration, it is highly recommended to have a Redis database. This configuration, once `enabled`,
allows you to configure:

- hostname: `localhost` or `url` to the remote redis database
- database: redis database number (0 - 15)
- password: password used for connection
- port: port used for connection (_default is 6379_)

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
```

By default, all data is stored locally in SQLite in the file `plugins/HeadBlocks/headblocks.db`. If **enabled**, it is
possible to connect a remote database in MySQL:

- hostname: `localhost` or `url` to the remote database
- database: database name
- username: user used for connection with read/write access
- password: password used for connection
- port: 3306 port used for connection (_default is 3306_)

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

?> Hex colors are supported

#### Particles (floating on the head)

!> Particles are not supported below Minecraft server version 1.13

```
particles:
  enabled: true
  delay: 20
  playerViewDistance: 16
  notFound:
    type: REDSTONE
    colors:
      - '121, 51, 32'
      - '10, 154, 15'
    amount: 1
```

It is possible to float particles over the head if the player has not found the head if **enabled**:

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
It is not recommended to deactivate it because the data will never be deleted.

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
```

It is possible to set up reward levels. When the player reaches the level, that will send a message containing several
lines and execute a list of commands.

?> Hex colors and placeholders are supported

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

!> Title is not supported below Minecraft server version 1.10

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

?> Hex colors and placeholders are supported

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

!> Title is not supported below Minecraft server version 1.13

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
```

It is possible to execute a command list when a player clicks on the head.

?> Hex colors and placeholders are supported

