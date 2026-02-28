# How to Use

The plugin is easy to use and can be configured in just 1 minute!

1. Make sure you are OP or have the `headblocks.admin` permission.
2. Give yourself a HeadBlock using the command `/hb give`.
3. In Creative mode, sneak and right-click with the head in your hand at the desired location.
4. To remove a head, sneak and left-click on it.

!> Note: By default, any player can interact with heads. Negate the permission `headblocks.use` to prevent this.

## Multi-Hunt Mode

HeadBlocks supports creating multiple independent hunts, each with their own set of heads, player progression, and configuration.

**Quick start with hunts:**

1. Create a new hunt: `/hb hunt create christmas`
2. Select it as your active hunt: `/hb hunt select christmas` (will be selected by default if create command issued before)
3. Place heads as usual — they will automatically be assigned to the selected hunt.
4. Use `/hb hunt active` to check which hunt is currently selected.
5. Reset your selection with `/hb hunt select` (no argument) to go back to "default".

?> The hunt selection is session-based and resets to "default" when you disconnect.

You can also reassign existing heads to a hunt:

- **By looking at a head:** `/hb hunt set christmas`
- **All heads from default:** `/hb hunt assign christmas all`
- **Heads within a radius:** `/hb hunt assign christmas radius 50`
- **A specific head by UUID:** `/hb hunt transfer <uuid> christmas`

For detailed hunt configuration (per-hunt rewards, holograms, behaviors, etc.), see the [Hunt files](../config/hunts.md) page.