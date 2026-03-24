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

### /hb leave

Leave the current timed run.

|                |                  |
|----------------|------------------|
| **Permission** | `headblocks.use` |
| **Sender**     | Player           |

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

List all spawned heads (grouped by hunt, filterable).

|               |                   |
|---------------|-------------------|
| **Alias**     | `l`               |
| **Arguments** | `(huntId) (page)` |

### /hb stats

Show heads found by player (grouped by hunt, filterable).

|               |                            |
|---------------|----------------------------|
| **Alias**     | `s`                        |
| **Arguments** | `(player) (huntId) (page)` |

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

## Hunt Commands

All hunt commands require `headblocks.admin`.

### /hb hunt create

Create a new hunt (alphanumeric + hyphens only).

|               |          |
|---------------|----------|
| **Arguments** | `<name>` |

### /hb hunt delete

Delete a hunt. Default: removes heads from world and resets progress. With `--keepHeads`: moves heads and progress to fallback hunt.

|               |                                              |
|---------------|----------------------------------------------|
| **Arguments** | `<name> --confirm [--keepHeads] [--fallback <hunt>]` |

{% hint style="warning" %}
The default hunt cannot be deleted.
{% endhint %}

### /hb hunt enable

Set a hunt state to ACTIVE.

|               |          |
|---------------|----------|
| **Arguments** | `<name>` |

### /hb hunt disable

Set a hunt state to INACTIVE.

|               |          |
|---------------|----------|
| **Arguments** | `<name>` |

### /hb hunt list

List all hunts with state and head count.

### /hb hunt info

Show detailed hunt info (state, priority, behaviors, heads, players).

|               |          |
|---------------|----------|
| **Arguments** | `<name>` |

### /hb hunt select

Set the active hunt for head placement. No argument resets to "default".

|               |          |
|---------------|----------|
| **Arguments** | `(name)` |
| **Sender**    | Player   |

### /hb hunt active

Display your currently selected hunt.

|            |        |
|------------|--------|
| **Sender** | Player |

### /hb hunt set

Reassign the targeted head to the specified hunt.

|               |          |
|---------------|----------|
| **Arguments** | `<name>` |
| **Sender**    | Player   |

### /hb hunt assign

Mass-assign heads to a hunt.

|               |                        |
|---------------|------------------------|
| **Arguments** | `<name> <all\|radius> (N)` |

### /hb hunt transfer

Transfer a specific head (by UUID) to a different hunt.

|               |                |
|---------------|----------------|
| **Arguments** | `<uuid> <name>` |

### /hb hunt progress

Show progression for a specific hunt.

|               |                    |
|---------------|--------------------|
| **Arguments** | `<name> (player)`  |

### /hb hunt top

Show leaderboard for a specific hunt.

|               |                   |
|---------------|-------------------|
| **Arguments** | `<name> (limit)`  |

### /hb hunt reset

Reset a player's progress in a specific hunt.

|               |                    |
|---------------|--------------------|
| **Arguments** | `<name> <player>`  |
