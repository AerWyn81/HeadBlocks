# Placeholders

HeadBlocks supports placeholders through [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/).

## Available Placeholders

| Placeholder                                            | Description                                                   | Output         |
|--------------------------------------------------------|---------------------------------------------------------------|----------------|
| `%headblocks_current%`                                 | Number of heads found by the player                           | Integer        |
| `%headblocks_left%`                                    | Number of heads remaining to be found                         | Integer        |
| `%headblocks_max%`                                     | Total number of heads placed                                  | Integer        |
| `%headblocks_hasHead_uuid%`                            | Whether the player has found the head with the specified UUID | Boolean        |
| `%headblocks_order_previous%`                          | Previous named head found (if order is set)                   | Integer or `-` |
| `%headblocks_order_current%`                           | Current named head found (if order is set)                    | Integer or `-` |
| `%headblocks_order_next%`                              | Next head to find (if order is set)                           | Integer or `-` |
| `%headblocks_leaderboard_position%`                    | Player's position in the leaderboard                          | Integer        |
| `%headblocks_leaderboard_<pos>_<name\|custom\|value>%` | Build a leaderboard (`custom` uses config value)              | String         |

{% hint style="info" %}
The UUID for `%headblocks_hasHead_uuid%` can be found using the `/hb info` command.
{% endhint %}
