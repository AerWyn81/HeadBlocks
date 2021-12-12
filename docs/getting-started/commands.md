?> Arguments with `<>` are required and `()` are optional parameters.

# Commands and Permissions

| **Command**                        | **Arguments**                                | **Permission**   | **
Details**                                              |
| ---------------------------------- | -------------------------------------------- | ---------------  | -------------------------------------------------------- |
| **/hb
help**                       |                                              |                  | Show all command help                                    |
| **/hb
version**                    |                                              |                  | Show the current version                                 |
| **/hb
me**                         |                                              | headblocks.use   | Show the current amount of head found                    |
| **/hb
remove**                     | `<headUUID>`                                 | headblocks.admin | Remove the head block according to its UUID              |
| **/hb
removeAll**                  | `--confirm`                                  | headblocks.admin | Remove all head spawned (_
type --confirm to confirm_)    |
| **/hb
give**                       | `(player)`                                   | headblocks.admin | Give the HeadBlocks head                                 |
| **/hb
reset**                      | `<player>`                                   | headblocks.admin | Reset player's data                                      |
| **/hb
resetAll**                   | `--confirm`                                  | headblocks.admin | Reset all player data (_
type --confirm to confirm_)      |
| **/hb
list**                       | `(page)`                                     | headblocks.admin | Show list of heads spawned (with remove/teleport)        |
| **/hb
stats**                      | `<player> (page)`                            | headblocks.admin | Show heads found for the player (same display as list    |
| **/hb
reload**                     |                                              | headblocks.admin | Reload configuration and language file                   |