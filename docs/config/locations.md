# Locations

This is an automatically generated file created when heads are placed.
Do not modify this file unless you know what you're doing.

Default file:
```
locations: []
```

Example with one head:
```
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

Each head has a UUID as its key identifier (line 9).
Each head has the following properties:

- **name**: The head's name, displayed in GUIs (options menu, chat for order/clicks). Supports hex colors.
- **[A] location**: The location (X, Y, Z, world) of the head
- **rewards**:
  - Supports 3 types: MESSAGE, COMMAND, BROADCAST
  - Can now be configured via GUI (`/hb options rewards`)
  - You can configure multiple rewards per head
- **[A] orderIndex**: The head's index, defined with `/hb options`
- **[A] hitCount**: Maximum number of clicks allowed on the head (global), defined with `/hb options`

Properties marked **[A]** are automatically generated. Don't change these values unless you know what you're doing.
