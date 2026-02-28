?> Arguments with `<>` are required and `()` are optional parameters.

# Commands and Permissions

| **Command**       | **Alias** | **Sender** | **Arguments**                      | **Permission**                                                        | **Details**                                                 |
|-------------------|:---------:|:----------:|------------------------------------|-----------------------------------------------------------------------|-------------------------------------------------------------|
| **/hb help**      |     h     |    Any     |                                    | headblocks.use                                                        | Display command help                                        |
| **/hb progress**  |     p     |   Player   | `(player)`                         | headblocks.commands.progress <br/> headblocks.commands.progress.other | Show heads found for the executing player or another player |
| **/hb top**       |     t     |    Any     | `(limit)`                          | headblocks.commands.top                                               | Display leaderboard of heads found (with optional limit)    |
| **/hb version**   |     v     |    Any     |                                    | headblocks.admin                                                      | Show current plugin version                                 |
| **/hb remove**    |           |    Any     | `(headUUID)`                       | headblocks.admin                                                      | Remove head by UUID or remove targeted head                 |
| **/hb removeAll** |           |    Any     | `--confirm`                        | headblocks.admin                                                      | Remove all spawned heads (_type --confirm to confirm_)      |
| **/hb give**      |     g     |   Player   | `<player> (* or head number)`      | headblocks.admin                                                      | Give a HeadBlock item to a player                           |
| **/hb reset**     |           |    Any     | `<player>`                         | headblocks.admin                                                      | Reset player's progress                                     |
| **/hb resetAll**  |           |    Any     | `--confirm`                        | headblocks.admin                                                      | Reset all player progress (_type --confirm to confirm_)     |
| **/hb list**      |     l     |    Any     | `(page)`                           | headblocks.admin                                                      | List all spawned heads (with remove/teleport options)       |
| **/hb stats**     |     s     |    Any     | `(player) (page)`                  | headblocks.admin                                                      | Show heads found by player (same display as list)           |
| **/hb reload**    |           |    Any     |                                    | headblocks.admin                                                      | Reload configuration and language files                     |
| **/hb move**      |     m     |   Player   | `--confirm / --cancel`             | headblocks.admin                                                      | Move targeted HeadBlock to another location                 |
| **/hb export**    |     e     |    Any     | `<database> <MySQL or SQLite>`     | headblocks.admin                                                      | Export database to SQL file with player data                |
| **/hb rename**    |     r     |   Player   | `(name)`                           | headblocks.admin                                                      | Rename targeted head                                        |
| **/hb options**   |     o     |   Player   | `counter / hint / order / rewards` | headblocks.admin                                                      | Configure head mechanics                                    |

## Hunt Commands

All hunt commands require the `headblocks.admin` permission.

| **Command**           | **Sender** | **Arguments**              | **Details**                                                               |
|-----------------------|:----------:|----------------------------|---------------------------------------------------------------------------|
| **/hb hunt create**   |    Any     | `<name>`                   | Create a new hunt (alphanumeric + hyphens only)                           |
| **/hb hunt delete**   |    Any     | `<name> (--confirm)`       | Delete a hunt (cannot delete "default"). Orphaned heads return to default |
| **/hb hunt enable**   |    Any     | `<name>`                   | Set a hunt state to ACTIVE                                                |
| **/hb hunt disable**  |    Any     | `<name>`                   | Set a hunt state to INACTIVE                                              |
| **/hb hunt list**     |    Any     |                            | List all hunts with state and head count                                  |
| **/hb hunt info**     |    Any     | `<name>`                   | Show detailed hunt info (state, priority, behaviors, heads, players)      |
| **/hb hunt select**   |   Player   | `(name)`                   | Set the active hunt for head placement. No argument resets to "default"   |
| **/hb hunt active**   |   Player   |                            | Display your currently selected hunt                                      |
| **/hb hunt set**      |   Player   | `<name>`                   | Reassign the targeted head to the specified hunt                          |
| **/hb hunt assign**   |    Any     | `<name> <all\|radius> (N)` | Mass-assign heads to a hunt (all from default, or within radius N blocks) |
| **/hb hunt transfer** |    Any     | `<uuid> <name>`            | Transfer a specific head (by UUID) to a different hunt                    |
| **/hb hunt progress** |    Any     | `<name> (player)`          | Show progression for a specific hunt (optionally for another player)      |
| **/hb hunt top**      |    Any     | `<name> (limit)`           | Show leaderboard for a specific hunt                                      |
| **/hb hunt reset**    |    Any     | `<name> <player>`          | Reset a player's progress in a specific hunt                              |