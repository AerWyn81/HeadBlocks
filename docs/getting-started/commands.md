# Commands and Permissions

{% hint style="info" %}
Arguments with `<>` are required and `()` are optional parameters.
{% endhint %}

## Player Commands

### /hb help

Display command help.

|                |                  |
|----------------|------------------|
| **Alias**      | `h`              |
| **Permission** | `headblocks.use` |

### /hb progress

Show heads found for the executing player or another player.

|                  |                                      |
|------------------|--------------------------------------|
| **Alias**        | `p`                                  |
| **Arguments**    | `(player)`                           |
| **Sender**       | Player                               |
| **Permission**   | `headblocks.commands.progress`       |
| **Other player** | `headblocks.commands.progress.other` |

### /hb top

Display leaderboard of heads found.

|                |                           |
|----------------|---------------------------|
| **Alias**      | `t`                       |
| **Arguments**  | `(limit)`                 |
| **Permission** | `headblocks.commands.top` |

## Admin Commands

All admin commands require `headblocks.admin`.

### /hb give

Give a HeadBlock item to a player.

|               |                               |
|---------------|-------------------------------|
| **Alias**     | `g`                           |
| **Arguments** | `<player> (* or head number)` |
| **Sender**    | Player                        |

### /hb remove

Remove head by UUID or remove targeted head.

|               |              |
|---------------|--------------|
| **Arguments** | `(headUUID)` |

### /hb removeAll

Remove all spawned heads.

|               |             |
|---------------|-------------|
| **Arguments** | `--confirm` |

### /hb reset

Reset player's progress.

|               |            |
|---------------|------------|
| **Arguments** | `<player>` |

### /hb resetAll

Reset all player progress.

|               |             |
|---------------|-------------|
| **Arguments** | `--confirm` |

### /hb list

List all spawned heads with remove/teleport options.

|               |          |
|---------------|----------|
| **Alias**     | `l`      |
| **Arguments** | `(page)` |

### /hb stats

Show heads found by player.

|               |                   |
|---------------|-------------------|
| **Alias**     | `s`               |
| **Arguments** | `(player) (page)` |

### /hb reload

Reload configuration and language files.

### /hb move

Move targeted HeadBlock to another location.

|               |                        |
|---------------|------------------------|
| **Alias**     | `m`                    |
| **Arguments** | `--confirm / --cancel` |
| **Sender**    | Player                 |

### /hb export

Export database to SQL file with player data.

|               |                                |
|---------------|--------------------------------|
| **Alias**     | `e`                            |
| **Arguments** | `<database> <MySQL or SQLite>` |

### /hb rename

Rename targeted head.

|               |          |
|---------------|----------|
| **Alias**     | `r`      |
| **Arguments** | `(name)` |
| **Sender**    | Player   |

### /hb options

Configure head mechanics via GUI.

|               |                                    |
|---------------|------------------------------------|
| **Alias**     | `o`                                |
| **Arguments** | `counter / hint / order / rewards` |
| **Sender**    | Player                             |

### /hb version

Show current plugin version.

|           |     |
|-----------|-----|
| **Alias** | `v` |
