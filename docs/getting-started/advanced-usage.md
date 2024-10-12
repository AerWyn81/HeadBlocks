# Advanced usage

### Important note

*Please follow this only if the developer asked you.*

From the game, you can run the `/hb debug` command with two options:

- texture
- give

The debug argument is not listed in the in-game help, nor in autocomplete.

!> Note: **Always back up your server AND HeadBlocks database tables before doing these commands.**

### Texture

If one of your heads loses its texture (steve's head), you can restore the texture by targeting the head and using the
command: `/hb debug texture <base64Texture>`.  
By default, texture is autocompleted to help you.

### Give

If you need to give heads to players manually, you can run the `/hb debug give` command.

Full command: `/hb debug give <all|player> <all|random|ordered> <numberOfHeads>`

- `<all|player>`: You can give heads to all players or just one.
- `<all|random|ordered>`: You can give all the heads, randomly or in order with a given amount.

The algorithm reclaims the heads already owned by the player, so as not to give them back.

?> In the server console, you can see in detail what's going on.

Some examples:
> I want to give all my players, all the heads

`/hb debug give all all`

> I want to give a random head to a specific player

`/hb debug give <playerName> random 1`

> I want to give the following three heads that a player hasn't got back

`/hb debug give <playerName> ordered 3`

> I want to give all the players 30 random heads.

`/hb debug give all random 30`

!> **Very important! Rewards are not given.**  
**Click order and hit count options are not taken into account, so this will force the insertion in the database!**
