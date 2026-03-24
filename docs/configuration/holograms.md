# Holograms

Display text holograms above heads showing whether a player has found them or not.

```yaml
holograms:
  plugin: DEFAULT
  heightAboveHead: 0.4
  found:
    enabled: true
    lines:
      - "&a&lFound"
  notFound:
    enabled: true
    lines:
      - "&c&lNot found"
  advanced:
    foundPlaceholder: "&a&lFound"
    notFoundPlaceholder: "&c&lNot found"
    lines:
      - "%state% &7(%current%/%max%)"
```

## Plugin Mode

- **DEFAULT**: Uses Minecraft TextDisplay (simple text, no placeholders)
- **ADVANCED**: Supports placeholders (requires [PacketEvents](https://www.spigotmc.org/resources/packetevents-api.80279/))

## Options

- **heightAboveHead**: distance between the top of the head and the bottom of the hologram (supports decimals)
- **enabled**: enable or disable hologram for found/not found heads
- **lines**: text lines displayed in the hologram

## Advanced Mode

In advanced mode, you can use an internal `%state%` placeholder that automatically resolves to `foundPlaceholder` or `notFoundPlaceholder` based on the player's status.

Advanced mode also automatically hides holograms when not in the player's field of vision.

{% hint style="info" %}
Hex colors (`{#ffffff}`) and PlaceholderAPI placeholders are supported.
{% endhint %}
