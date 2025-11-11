# Advanced Usage

### Important Note

*Only follow these instructions if requested by the developer.*

You can run the `/hb debug` command in-game with the following options:

- texture
- give
- holograms

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