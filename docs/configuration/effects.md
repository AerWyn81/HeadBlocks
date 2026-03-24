# Effects

## Floating Particles

Particles that float above heads based on whether the player has found them.

```yaml
particles:
  delay: 20
  playerViewDistance: 16
  notFound:
    enabled: true
    type: REDSTONE
    colors:
      - '121, 51, 32'
      - '10, 154, 15'
    amount: 1
  found:
    enabled: false
    type: REDSTONE
    colors:
      - '255, 0, 0'
    amount: 3
```

| Option                 | Description                                                           |
|------------------------|-----------------------------------------------------------------------|
| **delay**              | Time between each particle display in ticks (20 ticks = 1 second)     |
| **playerViewDistance** | Distance at which players can see particles                           |
| **enabled**            | Enable/disable for found/not found heads separately                   |
| **type**               | Particle type (see [Particles Reference](../reference/particles.md))  |
| **colors**             | RGB color list — only works for `REDSTONE` type (format: `'R, G, B'`) |
| **amount**             | Number of particles per effect                                        |

{% hint style="warning" %}
Particles are not supported below Minecraft server version 1.13.
{% endhint %}

## Spin Mode

```yaml
spin:
  enabled: false
  speed: 20
  linked: true
```

Make heads rotate continuously:

- **linked = true**: all heads rotate synchronously according to the `internalTask.delay` setting
- **linked = false**: each head rotates independently with a 5-tick offset, at the configured `speed`

## Hints

```yaml
hint:
  distance: 16
  frequency: 20
  sound:
    volume: 1
    sound: BLOCK_AMETHYST_BLOCK_CHIME
  actionBarMessage: "%prefix% &aPssst, a head is near &7(%arrow%) !"
```

Audio and visual hints to help players locate unfound heads. To enable hints for a specific head, use `/hb options` → Hint tab.

| Option               | Description                                                 |
|----------------------|-------------------------------------------------------------|
| **distance**         | How close a player must be to receive hints                 |
| **frequency**        | How often hints appear in ticks (lower = more frequent)     |
| **sound**            | Sound settings (volume + sound type)                        |
| **actionBarMessage** | Message shown to nearby players (`%arrow%` shows direction) |

## Hide Found Heads

```yaml
hideFoundHeads: false
```

When enabled, heads a player has already found are hidden from their view using [PacketEvents](https://www.spigotmc.org/resources/packetevents-api.80279/).

{% hint style="warning" %}
**Known limitation**: Since this feature only hides the block visually on the client side, the server still maintains the physical block. Players will encounter an invisible collision box (forcefield effect) where hidden heads are located.
{% endhint %}

{% hint style="warning" %}
Requires PacketEvents and a server restart to apply changes. Remember to disable the 'Found' hologram or leave the placeholder empty in advanced hologram mode to prevent holograms from appearing above hidden heads.
{% endhint %}

## Head Options GUI

Use the `/hb options` command to configure additional head parameters through an intuitive GUI:

- **Order**: Define a specific sequence in which players must click heads. Lower order values have higher priority. Multiple heads can share the same position.
- **Click Counter**: Set a global maximum click limit for each head. Once reached, the head stops distributing rewards.
- **Hint**: Enable proximity-based hints for specific heads.
- **Rewards**: Configure per-head rewards including messages, commands, and broadcasts.
