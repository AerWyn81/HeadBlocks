# Translations

All plugin messages (except console messages) can be translated.

To add a translation, duplicate the `messages_en.yml` file in the language folder and rename it, keeping the `_xx.yml`
format.
Make your translations and then in the [config.yml](config.md) file, set the `xx` code in the language section.

### Colors

HeadBlocks supports Minecraft legacy colors, hexadecimal colors, and gradients thanks
to [IridiumColorAPI](https://github.com/Iridium-Development/IridiumColorAPI).

**How to use:**

Minecraft native color:
`&6&lH&e&lead&6&lB&e&llocks`

Hexadecimal color:
`<SOLID:FFFF00>HeadBlocks`

Gradient:
`<GRADIENT:ff0000>HeadBlocks</GRADIENT:ffff00>`

Rainbow (bonus):
`<RAINBOW1>HeadBlocks</RAINBOW>`

Gradient saturation:
`<RAINBOW100>HeadBlocks</RAINBOW>`

---

```
Prefix: '&6&lH&e&lead&6&lB&e&llocks &7»&r'

Head:
  Name: "&6&lH&e&lead&6&lB&e&llocks"
  Lore:
    - ""
    - "&7&oCreates a HeadBlock at the chosen location"
    - ""
    - "&2✔ &8- &7Right click + sneak to create"
    - "&c✘ &8- &7Left click + sneak to delete"
    - ""

Messages:
  NoPermission: "%prefix% &cYou do not have permission to run this command."
  NoPermissionBlock: "%prefix% &cYou do not have permission to interact with this head."
  ErrorCommand: "%prefix% &cError in the command. See usage in /hb help."
  PlayerOnly: "%prefix% &cThis command can only be executed by a player."
  PlayerNotFound: "%prefix% &cThe player %player% does not exist."
  PlayerNotConnected: "%prefix% &cThe player %player% is not connected."
  HeadNotYetLoaded: "%prefix% &cThe head id %id% cannot be given until HeadDatabase is fully loaded."
  StorageError: "%prefix% &cAn error occurred with the storage system. Please check your console logs."
  ExportSuccess: "%prefix% &aThe SQL database export file &7(%fileName%) &ahas been successfully generated in the plugin folder."
  ExportError: "%prefix% &cAn error occurred while creating the sql file: "
  ExportInProgress: "%prefix% &aGeneration of the export of the current SQL script..."
  ReloadComplete: "%prefix% &aThe configuration has been successfully reloaded."
  ReloadWithErrors: "%prefix% &cErrors found when reloading, check your console logs."
  InventoryFull: "%prefix% &cThere is not enough space in the player's inventory to give a HeadBlock."
  TopEmpty: "%prefix% &cPlayer top is empty."
  CreativeSneakRemoveHead: "%prefix% &cYou must be in creative and crouching to destroy a HeadBlock."
  CreativeSneakAddHead: "%prefix% &cYou must be in creative and crouching to place a HeadBlock."
  HeadPlaced: "%prefix% &2&l+ &aHeadBlock placed &7(%world%, %x%, %y%, %z%)&a."
  HeadRemoved: "%prefix% &2&c- &eHeadBlock removed &7(%world%, %x%, %y%, %z%)&e."
  RemoveAllConfirm: "%prefix% &cWarning: This command will remove &7%headCount% &chead(s). Add &7--confirm &cto the command to confirm."
  RemoveAllInProgress: "%prefix% &7&oRemoving &e&o%headCount% &7&ohead(s), please wait..."
  RemoveAllSuccess: "%prefix% &e%headCount% &ahead(s) successfully removed."
  RemoveAllError: "%prefix% &cNo heads have been removed."
  AlreadyClaimHead: "%prefix% &cYou have already clicked on this head."
  ListHeadEmpty: "%prefix% &cThere are no heads yet."
  HeadGiven: "%prefix% &aYou received a HeadBlock."
  NoHeadFound: "%prefix% &cThis player has not yet found a head."
  ResetAllNoData: "%prefix% &cThere is no data to reset."
  ResetAllConfirm: "%prefix% &cWarning: This command will reset &7%playerCount% &cplayer(s) data. Add &7--confirm &cto the command to confirm."
  ResetAllSuccess: "%prefix% &aData of &e%playerCount% &aplayer(s) successfully reset."
  PlayerReset: "%prefix% &aThe %player% data has been reset."
  PlayerHeadReset: "%prefix% &aThe head &e%headName% &afor &e%player% &ahas been reset."
  HeadNameNotFound: "%prefix% &cNo head named &7%headName% &cwas found."
  ResetAllHeadConfirm: "%prefix% &cWarning: This command will reset the head &7%headName% &cfor &7%playerCount% &cplayer(s). Add &7--confirm &cto the command to confirm."
  ResetAllHeadSuccess: "%prefix% &aThe head &e%headName% &ahas been reset for &e%playerCount% &aplayer(s)."
  RemoveLocationError: "%prefix% &cError: No matching HeadBlock was found."
  HeadAlreadyExistHere: "%prefix% &cA HeadBlock already exists at this location."
  ErrorCannotPlaySound: "%prefix% &cSound on head click cannot be played."
  NoTargetHeadBlock: "%prefix% &cNo HeadBlock was found at the targeted location."
  TargetBlockInfo: "%prefix% &aYou have selected a HeadBlock &7(uuid: %uuid% - %world%, %x%, %y%, %z%)&a. Target the block on which you want to move the HeadBlock and use the command &e/hb move --confirm &ato confirm. Use &e/hb move --cancel &ato cancel."
  TargetBlockInvalid: "%prefix% &cThe block or the block above the targeted block is invalid."
  HeadMoveNoPlayer: "%prefix% &cNo HeadBlock to move was found."
  HeadMoveOtherLoc: "%prefix% &cYou cannot move the HeadBlock to the same place as before."
  HeadMoveAlready: "%prefix% &cYou already have a HeadBlock move in progress, use the command &e/hb move --cancel &cfirst to cancel."
  HeadMoveCancel: "%prefix% &cYou have cancelled the move of the HeadBlock."
  TargetBlockMoved: "%prefix% &aThe HeadBlock has been successfully moved to &e%world%&a, &e%x%&a, &e%y%&a, &e%z%&a."
  PluginReloading: "%prefix% &cPlease wait, the plugin is reloading..."
  TargetBlockNotHead: "%prefix% &cTargeted block is not a HeadBlock."
  OrderClickError: "%prefix% &cAnother HeadBlock needs to be found before this one &7(%name%&7)&c."
  HitClickMax: "%prefix% &cThis HeadBlock has reached its click limit &7(%count%)&c."
  InventoryFullReward: "%prefix% &cYou do not have enough space in your inventory to receive the reward."
  NameCannotBeEmpty: "%prefix% &cHead name cannot be empty."
  HeadRenamed: "%prefix% &aSuccessfully renamed head to &e%name%&a."
  EnterRewardCommand: "%prefix% &aPlease enter the reward command in chat &7(without /, type 'cancel' to abort)&a:"
  EnterRewardMessage: "%prefix% &aPlease enter the reward message in chat &7(type 'cancel' to abort)&a:"
  EnterRewardBroadcast: "%prefix% &aPlease enter the broadcast message in chat &7(type 'cancel' to abort)&a:"
  RewardAdded: "%prefix% &aReward successfully added!"
  RewardUpdated: "%prefix% &aReward successfully updated!"
  ResyncUsage: "%prefix% &cUsage: /hb debug resync <database|locations> [--force]"
  ResyncUnknownType: "%prefix% &cUnknown resync type! Use: database or locations."
  ResyncMySQLRequiresForce: "%prefix% &cMySQL detected. Please backup your database manually, then use &7--force &cto proceed."
  ResyncMultiServerDetected: "%prefix% &cMulti-server setup detected!"
  ResyncMultiServerCount: "%prefix% &cFound &e%count% &cdifferent server IDs: &e%serverIds%&c."
  ResyncMultiServerWarningDb: "%prefix% &cThis operation would delete heads from other servers."
  ResyncMultiServerWarningLoc: "%prefix% &cCannot restore locations from a shared database safely."
  ResyncOperationCancelled: "%prefix% &cOperation cancelled for safety. Use &7--force &cto override."
  ResyncCurrentServerId: "%prefix% &7Current server ID: &e%serverId%&7."
  ResyncDatabaseAlreadyInSync: "%prefix% &aDatabase is already in sync with locations.yml!"
  ResyncDatabaseFoundHeads: "%prefix% &7Found &e%count% &7head(s) in database not present in locations.yml."
  ResyncDatabaseBackupSuccess: "%prefix% &7Database backup created: &e%fileName%&7."
  ResyncDatabaseBackupError: "%prefix% &cFailed to backup database, see console for details."
  ResyncDatabaseSuccess: "%prefix% &aSuccessfully removed &e%count% &ahead(s) from database!"
  ResyncLocationsStarting: "%prefix% &7Processing &e%count% &7head location(s)..."
  ResyncLocationsSuccess: "%prefix% &aResync complete! Restored: &e%restored%&a, Texture applied: &e%textureApplied%&a, Skipped: &e%skipped%&a, Failed: &e%failed%"
  ResyncError: "%prefix% &cError during resync: &e%error%"
  HuntUsage: "%prefix% &cUsage: /hb hunt <create|delete|enable|disable|list|info> [name]"
  HuntCreated: "%prefix% &aHunt &e%hunt% &ahas been created successfully."
  HuntAlreadyExists: "%prefix% &cA hunt named &e%hunt% &calready exists."
  HuntInvalidName: "%prefix% &cInvalid hunt name. Only alphanumeric characters and hyphens are allowed."
  HuntNotFound: "%prefix% &cHunt &e%hunt% &cnot found. Use &7/hb hunt list &cto see available hunts."
  HuntDeleteConfirm: "%prefix% &cWarning: This will delete hunt &7%hunt% &cand its &7%headCount% &chead(s). Add &7--confirm &cto confirm."
  HuntDeleted: "%prefix% &aHunt &e%hunt% &ahas been deleted successfully."
  HuntCannotDeleteDefault: "%prefix% &cThe default hunt cannot be deleted."
  HuntEnabled: "%prefix% &aHunt &e%hunt% &ahas been enabled."
  HuntDisabled: "%prefix% &eHunt &6%hunt% &ehas been disabled."
  HuntAlreadyActive: "%prefix% &cHunt &e%hunt% &cis already active."
  HuntAlreadyInactive: "%prefix% &cHunt &e%hunt% &cis already inactive."
  HuntListHeader: "%prefix% &7-----[ &6Hunts &7(&e%count%&7) ]-----"
  HuntListEntry: " &8- &e%hunt% &7(%displayName%&7) &8| %state% &8| &7Heads: &e%headCount%"
  HuntListEmpty: "%prefix% &cNo hunts found."
  HuntInfoHeader: "%prefix% &7-----[ &6Hunt: &e%hunt% &7]-----"
  HuntInfoName: " &aDisplay name: &e%displayName%"
  HuntInfoState: " &aState: %state%"
  HuntInfoPriority: " &aPriority: &e%priority%"
  HuntInfoHeads: " &aHeads: &e%headCount%"
  HuntInfoBehaviors: " &aBehaviors: &e%behaviors%"
  HuntSelected: "%prefix% &aActive hunt set to &e%hunt%&a."
  HuntSelectReset: "%prefix% &aActive hunt reset to &edefault&a."
  HuntActiveSelection: "%prefix% &7Your active hunt is: &e%hunt%"
  HuntHeadTransferred: "%prefix% &aHead &e%head% &atransferred to hunt &e%hunt%&a."
  HuntHeadNotFound: "%prefix% &cHead with UUID &e%uuid% &cnot found."
  HuntAssignNoHeads: "%prefix% &cNo heads found matching the criteria."
  HuntAssignSuccess: "%prefix% &aSuccessfully assigned &e%count% &ahead(s) to hunt &e%hunt%&a."
  HuntProgressHeader: "%prefix% &7-----[ &6Progress for &e%player% &7]-----"
  HuntProgressEntry: " &8- &e%displayName% &7(%state%&7) &8| &e%current%&7/&e%max% &7[%progress%&7]"
  HuntProgressDetail: "%prefix% &aPlayer: &e%player% &8| &aHunt: &e%displayName% &8| &e%current%&7/&e%max% &7[%progress%&7]"
  HuntTopHeader: "%prefix% &7-----[ &6Top &8- &e%displayName% &7]-----"
  HuntPlayerReset: "%prefix% &aPlayer &e%player% &aprogress in hunt &e%hunt% &ahas been reset."
  HuntResetRequireHunt: "%prefix% &cMultiple hunts detected. Use &7/hb hunt <name> reset <player> &cto reset a specific hunt."
  HuntHeadInactive: "%prefix% &cThis head is part of an inactive hunt."
  HuntInfoPlayers: " &aPlayers with progress: &e%playerCount%"
  ProgressCommand:
    - ""
    - "&aPlayer: &e&l%player%"
    - "&aNumber of heads collected: &e%current%&7/&e%max%"
    - "&aProgression: &7[%progress%&7]"
    - ""

Chat:
  Hover:
    Teleport: "&7&oTeleport to the HeadBlock"
    BlockedTeleport: "&7&oTeleportation impossible"
    Remove: "&7&oDelete this HeadBlock"
    PreviousPage: "&7&oPrevious page"
    NextPage: "&7&oNext page"
    Own: "&7&oHead found"
    NotOwn: "&7&oHead not found yet"
    LineTop: "&7&oShow details for player"
    HeadIsNotOnThisServer: "&7&oHead is not on this server"
  Box:
    Teleport: "&8[&a☄&8] &7|"
    Remove: "&7| &8[&c✘&8]"
    Own: "&7| &8[&2✔&8]"
    NotOwn: "&7| &8[&c✘&8]"
  StatsTitleLine: "&7---------[ &eData of &6%player% &8» &6%headCount%&8/&6%max% &eheads &7]---------"
  LineTitle: "&7----------------[ &6&lH&e&lead&6&lB&e&llocks&7 ]---------------"
  TopTitle: "&7---------[ &6&lH&e&lead&6&lB&e&llocks&7 &7- &6&6&lL&eeaderboard &7]---------"
  LineTop: "&6%pos% &7- &e%player% &8(&7%count%&8)"
  LineCoordinate: "&6World&8: &e%world% &8[&6X&8: &e%x%&8, &6Y&8: &e%y%&8, &6Z&8: &e%z%&8]"
  LineWorldNotFound: "&cWorld &7%world% &cnot found"
  PreviousPage: "&7--------[&8<<<&7]-------"
  NextPage: "&7-------[&8>>>&7]--------"
  PageFooter: " &8[ &e%pageNumber%&7/&e%totalPage% &8] "
  Info:
    Name: "&aName: &7"
    HoverCopyName: "&7&oClick to copy name"
    Uuid: "&aUUID: &7"
    HoverCopyUuid: "&7&oClick to copy UUID"
    Location: "&aLocation: &7"
    HoverLocationTp: "&7&oClick to teleport"
    Loaded: "&aLoaded: &7"
    HitCount: "&aHit count: "
    OrderIndex: "&aOrder index: "
    HintSound: "&aHint sound: &7"
    HintActionBar: "&aHint action bar: &7"
    HintsTitle: "&e&lHints:"
    RewardsTitle: "&e&lRewards: &7"
    HoverForMore: "&8&oHover for details"

Hunt:
  State:
    Active: "&aActive"
    Inactive: "&cInactive"
    Archived: "&8Archived"
  Behavior:
    Ordered: "&6Ordered"
    ScheduledNotStarted: "%prefix% &cThis hunt has not started yet."
    ScheduledEnded: "%prefix% &cThis hunt has ended."

Other:
  NameNotSet: "&cName not set"
  Sound: "Sound"
  ActionBar: "ActionBar"

Gui:
  TitleOptions: "&8HeadBlocks &7- &c&lOptions"
  TitleOrder: "&8HeadBlocks &7- &6&lOrder"
  TitleClickCounter: "&8HeadBlocks &7- &2&lClick counter"
  TitleHint: "&8HeadBlocks &7- &b&lHint"
  PreviousPage: "&cPrevious page"
  NextPage: "&aNext page"
  Close: "&cClose"
  Unnamed: "&7&oUnnamed"
  Infinite: "&7&oInfinite"
  NoOrder: "&7&oNo order"
  Enabled: "&aEnabled"
  Disabled: "&cDisabled"
  CloseLore:
    - "&7Close this interface"
  Back: "&cBack"
  BackLore:
    - "&7Back to previous interface"
  Previous: "&cPrevious page"
  PreviousLore:
    - "&7Back to page %page%"
  Next: "&aNext page"
  NextLore:
    - "&7Next to page %page%"
  NoHeads: "&cNo heads yet"
  OrderName: "&7Set up &8- &6&lOrder"
  OrderLore:
    - ""
    - "&e&lINFO:"
    - "&7 This interface allows you to configure"
    - "&7 the click order on the heads"
  ClickCounterName: "&7Set up &8- &2&lClick counter"
  ClickCounterLore:
    - ""
    - "&e&lINFO:"
    - "&7 This interface allows you to configure"
    - "&7 the click counter on each head"
  OrderItemName: "&6%headName% &7(%world%, %x%, %y%, %z%)"
  OrderItemLore:
    - ""
    - "&e&lINFO:"
    - "&7 Define the click order of this head"
    - ""
    - "&c&lLEFT CLICK&8: &7Decrease order position"
    - "&a&lRIGHT CLICK&8: &7Increase order position"
    - ""
    - "&7Current position: &e%position%"
  CounterClickItemName: "&6%headName% &7(%world%, %x%, %y%, %z%)"
  CounterClickItemLore:
    - ""
    - "&e&lINFO:"
    - "&7 Define the click count of this head"
    - ""
    - "&c&lLEFT CLICK&8: &7Decrease count"
    - "&a&lRIGHT CLICK&8: &7Increase count"
    - ""
    - "&7Count: &e%count%"
  RewardsName: "&7Set up &8- &b&lRewards"
  RewardsLore:
    - ""
    - "&e&lINFO:"
    - "&7 This interface allows you to configure"
    - "&7 the rewards for each head"
  TitleRewardsSelection: "&8HeadBlocks &7- &b&lRewards"
  RewardsSelectionItemName: "&6%headName% &7(%world%, %x%, %y%, %z%)"
  RewardsSelectionItemLore:
    - ""
    - "&7Rewards configured: &e%count%"
    - ""
    - "&a&lCLICK&8: &7Manage rewards"
  TitleRewards: "&8Rewards &7- &6%headName%"
  RewardItemName: "&bReward #%index%"
  RewardType: "&7Type: &e%type%"
  RewardCommand: "&7Command: &e%command%"
  RewardMessage: "&7Message: %message%"
  RewardItemLore:
    - ""
    - "&c&lLEFT CLICK&8: &7Edit reward"
    - "&4&lSHIFT+LEFT CLICK&8: &7Delete reward"
  AddRewardName: "&a&lAdd new reward"
  AddRewardLore:
    - ""
    - "&a&lCLICK&8: &7Add a new reward"
  TitleRewardTypeSelection: "&8Select reward type"
  RewardTypeMessage: "&a&lMessage"
  RewardTypeMessageLore:
    - ""
    - "&7Send a personal message to the player"
    - "&7who finds this head"
    - ""
    - "&a&lCLICK&8: &7Select this type"
  RewardTypeCommand: "&6&lCommand"
  RewardTypeCommandLore:
    - ""
    - "&7Execute a command as console"
    - "&7when a player finds this head"
    - ""
    - "&a&lCLICK&8: &7Select this type"
  RewardTypeBroadcast: "&b&lBroadcast"
  RewardTypeBroadcastLore:
    - ""
    - "&7Send a message to all online players"
    - "&7when a player finds this head"
    - ""
    - "&a&lCLICK&8: &7Select this type"
  HintName: "&7Set up &8- &b&lHint"
  HintLore:
    - ""
    - "&e&lINFO:"
    - "&7 This interface allows you to configure"
    - "&7 hint sound activation on heads"
  HintItemName: "&6%headName% &7(%world%, %x%, %y%, %z%)"
  HintItemLore:
    - ""
    - "&e&lINFO:"
    - "&7 Define hint options of this head"
    - ""
    - "&b&lMIDDLE CLICK&8: &7Change hint mode"
    - "&7 Current mode: &e%mode%"
    - ""
    - "&c&lLEFT CLICK&8: &7Enable hint"
    - "&a&lRIGHT CLICK&8: &7Disable hint"
    - "&7&o+ SHIFT to apply on all heads"
    - ""
    - "&7State: &e%state%"

Help:
  LineTop: "&7--------------------[ &e&lHelp &7]--------------------"
  Help: "&6/hb help&8: &7&oDisplay this message"
  Progress: "&6/hb progress&8: &7&oDisplays your progress"
  Remove: "&6/hb remove (headUuid)&8: &7&oRemove head by its <headUuid> or targeted head"
  RemoveAll: "&6/hb removeall&8: &7&oRemove all spawned heads"
  Give: "&6/hb give <player> <*|number>&8: &7&oGives a HeadBlock to <player>. <number> when multi HeadBlocks are defined"
  Reset: "&6/hb reset <player> [--head <name|uuid>]&8: &7&oReset data of the <player>. Use --head to reset a specific head (empty for targeted head)"
  ResetAll: "&6/hb resetall [--head <name|uuid>] [--confirm]&8: &7&oReset data of all players. Use --head to reset a specific head for all players"
  List: "&6/hb list <page>&8: &7&oDisplays the list of existing HeadBlocks"
  Stats: "&6/hb stats <player> <page>&8: &7&oDisplays the list of found and missing heads for <player>"
  Reload: "&6/hb reload&8: &7&oReload the configuration"
  Version: "&6/hb version&8: &7&oDisplays the version of the plugin"
  Top: "&6/hb top <number>&8: &7&oDisplays the top leaderboard of <number> first players"
  Move: "&6/hb move&8: &7&oMove the HeadBlock to another location"
  Export: "&6/hb export database&8: &7&oExport the database to an SQL file with player data"
  Rename: "&6/hb rename <name>&8: &7&oRename target head"
  Hunt: "&6/hb hunt <create|delete|enable|disable|list|info>&8: &7&oManage hunts"
```