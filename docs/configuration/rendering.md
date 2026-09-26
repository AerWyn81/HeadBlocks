# Heads, Blocks and Mobs

HeadBlocks is not limited to player heads anymore: blocks, items, texts, mobs, custom items, 3D models and NPCs can be hidden too, and each hunt decides how they appear in the world.

Two things are configured separately:

- **What** is hidden comes from the catalog, the `heads` list of `config.yml`. The item you place carries it.
- **How** it is rendered comes from the hunt: placed as a real block, or as a display entity.

## The catalog

Every entry of the `heads` list is something you can place. The prefix tells what it is:

| Format                     | Content                                                       | Example             |
|----------------------------|---------------------------------------------------------------|---------------------|
| `default:<base64>`         | Head texture                                                  | `default:eyJ0ZX...` |
| `hdb:<id>` / `headdb:<id>` | Head from HeadDatabase / HeadDB                               | `hdb:1234`          |
| `player:<uuid>`            | Head of a player                                              |                     |
| `block:<MATERIAL>`         | A placeable block that stays in place (no sand, doors, beds…) | `block:LANTERN`     |
| `item:<MATERIAL>[:<cmd>]`  | Any item, with an optional custom model data                  | `item:DIAMOND:1001` |
| `frame:<MATERIAL>[:<cmd>]` | An item in an invisible item frame, on the face you click     | `frame:FILLED_MAP`  |
| `text:<text>`              | A floating text, with `&` colors and `\n` for new lines       | `text:&6Find me!`   |
| `mob:<ENTITY_TYPE>`        | A mob without AI                                              | `mob:CAT`           |

### Options

Options are added between brackets at the end of an entry, as `key=value` pairs separated by commas:

```yaml
heads:
  - 'mob:ZOMBIE[head=DIAMOND_HELMET,hand=IRON_SWORD,baby=true]'
  - 'mob:ARMOR_STAND[head=<texture>,arms=true,small=true,baseplate=false]'
  - 'text:&eSecret\n&7Look closer[billboard=fixed,background=none]'
  - 'frame:FILLED_MAP[visible=true]'
```

| Content | Option                                             | Effect                                                                           |
|---------|----------------------------------------------------|----------------------------------------------------------------------------------|
| `mob`   | `head`, `chest`, `legs`, `feet`, `hand`, `offhand` | Equipment: an item (`DIAMOND_HELMET`), or a head texture for `head`              |
| `mob`   | `baby`, `invisible`                                | `true` for a baby, or an invisible mob that only shows its equipment             |
| `mob`   | `small`, `arms`, `baseplate`                       | Armor stands only                                                                |
| `text`  | `billboard`                                        | `center` (default, always faces the player), `fixed`, `vertical` or `horizontal` |
| `text`  | `background`                                       | `none`, or a color: `#RRGGBB` / `#AARRGGBB`                                      |
| `text`  | `shadow`                                           | `true` to draw a shadow under the text                                           |
| `frame` | `visible`, `lit`                                   | Show the frame itself, use a glow item frame                                     |

### Contents of other plugins

When the plugin is installed, its contents can be hidden too:

| Format                      | Plugin      | Content                                        |
|-----------------------------|-------------|------------------------------------------------|
| `nexo:<id>`                 | Nexo        | Custom item, or furniture placed by Nexo       |
| `itemsadder:<namespace:id>` | ItemsAdder  | Custom item, or furniture placed by ItemsAdder |
| `oraxen:<id>`               | Oraxen      | Custom item, or furniture shown as in Oraxen   |
| `mythicmobs:<mob>`          | MythicMobs  | A MythicMobs mob without AI (`level` option)   |
| `modelengine:<model>`       | ModelEngine | A 3D model                                     |
| `bettermodel:<model>`       | BetterModel | A 3D model                                     |
| `citizens:<skin>`           | Citizens    | An NPC wearing the skin of a player            |
| `fancynpcs:<skin>`          | FancyNpcs   | An NPC wearing the skin of a player            |
| `znpcs:<skin>`              | ZNPCsPlus   | An NPC wearing the skin of a player            |

- NPCs accept `name` (shown above them, hidden by default), `type` (another entity type than a player) and, for Citizens and FancyNpcs, `look=true` to look at nearby players.
- Models and NPCs accept `width` and `height` to size the zone players click.
- HeadBlocks creates its own copies: they are never saved by the other plugin and don't appear in its lists.
- Nexo and ItemsAdder furniture is placed by its plugin, with its model and orientation. Seats, storage and other furniture actions are disabled.
- Oraxen furniture (display entity type) is shown with the model, size and orientation set in Oraxen, without its seats, barriers or light. Other Oraxen furniture types are shown as an item.
- Furniture keeps its own size: `scale` and spin don't apply to it. Use `width` and `height` to size the zone players click.
- Contents are loaded once their plugin is ready, and again after `/nexo reload`, `/iareload`, `/oraxen reload`, `/mm reload`…

## Taking the items

Run `/hb give` to open the catalog (or `/hb give <player>` to open it for another player):

- **Click** an item to take one, **shift + click** to take a stack.
- The **filter** button (bottom left) cycles through heads, blocks, items, texts, mobs and the contents of other plugins.
- The **hunt** button (bottom middle) links the items you take to a hunt: they are always placed in it. Choose *No hunt* to get items that follow `/hb hunt select`.
- The lore of every item shows how it will be rendered in the chosen hunt.

Placing is unchanged: in creative mode, sneak and right-click. In creative mode, sneak and left-click a head, block or mob to remove it.

## Rendering

Each hunt has a rendering mode, inherited from `config.yml` when the hunt does not set its own:

```yaml
# config.yml, or under "config:" in a hunt file
rendering:
  mode: BLOCK   # BLOCK or DISPLAY
  scale: 1.0
  glow: false
```

| Content      | `BLOCK` mode         | `DISPLAY` mode               |
|--------------|----------------------|------------------------------|
| Head texture | Head block (classic) | Floating head (item display) |
| Block        | Real block           | Block display                |
| Item         | Item display         | Item display                 |
| Frame        | Item frame           | Item frame                   |
| Text         | Text display         | Text display                 |
| Mob          | Mob without AI       | Mob without AI               |
| Other plugin | Its own render       | Its own render               |

- `scale` and `glow` apply to everything rendered as an entity.
- Display entities spin smoothly when [spin](effects.md#spin) is enabled.

### Changing the mode of an existing hunt

Each head remembers how it is rendered. The mode of a hunt (or of `config.yml`) only decides how **new** heads are placed: editing it and reloading never touches the heads already in the world.

To convert the heads already placed, use:

```
/hb hunt rendering <hunt> <block|display>
```

Every head of the hunt is converted in place: same content, same position, same player progress. A head that cannot become a block (the spot is occupied, or its world is not loaded) keeps its current look, and the command reports how many heads were kept. Run the command again once the spot is free.

A head moved to another hunt (`/hb hunt set`, `assign`, `transfer`, or `delete --keepHeads`) takes the rendering of its new hunt.

## How entities are handled

- Entities are never saved in the world. HeadBlocks spawns them when their chunk loads and respawns them if something removes them, so uninstalling the plugin leaves nothing behind.
- They cannot be damaged, pushed, set on fire, leashed, traded with or ridden.
- With `hideFoundHeads: true`, entities of found heads are hidden for the player who found them. This needs neither PacketEvents nor a restart, and there is no invisible collision left behind.
- Holograms are raised above tall contents such as mobs.
- A player finds a mob, an NPC, a model or a display with a left or a right click, like a head block.

## Limits of block contents

- Blocks that fall (sand, gravel…) or span two blocks (doors, beds, tall plants) are refused in the catalog.
- Placed block heads are protected: they don't burn, melt, decay or pop off when their support is removed, and clicking one only claims it (a lever doesn't toggle, a chest doesn't open, a cake isn't eaten).
- Only the block and its state (orientation, hanging…) are kept: the text of a sign, the patterns of a banner or the content of a container are not. Converting or moving such a head resets them.
