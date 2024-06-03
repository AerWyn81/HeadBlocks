# Locations

This is an "automated" file generated when any head is placed.  
Do not modify this file without knowing what you're doing.  

Default file:
```
locations: []
```

Example of one head:
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

Each head has a UUID as key to distinguish them (l. 9).
Now for each head, there's some properties:
- name : the head name, visible in GUI like options or chat for order/click. (support hex colors)
- **[A]** location : the location (X, Y, Z, world) of the head
- rewards :
  - Support 3 types: MESSAGE, COMMAND, BROADCAST
  - At this moment, should be manually configured
  - You can configure a list of rewards
- **[A]** orderIndex: index of the head defined with /hb options
- **[A]** hitCount: number of authorized click on the head (global) defined with /hb options

Properties marked **[A]** are generated automatically, don't change the value unless you know what you're doing.