?> Arguments with `<>` are required and `()` are optional parameters.

# Commands and Permissions

| **Command**                        | **Sender** | **Arguments**                                | **Permission**   | **Details**                                              |
| ---------------------------------- |------------|-------------------------------------------- | ---------------  | -------------------------------------------------------- |
| **/hb help**                       | Any        |                                             |                  | Show all command help                                    |
| **/hb version**                    | Any        |                                             |                  | Show the current version                                 |
| **/hb me**                         | Player     |                                             | headblocks.use   | Show the current amount of head found                    |
| **/hb remove**                     | Any        | `<headUUID>`                                | headblocks.admin | Remove the head block according to its UUID              |
| **/hb removeAll**                  | Any        | `--confirm`                                 | headblocks.admin | Remove all head spawned (_type --confirm to confirm_)    |
| **/hb give**                       | Player     | `(player)`                                  | headblocks.admin | Give the HeadBlocks head                                 |
| **/hb reset**                      | Any        | `<player>`                                  | headblocks.admin | Reset player's data                                      |
| **/hb resetAll**                   | Any        | `--confirm`                                 | headblocks.admin | Reset all player data (_type --confirm to confirm_)      |
| **/hb list**                       | Any        | `(page)`                                    | headblocks.admin | Show list of heads spawned (with remove/teleport)        |
| **/hb stats**                      | Any        | `(player) (page)`                           | headblocks.admin | Show heads found for the player (same display as list    |
| **/hb reload**                     | Any        |                                             | headblocks.admin | Reload configuration and language file                   |