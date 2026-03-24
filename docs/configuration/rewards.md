# Rewards

## Tiered Rewards

Configure milestone rewards that trigger when a player reaches a specific head count.

```yaml
tieredRewards:
  1:
    messages:
      - ''
      - '%prefix% &e%player% &afound one head!'
      - ''
    commands:
      - 'give %player% diamond'
  10:
    messages:
      - ''
      - '%prefix% &e%player% &afound 10 heads!'
      - ''
    commands:
      - 'give %player% diamond 10'
  100:
    messages:
      - ''
      - '%prefix% &e%player% &afound &6100 heads&a!!!'
      - ''
    commands:
      - 'give %player% diamond 100'
    broadcast:
      - ''
      - '%prefix% &6%player% &afound &6100 heads&a!!!'
      - ''
    slotsRequired: 2
    randomizeCommands: false
```

Each milestone can have:

- **messages**: multi-line messages sent to the player
- **commands**: list of commands executed
- **broadcast**: messages sent to all online players
- **slotsRequired**: ensure the player has enough inventory space
- **randomizeCommands**: execute commands in random order

{% hint style="info" %}
Hex colors, placeholders, and text centering (`{center}`) are supported.
{% endhint %}

{% hint style="warning" %}
Milestone rewards cannot be triggered retroactively. If a player has already passed the threshold, adding a new milestone later won't trigger it for them.
{% endhint %}

## Prevent Overlap with Head Click

### Prevent Messages

```yaml
preventMessagesOnTieredRewardsLevel: false
```

When enabled, `headClick` messages are **not** sent if the player's current head count matches a tieredRewards milestone. Only the milestone message is shown.

### Prevent Commands

```yaml
preventCommandsOnTieredRewardsLevel: false
```

When enabled, `headClick` commands are **not** executed if the player's current count matches a milestone. Only the milestone commands run.

> **Example**: Player finds head #10. `headClick` gives 500 coins, tieredRewards gives 2000 coins. With this enabled, only the 2000 coins are given.

## Per-Head Rewards

Individual heads can have their own rewards configured via the `/hb options rewards` GUI. Each head supports:

- **MESSAGE**: Send a personal message to the player
- **COMMAND**: Execute a command as console
- **BROADCAST**: Send a message to all online players

See [Locations](locations.md) for the file format details.
