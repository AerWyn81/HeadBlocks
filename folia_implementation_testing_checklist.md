## Folia Testing Checklist - HeadBlocks

## Testing Environment

*   **Server Type**: Folia (1.21.8)
*   **Plugin Version**: 2.8.2 (dev - customized)
*   **Status**: ⏳ In Progress

## Test Results Legend

*   ✅ **PASSED** - Feature works correctly
*   ❌ **FAILED** - Feature has errors/issues
*   ⏸️ **SKIPPED** - Not tested yet
*   ⚠️ **PARTIAL** - Works but with minor issues

## 1\. Basic Configuration Features

### 1.1 Heads Theme

*   ✅ **headsTheme** - Theme switching works correctly
    *   Easter theme loads and displays correctly
    *   Halloween theme loads and displays correctly
    *   Christmas theme loads and displays correctly
    *   Custom theme works
    *   Theme switching doesn't break existing heads

### 1.2 Progress Bar

*   ✅ **progressBar** - Progress bar displays correctly
    *   Progress bar shows correct completion percentage
    *   Colors display correctly (completed/not completed)
    *   Symbol displays correctly
    *   Works in placeholders (%progress%)

## 2\. Head Click Interactions

### 2.1 Messages

*   ✅ **headClick.messages** - Messages send correctly on head click
    *   Messages display with correct formatting
    *   Placeholders work (%player%, %prefix%, %current%, %max%, %progress%, %headName%)
    *   PlaceholderAPI placeholders work
    *   Multiple messages display correctly

### 2.2 Title

*   ✅ **headClick.title** - Title displays correctly
    *   Title enabled/disabled works
    *   firstLine displays correctly
    *   subTitle displays correctly
    *   Fade in/stay/fade out timings work
    *   Placeholders work in title

### 2.3 Firework

*   ✅ **headClick.firework** - Firework launches correctly
    *   Firework enabled/disabled works
    *   Custom colors work
    *   Random colors work (empty colors array)
    *   fadeColors work
    *   flicker setting works
    *   power setting works

### 2.4 Particles

*   ✅ **headClick.particles** - Particles display correctly
    *   Particles enabled/disabled works
    *   Particle type displays correctly (VILLAGER\_ANGRY)
    *   Particles show for already found heads
    *   Colors work for REDSTONE type

### 2.5 Sounds

*   ✅ **headClick.sounds** - Sounds play correctly
    *   alreadyOwn sound plays (block\_note\_block\_didgeridoo)
    *   notOwn sound plays (block\_note\_block\_bell)
    *   Sounds play at correct volume/location

### 2.6 Commands

*   ✅ **headClick.commands** - Commands execute correctly
    *   Commands execute on head click
    *   Placeholders work in commands (%player%)
    *   Multiple commands execute
    *   Commands execute in correct order

### 2.7 Randomize Commands

*   ✅ **headClick.randomizeCommands** - Command randomization works
    *   When false: commands execute in order
    *   When true: commands execute randomly
    *   All commands still execute (no duplicates)

### 2.8 Slots Required

*   ⏸️ **headClick.slotsRequired** - Inventory space check works
    *   When -1: no check performed
    *   When set: commands only execute if enough inventory space
    *   Correct number of slots checked

### 2.9 Push Back

*   ✅ **headClick.pushBack** - Push back works correctly
    *   Push back enabled/disabled works
    *   Power setting works correctly
    *   Only pushes when head is already found
    *   Push direction is correct

## 3\. Visual Features

### 3.1 Holograms

*   ✅ **holograms** - Holograms display correctly
    *   DEFAULT plugin mode works (TextDisplay)
    *   ADVANCED plugin mode works (requires PacketEvents)
    *   Height above head is correct (0.4)
    *   Found hologram displays correctly
    *   Not found hologram displays correctly
    *   Advanced placeholders work (%state%, %current%, %max%)
    *   Holograms update when head is found
    *   Multiple lines display correctly

### 3.2 Floating Particles

*   ✅ **floatingParticles** - Floating particles display correctly
    *   Not found particles enabled/disabled works
    *   Found particles enabled/disabled works
    *   Particle type displays correctly (REDSTONE)
    *   Colors work correctly (multiple colors)
    *   Amount setting works
    *   Particles float above head correctly

### 3.3 Spin

*   ⏸️ **spin** - Head spinning works correctly
    *   Spin enabled/disabled works
    *   Speed setting works (20)
    *   Linked mode works (all heads spin identically)
    *   Unlinked mode works (heads spin independently)
    *   Spinning doesn't cause performance issues
    *   Spinning continues after server restart

## 4\. System Features

### 4.1 Hint System

*   ⏸️ **hint** - Hint system works correctly
    *   Distance setting works (16 blocks)
    *   Frequency setting works (20)
    *   Sound plays correctly (BLOCK\_AMETHYST\_BLOCK\_CHIME)
    *   Action bar message displays correctly
    *   Placeholders work (%prefix%, %arrow%, %position%, %distance%)
    *   Hint only shows when within distance

### 4.2 Internal Task

*   ⏸️ **internalTask** - Internal tasks work correctly
    *   Delay setting works (20)
    *   Hologram updates work correctly
    *   Particle updates work correctly
    *   Player view distance works (16 blocks)
    *   Performance is acceptable

### 4.3 Should Reset Player Data

*   ✅ **shouldResetPlayerData** - Data reset works correctly
    *   When true: player data deleted when head destroyed
    *   When false: player data remains when head destroyed
    *   Data reset doesn't affect other heads

### 4.4 Hide Found Heads

*   ⏸️ **hideFoundHeads** - Head hiding works correctly
    *   Feature enabled/disabled works
    *   Found heads are hidden visually
    *   Collision box still exists (forcefield effect)
    *   Requires PacketEvents (if enabled)
    *   Requires server restart to apply
    *   Works correctly on Folia

## 5\. GUI Features

### 5.1 GUI Configuration

*   ✅ **gui** - GUI displays and works correctly
    *   Border icon displays (GRAY\_STAINED\_GLASS\_PANE)
    *   Previous icon displays (ARROW)
    *   Next icon displays (ARROW)
    *   Back icon displays (SPRUCE\_DOOR)
    *   Close icon displays (BARRIER)
    *   All GUI interactions work
    *   GUI opens/closes correctly
    *   Navigation works correctly

## 6\. Additional Tests

### 6.1 General Functionality

*   ✅ Plugin loads without errors on Folia
*   ✅ Plugin enables successfully
*   ✅ Plugin disables cleanly
*   ✅ No console errors during normal operation
*   ✅ No performance issues with multiple heads
*   ✅ Commands work correctly
*   ✅ Permissions work correctly

### 6.2 Edge Cases

*   ⏸️ Multiple players interacting with same head
*   ⏸️ Head in unloaded chunk
*   ⏸️ Server restart preserves data
*   ⏸️ Chunk load/unload doesn't break heads
*   ⏸️ World change doesn't break heads

## Test Results Summary

**Total Features**: 19 main categories  
**Passed**: 15  
**Failed**: 0  
**Skipped**: 4  
**Partial**: 0

### Passed Features:

*   ✅ 1.1 Heads Theme
*   ✅ 1.2 Progress Bar
*   ✅ 2.1 Messages
*   ✅ 2.2 Title
*   ✅ 2.3 Firework
*   ✅ 2.4 Particles
*   ✅ 2.5 Sounds
*   ✅ 2.6 Commands
*   ✅ 2.7 Randomize Commands
*   ✅ 2.9 Push Back
*   ✅ 3.1 Holograms
*   ✅ 3.2 Floating Particles
*   ✅ 4.3 Should Reset Player Data
*   ✅ 5.1 GUI Configuration
*   ✅ 6.1 General Functionality

### Remaining Tests:

*   ⏸️ 2.8 Slots Required
*   ⏸️ 3.3 Spin
*   ⏸️ 4.1 Hint System
*   ⏸️ 4.2 Internal Task
*   ⏸️ 4.4 Hide Found Heads
*   ⏸️ 6.2 Edge Cases

## Notes

*   Test on Folia server first
*   Report any errors immediately
*   Test features in batches for efficiency
*   Update status as you test each feature

## Error Log

_Add any errors encountered during testing here:_

```plaintext
[No errors yet]
```