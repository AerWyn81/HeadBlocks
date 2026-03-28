# Effects

Visual and audio effects displayed around heads.

## Floating Particles

Particles that float above heads based on whether the player has found them.

```yaml
floatingParticles:
  notFound:
    enabled: true
    type: REDSTONE
    colors:
      - 255,0,0
      - 255,50,0
      - 255,0,50
    amount: 3
  found:
    enabled: false
    type: REDSTONE
    colors:
      - 0,255,0
      - 50,255,0
      - 0,255,50
    amount: 3
```

| Option      | Description                                                          |
|-------------|----------------------------------------------------------------------|
| **enabled** | Enable/disable for found/not found heads separately                  |
| **type**    | Particle type (see [Particles Reference](../reference/particles.md)) |
| **colors**  | RGB color list — only works for `REDSTONE` type (format: `R,G,B`)    |
| **amount**  | Number of particles per effect                                       |

{% hint style="info" %}
The display frequency and view distance are controlled by [internalTask](global-settings.md#internaltask).
{% endhint %}

## Spin

```yaml
spin:
  enabled: false
  speed: 20
  linked: true
```

Make heads rotate continuously.

| Option      | Description                                                                                                                      |
|-------------|----------------------------------------------------------------------------------------------------------------------------------|
| **enabled** | Enable/disable spin animation                                                                                                    |
| **speed**   | Rotation speed in ticks (only used when `linked` is `false`)                                                                     |
| **linked**  | `true`: all heads rotate synchronously using `internalTask.delay`. `false`: each head rotates independently with a 5-tick offset |

## Hint

```yaml
hint:
  distance: 16
  frequency: 20
  sound:
    volume: 1
    sound: BLOCK_AMETHYST_BLOCK_CHIME
  actionBarMessage: "%prefix% &aPssst, a mystery block is near! &7(%arrow%)"
```

Audio and visual hints to help players locate unfound heads. To enable hints for a specific head, use `/hb options` → Hint tab.

| Option               | Description                                                                                           |
|----------------------|-------------------------------------------------------------------------------------------------------|
| **distance**         | How close (in blocks) a player must be to receive hints                                               |
| **frequency**        | How often hints appear in ticks (lower = more frequent)                                               |
| **sound.volume**     | Sound volume                                                                                          |
| **sound.sound**      | Sound type to play                                                                                    |
| **actionBarMessage** | Message shown in action bar. Supports `%position%`, `%distance%`, `%arrow%` and standard placeholders |
