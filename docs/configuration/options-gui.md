# Options GUI

`/hb options` (alias `/hb o`) configures **per-head mechanics** of an existing hunt — Hint, Order, and Rewards — through interactive menus instead of editing YAML. This page lists every clickable element and its exact click/drop action.

For the menus that appear while **creating** a hunt (behaviors, timed, zone, scheduled), see [Behavior GUI](behaviors-gui.md).

{% hint style="info" %}
When more than one hunt exists (multi-hunt mode), every menu first shows a **hunt selection** screen — click the hunt icon to configure. With a single hunt, that step is skipped and the menu opens directly.
{% endhint %}

## Click reference legend

| Notation          | Input                                                    |
|-------------------|----------------------------------------------------------|
| **LEFT CLICK**    | Left mouse button                                        |
| **RIGHT CLICK**   | Right mouse button                                       |
| **SHIFT + CLICK** | Hold Shift while clicking                                |
| **DROP (Q)**      | Press the drop key (default `Q`) while hovering the item |

---

## Main menu

`/hb options` opens a small menu with three buttons:

| Element     | Icon      | Action                             |
|-------------|-----------|------------------------------------|
| **Hint**    | Ender Eye | LEFT CLICK → open the Hint menu    |
| **Order**   | Clock     | LEFT CLICK → open the Order menu   |
| **Rewards** | Diamond   | LEFT CLICK → open the Rewards menu |

Each button can also be reached directly: `/hb options hint`, `/hb options order`, `/hb options rewards [head]`.

---

## Hint menu — `/hb options hint`

One item per head. Hints have two independent modes — **Sound** and **Action Bar** — and the menu edits whichever mode is currently selected.

| Element   | Input                   | Action                                                        |
|-----------|-------------------------|---------------------------------------------------------------|
| Head item | **DROP (Q)**            | Cycle the edited mode (Sound ↔ Action Bar)                    |
| Head item | **LEFT CLICK**          | Enable the hint (current mode) for **this** head              |
| Head item | **SHIFT + LEFT CLICK**  | Enable the hint (current mode) for **all** heads of the hunt  |
| Head item | **RIGHT CLICK**         | Disable the hint (current mode) for **this** head             |
| Head item | **SHIFT + RIGHT CLICK** | Disable the hint (current mode) for **all** heads of the hunt |

{% hint style="info" %}
Left click only has an effect when the hint is currently **off**, and right click only when it is currently **on**. The lore shows the current mode and state.
{% endhint %}

---

## Order menu — `/hb options order`

One item per head, sorted by order position. Used by the **Ordered** behavior.

| Element   | Input           | Action                                                 |
|-----------|-----------------|--------------------------------------------------------|
| Head item | **LEFT CLICK**  | Decrease this head's order position (`orderIndex − 1`) |
| Head item | **RIGHT CLICK** | Increase this head's order position (`orderIndex + 1`) |

Heads with `orderIndex <= 0` are always clickable in-game; the lowest positions are found first.

---

## Rewards menu — `/hb options rewards [head]`

### Reward list (per head)

Each existing reward is shown as an item (Paper = message, Command Block = command, Beacon = broadcast).

| Element        | Input                  | Action                                                |
|----------------|------------------------|-------------------------------------------------------|
| Reward item    | **LEFT CLICK**         | Edit the reward — prompts for a new value in chat     |
| Reward item    | **SHIFT + LEFT CLICK** | Delete the reward                                     |
| Reward item    | **DROP (Q)**           | Copy this head's rewards to **all** heads of the hunt |
| **Add reward** | Slime Ball, LEFT CLICK | Open the reward-type selection                        |

### Reward-type selection

| Element       | Icon          | Action                                      |
|---------------|---------------|---------------------------------------------|
| **Message**   | Paper         | LEFT CLICK → prompt for a private message   |
| **Command**   | Command Block | LEFT CLICK → prompt for a console command   |
| **Broadcast** | Beacon        | LEFT CLICK → prompt for a broadcast message |

After picking a type (or editing), the menu closes and you type the value in chat. Type `cancel` to abort and reopen the menu.
