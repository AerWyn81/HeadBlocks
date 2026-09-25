# Heads, Blocks and Mobs

HeadBlocks is not limited to player heads anymore: blocks, items and mobs can be hidden too, and each hunt decides how they appear in the world.

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
| `mob:<ENTITY_TYPE>`        | A mob without AI                                              | `mob:CAT`           |

## Taking the items

Run `/hb give` to open the catalog (or `/hb give <player>` to open it for another player):

- **Click** an item to take one, **shift + click** to take a stack.
- The **filter** button (bottom left) cycles through heads, blocks, items and mobs.
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
| Mob          | Mob without AI       | Mob without AI               |

- `scale` and `glow` apply to everything rendered as an entity.
- Display entities spin smoothly when [spin](effects.md#spin) is enabled.
- The mode can be chosen when creating a hunt, with the **Rendering** button of the `/hb hunt create` menu.

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
- A player finds a mob or a display with a left or a right click, like a head block.

## Limits of block contents

- Blocks that fall (sand, gravel…) or span two blocks (doors, beds, tall plants) are refused in the catalog.
- Placed block heads are protected: they don't burn, melt, decay or pop off when their support is removed, and clicking one only claims it (a lever doesn't toggle, a chest doesn't open, a cake isn't eaten).
- Only the block and its state (orientation, hanging…) are kept: the text of a sign, the patterns of a banner or the content of a container are not. Converting or moving such a head resets them.
