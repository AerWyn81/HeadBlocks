# First Steps

The plugin is easy to use and can be configured in just 1 minute!

1. Make sure you are OP or have the `headblocks.admin` permission.
2. Give yourself a HeadBlock using the command `/hb give`.
3. In Creative mode, sneak and right-click with the head in your hand at the desired location.
4. To remove a head, sneak and left-click on it.

{% hint style="warning" %}
By default, any player can interact with heads. Negate the permission `headblocks.use` to prevent this.
{% endhint %}

***

HeadBlocks supports creating multiple independent hunts, each with their own set of heads, player progression, and configuration. This allows you to create several separate trails for your players.

***

#### (Optional) Configure head textures

Before getting started, you can define which head textures will be available in your hunts.

1. Open HeadBlocks' `config.yml` file
2. Add the desired textures in the textures section\
   💡 To find textures, use [minecraft-heads.com](https://minecraft-heads.com/)
3. Go back in-game and run `/hb reload` to apply the changes

{% hint style="info" %}
The texture configuration is located in `config.yml`.
{% endhint %}

***

#### (Optional) Quick start with hunts

By default, HeadBlocks uses a single `default` hunt. Creating additional hunts lets you set up multiple independent trails, each with their own heads and player progression.

1. Create a new hunt: `/hb hunt create <name>`
2. Select it as your active hunt: `/hb hunt select <name>` _(selected by default after creation)_
3. Place heads as usual — they will automatically be assigned to the selected hunt

***

#### Useful commands

| Command           | Description                                  |
| ----------------- | -------------------------------------------- |
| `/hb hunt active` | Check which hunt is currently selected       |
| `/hb hunt select` | Reset your selection to go back to "default" |

{% hint style="info" %}
The hunt selection is session-based and resets to "default" when you disconnect.
{% endhint %}

You can also reassign existing heads to a hunt:

* **By looking at a head:** `/hb hunt set christmas`
* **All heads from default:** `/hb hunt assign christmas all`
* **Heads within a radius:** `/hb hunt assign christmas radius 50`
* **A specific head by UUID:** `/hb hunt transfer <uuid> christmas`

For detailed hunt configuration (per-hunt rewards, holograms, behaviors, etc.), see the [Hunt Files](../configuration/hunts.md) page.
