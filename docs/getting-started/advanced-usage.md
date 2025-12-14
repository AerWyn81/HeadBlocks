# Advanced Usage

### Important Note

*Only follow these instructions if requested by the developer.*

You can run the `/hb debug` command in-game with the following options:

- texture
- give
- holograms
- resync

The debug argument is not listed in the in-game help or tab completion.

!> Note: **Always back up your server AND HeadBlocks database tables before running these commands.**

### Texture

If a head loses its texture (appears as Steve's head), you can restore it by targeting the head and using the command:
`/hb debug texture <base64Texture>`.
The texture parameter has tab completion to assist you.

### Give

To manually give heads to players, use the `/hb debug give` command.

Full command: `/hb debug give <all|player> <all|random|ordered> <numberOfHeads>`

- `<all|player>`: Give heads to all players or a specific player.
- `<all|random|ordered>`: Give all heads, random heads, or heads in order up to the specified amount.

The algorithm excludes heads the player already owns.

?> Detailed progress information is displayed in the server console.

Examples:
> Give all heads to all players

`/hb debug give all all`

> Give a random head to a specific player

`/hb debug give <playerName> random 1`

> Give the next three uncollected heads to a player

`/hb debug give <playerName> ordered 3`

> Give 30 random heads to all players

`/hb debug give all random 30`

!> **Very important! Rewards are not granted.**
**Click order and hit count options are ignored, forcing insertion into the database!**

### Holograms

Force holograms to reappear.

### Resync

Synchronize the database with the `locations.yml` file.  
Useful when heads are out of sync (e.g., heads exist in the database but not in the world).

Full command: `/hb debug resync <database|locations> [--force]`

#### resync database

Removes head entries from the database that no longer exist in `locations.yml`.

- **SQLite**: Automatically creates a backup before making changes (`headblocks.db.save-resync-<date>`)
- **MySQL**: Requires `--force` flag. You must backup your database manually before running this command.

?> If multiple server IDs are detected (multi-server setup), the operation will be canceled unless `--force` is used.

Examples:
> Clean orphaned database entries (SQLite)

`/hb debug resync database`

> Clean orphaned database entries (MySQL - after manual backup)

`/hb debug resync database --force`

#### resync locations

Restores head blocks in the world based on `locations.yml`. For each location:

- If the block is already a head → applies the texture from the database
- If the block is not a head → creates the head block and applies the texture

This is useful when heads have been broken or lost their texture.

Example:
> Restore all heads from locations.yml

`/hb debug resync locations`