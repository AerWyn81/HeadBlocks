# Behavior GUI

Creating a hunt with **`/hb hunt create <name>`** opens the **Behaviors** menu, which chains into the Timed, Zone, and Scheduled configuration menus depending on what you select. This page lists every clickable element and its exact click/drop action.

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
| **Bounded Zone** | Lime/Gray Dye | LEFT CLICK → toggle the Zone behavior                             |
| **Ordered**      | Lime/Gray Dye | LEFT CLICK → toggle the Ordered behavior                          |
| **Scheduled**    | Lime/Gray Dye | LEFT CLICK → toggle the Scheduled behavior                        |
| **Timed**        | Lime/Gray Dye | LEFT CLICK → toggle the Timed behavior                            |
| **Validate**     | Diamond       | LEFT CLICK → create the hunt (chains into the config menus below) |

A green dye means selected, gray means not selected. If Zone, Timed, or Scheduled are selected, validating opens their configuration menus in turn before the hunt is created.

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

## Zone configuration

Opened when **Bounded Zone** is selected. While this menu is open, the selected zone is outlined with particles in the world.

| Element               | Icon                     | Input      | Action                                                                                              |
|-----------------------|--------------------------|------------|-----------------------------------------------------------------------------------------------------|
| **Zone type**         | Structure Void / Map     | LEFT CLICK | Toggle between **Cuboid** (2 corners) and **WorldGuard region**                                     |
| **Corner 1**          | Lime Concrete *(cuboid)* | LEFT CLICK | Close the menu, then click a block to set the first corner                                          |
| **Corner 2**          | Red Concrete *(cuboid)*  | LEFT CLICK | Close the menu, then click a block to set the second corner                                         |
| **WorldGuard region** | Name Tag *(WG)*          | LEFT CLICK | Close the menu, then type the region id in chat                                                     |
| **Return point**      | Ender Pearl              | LEFT CLICK | Close the menu, then sneak at the spot to set the return point *(only shown when Block exit is on)* |
| **Block exit**        | Lime/Gray Dye            | LEFT CLICK | Toggle physical confinement (push-back / teleport)                                                  |
| **Reset on leave**    | Lime/Gray Dye            | LEFT CLICK | Toggle wiping progress when the player leaves the zone                                              |
| **Message display**   | Oak Sign                 | LEFT CLICK | Cycle the entry message mode: Chat → Action bar → Title                                             |
| **Validate**          | Diamond / Barrier        | LEFT CLICK | Confirm (Barrier = blocked until the zone, and the return point if exit is blocked, are defined)    |
| **Back**              | Back icon                | LEFT CLICK | Discard and return to the Behaviors menu                                                            |

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
