# Locations

This is an automatically generated file created when heads are placed. **Do not modify this file unless you know what you're doing.**

## Default File

```yaml
locations: []
```

## Example

```yaml
locations:
  4848fbff-3002-46e6-98c2-14145ccb4ecb:
    name: ''
    location:
      x: -12
      y: 34
      z: 56
      world: default
    rewards:
      - type: MESSAGE
        value: '&aThis is a test message'
      - type: COMMAND
        value: give %player% gold_ingot
      - type: BROADCAST
        value: '&e%player% &afound one head!'
    orderIndex: 1
    hitCount: 12
```

## Properties

Each head has a UUID as its key identifier.

| Property       | Description                                                                            | Auto-generated |
|----------------|----------------------------------------------------------------------------------------|----------------|
| **name**       | Display name in GUIs and chat. Supports hex colors.                                    | No             |
| **location**   | X, Y, Z coordinates and world                                                          | Yes            |
| **rewards**    | Per-head rewards (MESSAGE, COMMAND, BROADCAST). Configurable via `/hb options rewards` | No             |
| **orderIndex** | Click order index, defined with `/hb options`                                          | Yes            |
| **hitCount**   | Maximum clicks allowed on the head (global), defined with `/hb options`                | Yes            |

{% hint style="warning" %}
Properties marked as auto-generated should not be changed manually unless you know what you're doing.
{% endhint %}
