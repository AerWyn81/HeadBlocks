# Placeholders

HeadBlocks supports placeholders through [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/).

## General Placeholders

| Placeholder                                            | Description                                                           | Output         |
|--------------------------------------------------------|-----------------------------------------------------------------------|----------------|
| `%headblocks_current%`                                 | Number of heads found by the player                                   | Integer        |
| `%headblocks_left%`                                    | Number of heads remaining to be found                                 | Integer        |
| `%headblocks_max%`                                     | Total number of heads placed                                          | Integer        |
| `%headblocks_hasHead_<uuid\|name>%`                    | Whether the player has found the head (by UUID or name)               | Boolean        |
| `%headblocks_order_previous%`                          | Previous named head found (if order is set)                           | Integer or `-` |
| `%headblocks_order_current%`                           | Current named head found (if order is set)                            | Integer or `-` |
| `%headblocks_order_next%`                              | Next head to find (if order is set)                                   | Integer or `-` |
| `%headblocks_leaderboard_position%`                    | Player's position in the leaderboard                                  | Integer        |
| `%headblocks_leaderboard_<pos>_<name\|custom\|value>%` | Build a leaderboard (`custom` uses config value)                      | String         |

{% hint style="info" %}
For `hasHead` with name-based lookup, replace spaces with underscores (e.g., `%headblocks_hasHead_my_head%`).
{% endhint %}

{% hint style="warning" %}
When using name-based lookup, make sure head names are unique. If multiple heads share the same name, only the first match is checked. Prefer UUIDs for reliability.
{% endhint %}

{% hint style="warning" %}
In multi-hunt mode (2+ hunts), `%headblocks_current%` and `%headblocks_left%` will return a message asking to use per-hunt placeholders instead.
{% endhint %}

## Per-Hunt Placeholders

These placeholders work with any hunt by replacing `<huntId>` with the hunt's ID (e.g., `christmas`, `default`).

| Placeholder                            | Description                        | Output  |
|----------------------------------------|------------------------------------|---------|
| `%headblocks_hunt_<huntId>_found%`     | Heads found by the player          | Integer |
| `%headblocks_hunt_<huntId>_total%`     | Total heads in this hunt           | Integer |
| `%headblocks_hunt_<huntId>_left%`      | Heads remaining in this hunt       | Integer |
| `%headblocks_hunt_<huntId>_progress%`  | Progress bar for this hunt         | String  |
| `%headblocks_hunt_<huntId>_name%`      | Display name of the hunt           | String  |
| `%headblocks_hunt_<huntId>_state%`     | Localized state (Active, Inactive, Archived) | String  |

## Per-Hunt Timed Placeholders

These placeholders are specific to hunts with the **Timed** behavior.

| Placeholder                                       | Description                                  | Output  |
|---------------------------------------------------|----------------------------------------------|---------|
| `%headblocks_hunt_<huntId>_besttime%`              | Player's best time (formatted, or `-`)       | String  |
| `%headblocks_hunt_<huntId>_timedcount%`            | Number of completed runs by the player       | Integer |
| `%headblocks_hunt_<huntId>_timeposition%`          | Player's position in the timed leaderboard   | Integer or `-` |
| `%headblocks_hunt_<huntId>_timetop_<pos>_name%`    | Player name at position in timed leaderboard | String  |
| `%headblocks_hunt_<huntId>_timetop_<pos>_time%`    | Best time at position in timed leaderboard   | String  |
