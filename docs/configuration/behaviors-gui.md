# Behavior GUI

Creating a hunt with **`/hb hunt create <name>`** opens the **Behaviors** menu, which chains into the Spawn points / Random spawn, Timed and Scheduled configuration menus depending on what you select, and gives access to the **Requirements** menu. This page lists every clickable element and its exact click/drop action.

For the per-head menus opened by `/hb options` (Hint, Order, Rewards), see [Options GUI](options-gui.md). For what each behavior does at runtime, see [Hunt Files](hunts.md).

## Click reference legend

| Notation          | Input                     |
|-------------------|---------------------------|
| **LEFT CLICK**    | Left mouse button         |
| **RIGHT CLICK**   | Right mouse button        |
| **SHIFT + CLICK** | Hold Shift while clicking |

---

## Behavior selection

Toggle the behaviors you want, then validate.

| Element          | Icon          | Action                                                            |
|------------------|---------------|-------------------------------------------------------------------|
| **Requirements** | Book          | LEFT CLICK → open the Requirements menu                           |
| **Ordered**      | Lime/Gray Dye | LEFT CLICK → toggle the Ordered behavior                          |
| **Scheduled**    | Lime/Gray Dye | LEFT CLICK → toggle the Scheduled behavior                        |
| **Timed**        | Lime/Gray Dye | LEFT CLICK → toggle the Timed behavior                            |
| **Spawn points** | Lime/Gray Dye | LEFT CLICK → toggle the Spawn points behavior (unselects Ordered) |
| **Random spawn** | Lime/Gray Dye | LEFT CLICK → toggle the Random spawn behavior (unselects Ordered and Spawn points). Without an area requirement, the Requirements menu opens |
| **Validate**     | Diamond       | LEFT CLICK → create the hunt (chains into the config menus below) |

A green dye means selected, gray means not selected. If Timed or Scheduled are selected, validating opens their configuration menus in turn before the hunt is created. Spawn points and Random spawn open their own configuration first. Ordered, Spawn points and Random spawn cannot be selected together. Random spawn needs an area requirement: validating without one opens the Requirements menu, and removing the area unselects Random spawn. Requirements are configured in their own menu and the item shows how many are set.

---

## Requirements

The conditions checked when a player clicks a head of the hunt. See [Hunt Files](hunts.md#requirements) for what each one does at runtime.

| Element               | Icon                | Action                                                                             |
|-----------------------|---------------------|------------------------------------------------------------------------------------|
| **Combination**       | Comparator/Repeater | LEFT CLICK → switch between **ALL** (every requirement) and **ANY** (at least one) |
| Requirement entry     | Type icon           | **LEFT CLICK** → edit &nbsp;·&nbsp; **RIGHT CLICK** → remove                       |
| **Add a requirement** | Lime Dye            | LEFT CLICK → open the type picker                                                  |
| **Validate**          | Diamond             | LEFT CLICK → save the requirements and go back to the Behaviors menu               |
| **Back**              | Back icon           | LEFT CLICK → discard the changes and go back to the Behaviors menu                 |

The default combination is **ALL**. When a player is blocked, the click lists every blocking requirement at once.

### Type picker

| Element            | Icon           | Action                                          |
|--------------------|----------------|-------------------------------------------------|
| **Area**           | Structure Void | LEFT CLICK → configure an area                  |
| **Previous hunt**  | Chest Minecart | LEFT CLICK → configure a hunt to progress in    |
| **Permission**     | Name Tag       | LEFT CLICK → configure a permission node        |
| **Playtime**       | Clock          | LEFT CLICK → configure a required playtime      |
| **PlaceholderAPI** | Paper          | LEFT CLICK → configure a placeholder comparison |

A type whose plugin is missing shows as a Barrier and cannot be picked (PlaceholderAPI needs the plugin installed).

### Area configuration

While this menu is open, the selected area is outlined with particles in the world.

| Element               | Icon                     | Input      | Action                                                                                              |
|-----------------------|--------------------------|------------|-----------------------------------------------------------------------------------------------------|
| **Area type**         | Structure Void / Map     | LEFT CLICK | Toggle between **Cuboid** (2 corners) and **WorldGuard region**                                     |
| **Corner 1**          | Lime Concrete *(cuboid)* | LEFT CLICK | Close the menu, then click a block to set the first corner                                          |
| **Corner 2**          | Red Concrete *(cuboid)*  | LEFT CLICK | Close the menu, then click a block to set the second corner                                         |
| **WorldGuard region** | Name Tag *(WG)*          | LEFT CLICK | Close the menu, then type the region id in chat                                                     |
| **Return point**      | Ender Pearl              | LEFT CLICK | Close the menu, then sneak at the spot to set the return point *(only shown when Block exit is on)* |
| **Block exit**        | Lime/Gray Dye            | LEFT CLICK | Toggle physical confinement (push-back / teleport)                                                  |
| **Reset on leave**    | Lime/Gray Dye            | LEFT CLICK | Toggle wiping progress when the player leaves the area                                              |
| **Message display**   | Oak Sign                 | LEFT CLICK | Cycle the entry message mode: Chat → Action bar → Title                                             |
| **Validate**          | Diamond / Barrier        | LEFT CLICK | Confirm (Barrier = blocked until the area, and the return point if exit is blocked, are defined)    |
| **Back**              | Back icon                | LEFT CLICK | Discard and return to the Requirements menu                                                         |

### Previous hunt configuration

First pick the hunt in the paginated list, then set the threshold.

| Element            | Icon           | Input                   | Action                                 |
|--------------------|----------------|-------------------------|----------------------------------------|
| **Hunt**           | Chest Minecart | LEFT CLICK              | Reopen the hunt picker                 |
| **Heads required** | Player Head    | **LEFT CLICK**          | +1 head *(0 = the whole hunt)*         |
| **Heads required** | Player Head    | **RIGHT CLICK**         | −1 head                                |
| **Heads required** | Player Head    | **SHIFT + LEFT CLICK**  | +10 heads                              |
| **Heads required** | Player Head    | **SHIFT + RIGHT CLICK** | −10 heads                              |
| **Validate**       | Diamond        | LEFT CLICK              | Confirm (only once a hunt is selected) |

### Permission configuration

| Element             | Icon     | Action                                          |
|---------------------|----------|-------------------------------------------------|
| **Permission node** | Name Tag | LEFT CLICK → close the menu and type it in chat |
| **Validate**        | Diamond  | LEFT CLICK → confirm (needs a node)             |

### Playtime configuration

| Element               | Icon    | Input                   | Action                                |
|-----------------------|---------|-------------------------|---------------------------------------|
| **Required playtime** | Clock   | **LEFT CLICK**          | +10 min                               |
| **Required playtime** | Clock   | **RIGHT CLICK**         | −10 min                               |
| **Required playtime** | Clock   | **SHIFT + LEFT CLICK**  | +1 hour                               |
| **Required playtime** | Clock   | **SHIFT + RIGHT CLICK** | −1 hour                               |
| **Validate**          | Diamond | LEFT CLICK              | Confirm (needs a duration above zero) |

### PlaceholderAPI configuration

| Element            | Icon       | Action                                                               |
|--------------------|------------|----------------------------------------------------------------------|
| **Placeholder**    | Paper      | LEFT CLICK → close the menu and type the placeholder in chat         |
| **Comparison**     | Comparator | LEFT CLICK → cycle `=` → `!=` → `>` → `>=` → `<` → `<=` → `contains` |
| **Expected value** | Name Tag   | LEFT CLICK → close the menu and type the value in chat               |
| **Validate**       | Diamond    | LEFT CLICK → confirm (needs the placeholder and the value)           |

---

## Spawn points configuration

Opened when validating with **Spawn points** or **Random spawn** selected, or with `/hb spawn <hunt> config` for an existing hunt. Numbers: **LEFT CLICK** → +, **RIGHT CLICK** → -, hold **SHIFT** for a bigger step.

| Element                          | Icon              | Action                                                          |
|----------------------------------|-------------------|-----------------------------------------------------------------|
| **Heads present at once**        | Player Head       | Number: heads visible at the same time                          |
| **Heads to find**                | Target            | Number: the goal of each player                                 |
| **Maximum spawns**               | Hopper            | Number: total heads that can appear (below 1 = unlimited)       |
| **Completion**                   | Golden Helmet     | LEFT CLICK → per player / first to the goal wins                |
| **Once the goal is reached**     | Iron Door         | LEFT CLICK → cannot click anymore / keeps finding heads         |
| **Templates**                    | Chest             | LEFT CLICK → open the templates                                 |
| **Respawn when found**           | Lime/Gray Dye     | LEFT CLICK → toggle                                             |
| **Minimum / maximum delay**      | Clock             | Number: seconds before a found head reappears                   |
| **Spawn when the hunt starts**   | Lime/Gray Dye     | LEFT CLICK → toggle                                             |
| **Redraw regularly**             | Lime/Gray Dye     | LEFT CLICK → toggle                                             |
| **Redraw interval**              | Recovery Compass  | Number: seconds between two draws (steps of 60, SHIFT: 600)     |
| **Reset progress on redraw**     | Lime/Gray Dye     | LEFT CLICK → toggle                                             |
| **Announce spawns**              | Lime/Gray Dye     | LEFT CLICK → toggle the broadcast when heads appear             |
| **Log file**                     | Lime/Gray Dye     | LEFT CLICK → toggle `spawns/<hunt>.log`                         |
| **Debug traces**                 | Lime/Gray Dye     | LEFT CLICK → toggle the admin traces of each spawn              |
| **Score**                        | Experience Bottle | LEFT CLICK → heads found / points of the heads                  |
| **Reset progress on activation** | Lime/Gray Dye     | LEFT CLICK → toggle: everyone starts from zero when the hunt is enabled again |
| **Validate**                     | Diamond/Barrier   | LEFT CLICK → save (needs a template with a weight above 0)      |
| **Back**                         | Back icon         | LEFT CLICK → discard the changes                                |

With **Random spawn**, a second row holds its own options:

| Element                  | Icon          | Action                                                         |
|--------------------------|---------------|----------------------------------------------------------------|
| **Surface only**         | Lime/Gray Dye | LEFT CLICK → toggle (off: caves included)                      |
| **Attempts per spawn**   | Compass       | Number: spots tested before retrying 30 seconds later          |
| **Support blocks**       | Grass Block   | LEFT CLICK → forbidden blocks / only these blocks              |
| **Block list**           | Stone         | LEFT CLICK → type block names in the chat · SHIFT + RIGHT CLICK → clear |

### Templates

| Element              | Icon           | Action                                                              |
|----------------------|----------------|---------------------------------------------------------------------|
| Template             | Its content    | **LEFT CLICK** → open the template · **SHIFT + RIGHT CLICK** → remove |
| **Add a template**   | Lime Concrete  | LEFT CLICK → pick a head, block, item or mob from the catalog       |

### Template

| Element                 | Icon              | Action                                                                           |
|-------------------------|-------------------|----------------------------------------------------------------------------------|
| **Weight**              | Anvil             | Number: chance to be picked compared to the other templates                     |
| **Points**              | Experience Bottle | Number: score of the head when the hunt scores points                           |
| **Rewards**             | Chest Minecart    | LEFT CLICK → open the rewards                                                    |
| **Reward chance (%)**   | Emerald           | Number: chance that the head carries its rewards                                 |
| **One random reward**   | Lime/Gray Dye     | LEFT CLICK → toggle: give one reward picked at random instead of all             |
| **Trap chance (%)**     | TNT               | Number: chance that the head is a trap                                           |
| **Trap commands**       | Command Block     | LEFT CLICK → type a command in the chat · SHIFT + RIGHT CLICK → clear            |
| **Particle**            | Blaze Powder      | LEFT CLICK → type `NAME [amount] [r,g,b ...]` · SHIFT + RIGHT CLICK → remove     |

### Template rewards

| Element                        | Icon          | Action                                                   |
|--------------------------------|---------------|----------------------------------------------------------|
| Reward                         | Paper         | SHIFT + RIGHT CLICK → remove                             |
| **Add a MESSAGE / COMMAND / BROADCAST reward** | Lime Concrete | LEFT CLICK → type the value in the chat |

Spots are not set in this menu: add them in game with `/hb spawn <hunt> point add`. A random spawn hunt uses its area instead.

---

## Timed configuration

Opened when **Timed** is selected.

| Element             | Icon                 | Input                   | Action                                                                             |
|---------------------|----------------------|-------------------------|------------------------------------------------------------------------------------|
| **Start Plate**     | Heavy Pressure Plate | LEFT CLICK              | Close the menu, then place a pressure plate in the world to set the start location |
| **Time limit**      | Clock                | **LEFT CLICK**          | +5 seconds                                                                         |
| **Time limit**      | Clock                | **RIGHT CLICK**         | −5 seconds                                                                         |
| **Time limit**      | Clock                | **SHIFT + LEFT CLICK**  | +60 seconds                                                                        |
| **Time limit**      | Clock                | **SHIFT + RIGHT CLICK** | −60 seconds                                                                        |
| **Repeatable**      | Lime/Gray Dye        | LEFT CLICK              | Toggle whether players can replay after finishing                                  |
| **Reset on expire** | Lime/Gray Dye        | LEFT CLICK              | Toggle wiping progress when time runs out                                          |
| **Validate**        | Diamond              | LEFT CLICK              | Confirm (only shown once a start plate is set)                                     |
| **Back**            | Back icon            | LEFT CLICK              | Return to the Behaviors menu                                                       |

Time limit is `0` (Unlimited) by default, clamped to `0–3600` seconds.

---


## Scheduled configuration

Opened when **Scheduled** is selected. First pick a mode, then configure it.

### Mode selection

| Element          | Icon              | Action                                     |
|------------------|-------------------|--------------------------------------------|
| **Date Range**   | Clock             | LEFT CLICK → configure a start/end range   |
| **Weekly Slots** | Repeater          | LEFT CLICK → configure weekly time windows |
| **Recurring**    | Daylight Detector | LEFT CLICK → configure a recurring cycle   |

### Range mode

| Element      | Icon            | Action                                                                |
|--------------|-----------------|-----------------------------------------------------------------------|
| **Start**    | Lime/Gray Dye   | LEFT CLICK → type the start date (`MM/dd/yyyy HH:mm` or `MM/dd/yyyy`) |
| **End**      | Lime/Gray Dye   | LEFT CLICK → type the end date                                        |
| **Validate** | Diamond/Barrier | LEFT CLICK → confirm (needs at least a start or end)                  |

### Slots mode

| Element      | Icon            | Action                                                                                |
|--------------|-----------------|---------------------------------------------------------------------------------------|
| Slot item    | Paper           | LEFT CLICK → remove the slot                                                          |
| **Add Slot** | Lime Dye        | LEFT CLICK → type days (`MON,WED,FRI`), then a start time, then an end time (`HH:mm`) |
| **Validate** | Diamond/Barrier | LEFT CLICK → confirm (needs at least one slot)                                        |

### Recurring mode

| Element             | Icon            | Action                                                                                |
|---------------------|-----------------|---------------------------------------------------------------------------------------|
| **Recurrence**      | Compass         | LEFT CLICK → cycle the unit (year → month → week)                                     |
| **Start Reference** | Name Tag        | LEFT CLICK → type the start ref (`MM/dd` yearly, day number monthly, day name weekly) |
| **Duration**        | Clock           | LEFT CLICK → type the duration (`31d`, `2w`, `48h`)                                   |
| **Validate**        | Diamond/Barrier | LEFT CLICK → confirm (needs all three fields)                                         |

{% hint style="info" %}
For every chat prompt, type `cancel` to abort and reopen the menu.
{% endhint %}
