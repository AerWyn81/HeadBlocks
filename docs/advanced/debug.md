# Debug Commands

{% hint style="warning" %}
Only follow these instructions if requested by the developer. **Always back up your server AND HeadBlocks database tables before running these commands.**
{% endhint %}

The debug argument is not listed in the in-game help or tab completion.

## Texture

Restore a head's texture if it has been lost (appears as Steve's head).

```
/hb debug texture <base64Texture>
```

Target the head and run the command. The texture parameter has tab completion.

## Give

Manually give heads to players.

```
/hb debug give <all|player> <all|random|ordered> <numberOfHeads>
```

| Parameter | Options                      | Description                                      |
|-----------|------------------------------|--------------------------------------------------|
| Target    | `all` / `<playerName>`       | Give to all players or a specific player         |
| Mode      | `all` / `random` / `ordered` | All heads, random selection, or sequential order |
| Count     | Number                       | Number of heads to give                          |

The algorithm excludes heads the player already owns.

{% hint style="info" %}
Detailed progress information is displayed in the server console.
{% endhint %}

**Examples:**

| Command                          | Description                                |
|----------------------------------|--------------------------------------------|
| `/hb debug give all all`         | Give all heads to all players              |
| `/hb debug give Steve random 1`  | Give a random head to Steve                |
| `/hb debug give Steve ordered 3` | Give the next 3 uncollected heads to Steve |
| `/hb debug give all random 30`   | Give 30 random heads to all players        |

{% hint style="warning" %}
Rewards are **not** granted. Click order and hit count options are ignored — this forces insertion into the database.
{% endhint %}

## Holograms

Force holograms to reappear.

```
/hb debug holograms
```

## Resync

Synchronize the database with `locations.yml`. Useful when heads are out of sync.

```
/hb debug resync <database|locations> [--force]
```

### resync database

Removes head entries from the database that no longer exist in `locations.yml`.

- **SQLite**: Automatically creates a backup (`headblocks.db.save-resync-<date>`)
- **MySQL**: Requires `--force` flag. Back up your database manually first.

{% hint style="info" %}
If multiple server IDs are detected (multi-server setup), the operation will be canceled unless `--force` is used.
{% endhint %}

### resync locations

Restores head blocks in the world based on `locations.yml`:

- If the block is already a head → applies the texture from the database
- If the block is not a head → creates the head block and applies the texture

```
/hb debug resync locations
```
