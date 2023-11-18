?> Arguments with `<>` are required and `()` are optional parameters.

# Commands and Permissions

| **Command**       | **Sender** | **Arguments**                  | **Permission**                | **Details**                                                               |
|-------------------|------------|--------------------------------|-------------------------------|---------------------------------------------------------------------------|
| **/hb help**      | Any        |                                | headblocks.use                | Show all command help                                                     |
| **/hb progress**  | Player     | `(player)`                     | headblocks.commands.progress  | Show the current amount of head found for the executing player or other   |
| **/hb top**       | Any        | `(limit)`                      | headblocks.commands.top       | Show the leaderboard of heads found (with limit)                          |
| **/hb version**   | Any        |                                | headblocks.admin              | Show the current versionUtils                                             |
| **/hb remove**    | Any        | `(headUUID)`                   | headblocks.admin              | Remove the head block according to its UUID or target head if not UUID    |
| **/hb removeAll** | Any        | `--confirm`                    | headblocks.admin              | Remove all head spawned (_type --confirm to confirm_)                     |
| **/hb give**      | Player     | `<player> (* or head number)`  | headblocks.admin              | Give the HeadBlocks head                                                  |
| **/hb reset**     | Any        | `<player>`                     | headblocks.admin              | Reset player's data                                                       |
| **/hb resetAll**  | Any        | `--confirm`                    | headblocks.admin              | Reset all player data (_type --confirm to confirm_)                       |
| **/hb list**      | Any        | `(page)`                       | headblocks.admin              | Show list of heads spawned (with remove/teleport)                         |
| **/hb stats**     | Any        | `(player) (page)`              | headblocks.admin              | Show heads found for the player (same display as list)                    |
| **/hb reload**    | Any        |                                | headblocks.admin              | Reload configuration and language file                                    |
| **/hb move**      | Player     | `--confirm / --cancel`         | headblocks.admin              | Move the HeadBlock targeted to another location                           |
| **/hb export**    | Any        | `<database> <MySQL or SQLite>` | headblocks.admin              | Export the database to an SQL file with player data                       |