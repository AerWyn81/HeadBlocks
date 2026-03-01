package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HuntCreateEvent;
import fr.aerwyn81.headblocks.api.events.HuntDeleteEvent;
import fr.aerwyn81.headblocks.api.events.HuntStateChangeEvent;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@HBAnnotations(command = "hunt", permission = "headblocks.admin")
public class Hunt implements Cmd {
    private final ServiceRegistry registry;

    public Hunt(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "enable" -> handleEnable(sender, args);
            case "disable" -> handleDisable(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "select" -> handleSelect(sender, args);
            case "active" -> handleActive(sender);
            case "set" -> handleSet(sender, args);
            case "assign" -> handleAssign(sender, args);
            case "transfer" -> handleTransfer(sender, args);
            case "progress" -> handleProgress(sender, args);
            case "top" -> handleTop(sender, args);
            case "reset" -> handleReset(sender, args);
            default -> sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
        }

        return true;
    }

    // --- E2: CRUD ---

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        String name = args[2];

        if (!name.matches("[a-zA-Z0-9-]+")) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntInvalidName"));
            return;
        }

        if (registry.getHuntService().getHuntNames().stream().anyMatch(n -> n.equalsIgnoreCase(name))) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntAlreadyExists")
                    .replaceAll("%hunt%", name));
            return;
        }

        // If the sender is a player, open the behavior selection GUI
        if (sender instanceof Player player) {
            registry.getGuiService().getBehaviorSelectionManager().open(player, name);
            return;
        }

        // Console: create directly with FreeBehavior
        String huntId = name.toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = new fr.aerwyn81.headblocks.data.hunt.Hunt(registry.getConfigService(), huntId, name, HuntState.ACTIVE, 1, "PLAYER_HEAD");

        HuntCreateEvent createEvent = new HuntCreateEvent(hunt);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (createEvent.isCancelled()) {
            return;
        }

        registry.getHuntConfigService().saveHunt(hunt);

        try {
            registry.getStorageService().createHuntInDb(hunt.getId(), hunt.getDisplayName(), hunt.getState().name());
        } catch (Exception e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            return;
        }

        registry.getHuntService().registerHunt(hunt);
        registry.getStorageService().incrementHuntVersion();

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntCreated")
                .replaceAll("%hunt%", hunt.getId()));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();

        if ("default".equals(huntId)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntCannotDeleteDefault"));
            return;
        }

        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = registry.getHuntService().getHuntById(huntId);
        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        // Parse flags from args[3..n] (order-independent)
        boolean hasConfirm = false;
        boolean keepHeads = false;
        String fallbackHuntId = null;

        for (int i = 3; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            switch (arg) {
                case "--confirm" -> hasConfirm = true;
                case "--keepheads" -> keepHeads = true;
                case "--fallback" -> {
                    if (i + 1 < args.length) {
                        fallbackHuntId = args[++i].toLowerCase();
                    }
                }
            }
        }

        // Validate: --fallback requires --keepHeads
        if (fallbackHuntId != null && !keepHeads) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleteFallbackRequiresKeepHeads"));
            return;
        }

        // Resolve fallback hunt
        String resolvedFallback = keepHeads ? (fallbackHuntId != null ? fallbackHuntId : "default") : null;

        if (keepHeads && fallbackHuntId != null) {
            fr.aerwyn81.headblocks.data.hunt.Hunt fb = registry.getHuntService().getHuntById(fallbackHuntId);
            if (fb == null) {
                sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleteFallbackNotFound")
                        .replaceAll("%hunt%", fallbackHuntId));
                return;
            }
        }

        // Show confirmation message if --confirm not provided
        if (!hasConfirm) {
            if (keepHeads) {
                sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleteKeepHeadsConfirm")
                        .replaceAll("%hunt%", huntId)
                        .replaceAll("%headCount%", String.valueOf(hunt.getHeadCount()))
                        .replaceAll("%fallback%", resolvedFallback));
            } else {
                sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleteConfirm")
                        .replaceAll("%hunt%", huntId)
                        .replaceAll("%headCount%", String.valueOf(hunt.getHeadCount())));
            }
            return;
        }

        HuntDeleteEvent deleteEvent = new HuntDeleteEvent(huntId);
        Bukkit.getPluginManager().callEvent(deleteEvent);
        if (deleteEvent.isCancelled()) {
            return;
        }

        if (keepHeads) {
            // Mode B: keep heads, transfer to fallback
            handleDeleteKeepHeads(sender, hunt, huntId, resolvedFallback);
        } else {
            // Mode A: delete hunt + heads physically
            handleDeleteWithHeads(sender, hunt, huntId);
        }
    }

    private void handleDeleteWithHeads(CommandSender sender, fr.aerwyn81.headblocks.data.hunt.Hunt hunt, String huntId) {
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleteInProgress")
                .replaceAll("%hunt%", huntId));

        // Collect HeadLocation objects for this hunt
        var headsToRemove = new ArrayList<HeadLocation>();
        for (UUID headUUID : hunt.getHeadUUIDs()) {
            HeadLocation hl = registry.getHeadService().getHeadByUUID(headUUID);
            if (hl != null) {
                headsToRemove.add(hl);
            }
        }

        // Remove heads physically (world + DB + locations.yml) async
        registry.getHeadService().removeAllHeadLocationsAsync(headsToRemove, true, (headRemoved) -> {
            try {
                registry.getStorageService().unlinkAllHeadsFromHuntInDb(huntId);
                registry.getStorageService().deletePlayerProgressForHunt(huntId);
                registry.getStorageService().deleteHuntFromDb(huntId);
            } catch (Exception e) {
                sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
                LogUtil.error("Error during hunt delete cleanup: {0}", e.getMessage());
                return;
            }

            registry.getHuntConfigService().deleteHuntFile(huntId);
            registry.getHuntService().unregisterHunt(huntId);
            registry.getStorageService().incrementHuntVersion();

            sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleted")
                    .replaceAll("%hunt%", huntId));
        });
    }

    private void handleDeleteKeepHeads(CommandSender sender, fr.aerwyn81.headblocks.data.hunt.Hunt hunt, String huntId, String fallbackHuntId) {
        try {
            fr.aerwyn81.headblocks.data.hunt.Hunt fallbackHunt = registry.getHuntService().getHuntById(fallbackHuntId);

            // Reassign heads to fallback hunt
            for (UUID headUUID : hunt.getHeadUUIDs()) {
                if (fallbackHunt != null && !fallbackHunt.containsHead(headUUID)) {
                    fallbackHunt.addHead(headUUID);
                    registry.getStorageService().linkHeadToHunt(headUUID, fallbackHuntId);
                }
            }

            // Transfer player progress to fallback
            registry.getStorageService().transferPlayerProgress(huntId, fallbackHuntId);

            registry.getStorageService().unlinkAllHeadsFromHuntInDb(huntId);
            registry.getStorageService().deleteHuntFromDb(huntId);
        } catch (Exception e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            LogUtil.error("Error during hunt delete (keepHeads): {0}", e.getMessage());
            return;
        }

        registry.getHuntConfigService().deleteHuntFile(huntId);
        registry.getHuntService().unregisterHunt(huntId);
        registry.getStorageService().incrementHuntVersion();

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleted")
                .replaceAll("%hunt%", huntId));
    }

    private void handleEnable(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        if (hunt.isActive()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntAlreadyActive")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        HuntStateChangeEvent stateEvent = new HuntStateChangeEvent(hunt, hunt.getState(), HuntState.ACTIVE);
        Bukkit.getPluginManager().callEvent(stateEvent);
        if (stateEvent.isCancelled()) {
            return;
        }

        hunt.setState(HuntState.ACTIVE);
        registry.getHuntConfigService().saveHunt(hunt);

        try {
            registry.getStorageService().updateHuntStateInDb(huntId, HuntState.ACTIVE.name());
        } catch (Exception e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            return;
        }

        registry.getStorageService().incrementHuntVersion();

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntEnabled")
                .replaceAll("%hunt%", huntId));
    }

    private void handleDisable(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        if (!hunt.isActive()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntAlreadyInactive")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        HuntStateChangeEvent stateEvent = new HuntStateChangeEvent(hunt, hunt.getState(), HuntState.INACTIVE);
        Bukkit.getPluginManager().callEvent(stateEvent);
        if (stateEvent.isCancelled()) {
            return;
        }

        hunt.setState(HuntState.INACTIVE);
        registry.getHuntConfigService().saveHunt(hunt);

        try {
            registry.getStorageService().updateHuntStateInDb(huntId, HuntState.INACTIVE.name());
        } catch (Exception e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            return;
        }

        registry.getStorageService().incrementHuntVersion();

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntDisabled")
                .replaceAll("%hunt%", huntId));
    }

    private void handleList(CommandSender sender) {
        var hunts = registry.getHuntService().getAllHunts();

        if (hunts.isEmpty()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntListEmpty"));
            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntListHeader")
                .replaceAll("%count%", String.valueOf(hunts.size())));

        for (fr.aerwyn81.headblocks.data.hunt.Hunt hunt : hunts) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntListEntry")
                    .replaceAll("%hunt%", hunt.getId())
                    .replaceAll("%displayName%", hunt.getDisplayName())
                    .replaceAll("%state%", hunt.getState().getLocalizedName(registry.getLanguageService()))
                    .replaceAll("%headCount%", String.valueOf(hunt.getHeadCount())));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoHeader")
                .replaceAll("%hunt%", hunt.getId()));
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoName")
                .replaceAll("%displayName%", hunt.getDisplayName()));
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoState")
                .replaceAll("%state%", hunt.getState().getLocalizedName(registry.getLanguageService())));
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoPriority")
                .replaceAll("%priority%", String.valueOf(hunt.getPriority())));
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoHeads")
                .replaceAll("%headCount%", String.valueOf(hunt.getHeadCount())));
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoBehaviors")
                .replaceAll("%behaviors%", hunt.getBehaviors().stream()
                        .map(Behavior::getId).collect(Collectors.joining(", "))));

        try {
            int playerCount = registry.getStorageService().getTopPlayersForHunt(huntId).size();
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoPlayers")
                    .replaceAll("%playerCount%", String.valueOf(playerCount)));
        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoPlayers")
                    .replaceAll("%playerCount%", "?"));
        }
    }

    // --- E3: Head assignment ---

    private void handleSelect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.PlayerOnly"));
            return;
        }

        if (args.length < 3) {
            registry.getHuntService().clearSelectedHunt(player.getUniqueId());
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntSelectReset"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        registry.getHuntService().setSelectedHunt(player.getUniqueId(), huntId);
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntSelected")
                .replaceAll("%hunt%", huntId));
    }

    private void handleActive(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.PlayerOnly"));
            return;
        }

        String huntId = registry.getHuntService().getSelectedHunt(player.getUniqueId());
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntActiveSelection")
                .replaceAll("%hunt%", huntId));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.PlayerOnly"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();

        if (!registry.getHuntService().huntExists(huntId)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        HeadLocation headLocation = registry.getHeadService().getHeadAt(player.getTargetBlock(null, 100).getLocation());

        if (headLocation == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.NoTargetHeadBlock"));
            return;
        }

        try {
            registry.getHuntService().transferHead(headLocation.getUuid(), huntId);
        } catch (Exception e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            LogUtil.error("Error transferring head to hunt: {0}", e.getMessage());
            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntHeadTransferred")
                .replaceAll("%head%", headLocation.getNameOrUuid())
                .replaceAll("%hunt%", huntId));
    }

    private void handleAssign(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();

        if (!registry.getHuntService().huntExists(huntId)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        String mode = args[3].toLowerCase();
        java.util.List<HeadLocation> headsToAssign;

        switch (mode) {
            case "all" -> {
                fr.aerwyn81.headblocks.data.hunt.Hunt defaultHunt = registry.getHuntService().getDefaultHunt();
                if (defaultHunt == null) {
                    sender.sendMessage(registry.getLanguageService().message("Messages.HuntListEmpty"));
                    return;
                }
                headsToAssign = registry.getHeadService().getHeadLocations().stream()
                        .filter(h -> defaultHunt.containsHead(h.getUuid()))
                        .collect(Collectors.toList());
            }
            case "radius" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(registry.getLanguageService().message("Messages.PlayerOnly"));
                    return;
                }

                if (args.length < 5) {
                    sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
                    return;
                }

                int radius;
                try {
                    radius = Integer.parseInt(args[4]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
                    return;
                }

                var playerLoc = player.getLocation();
                headsToAssign = registry.getHeadService().getHeadLocations().stream()
                        .filter(h -> h.getLocation() != null
                                && h.getLocation().getWorld() != null
                                && h.getLocation().getWorld().equals(playerLoc.getWorld())
                                && h.getLocation().distance(playerLoc) <= radius)
                        .collect(Collectors.toList());
            }
            default -> {
                sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
                return;
            }
        }

        if (headsToAssign.isEmpty()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntAssignNoHeads"));
            return;
        }

        int count = 0;
        for (HeadLocation head : headsToAssign) {
            try {
                registry.getHuntService().transferHead(head.getUuid(), huntId);
                count++;
            } catch (Exception e) {
                LogUtil.error("Error assigning head {0} to hunt {1}: {2}", head.getUuid(), huntId, e.getMessage());
            }
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntAssignSuccess")
                .replaceAll("%count%", String.valueOf(count))
                .replaceAll("%hunt%", huntId));

        if (sender instanceof Player player) {
            registry.getHuntService().setSelectedHunt(player.getUniqueId(), huntId);
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntSelected")
                    .replaceAll("%hunt%", huntId));
        }
    }

    private void handleTransfer(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        UUID headUUID;
        try {
            headUUID = UUID.fromString(args[2]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntHeadNotFound")
                    .replaceAll("%uuid%", args[2]));
            return;
        }

        HeadLocation headLocation = registry.getHeadService().getHeadByUUID(headUUID);
        if (headLocation == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntHeadNotFound")
                    .replaceAll("%uuid%", args[2]));
            return;
        }

        String huntId = args[3].toLowerCase();

        if (!registry.getHuntService().huntExists(huntId)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        try {
            registry.getHuntService().transferHead(headUUID, huntId);
        } catch (Exception e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            LogUtil.error("Error transferring head to hunt: {0}", e.getMessage());
            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntHeadTransferred")
                .replaceAll("%head%", headLocation.getNameOrUuid())
                .replaceAll("%hunt%", huntId));
    }

    // --- E7: Per-hunt commands ---

    private void handleProgress(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        // Resolve target player (self or other)
        PlayerProfileLight profile;
        if (args.length >= 4) {
            try {
                profile = registry.getStorageService().getPlayerByName(args[3]);
            } catch (InternalException e) {
                sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
                return;
            }
            if (profile == null) {
                sender.sendMessage(registry.getLanguageService().message("Messages.PlayerNotFound", args[3]));
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(registry.getLanguageService().message("Messages.PlayerOnly"));
                return;
            }
            profile = new PlayerProfileLight(player.getUniqueId(), player.getName(), player.getDisplayName());
        }

        try {
            ArrayList<UUID> huntHeads = registry.getStorageService().getHeadsPlayerForHunt(profile.uuid(), huntId);
            int current = huntHeads.size();
            int total = hunt.getHeadCount();

            String progress = MessageUtils.createProgressBar(current, total,
                    registry.getConfigService().progressBarBars(),
                    registry.getConfigService().progressBarSymbol(),
                    registry.getConfigService().progressBarCompletedColor(),
                    registry.getConfigService().progressBarNotCompletedColor());

            sender.sendMessage(registry.getLanguageService().message("Messages.HuntProgressDetail")
                    .replaceAll("%player%", profile.name())
                    .replaceAll("%hunt%", hunt.getId())
                    .replaceAll("%displayName%", hunt.getDisplayName())
                    .replaceAll("%current%", String.valueOf(current))
                    .replaceAll("%max%", String.valueOf(total))
                    .replaceAll("%progress%", progress));
        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            LogUtil.error("Error retrieving hunt progress: {0}", e.getMessage());
        }
    }

    private void handleTop(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        int limit = 10;
        if (args.length >= 4) {
            try {
                limit = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            var topPlayers = new ArrayList<>(registry.getStorageService().getTopPlayersForHunt(huntId).entrySet());

            if (topPlayers.isEmpty()) {
                sender.sendMessage(registry.getLanguageService().message("Messages.TopEmpty"));
                return;
            }

            sender.sendMessage(registry.getLanguageService().message("Messages.HuntTopHeader")
                    .replaceAll("%hunt%", hunt.getId())
                    .replaceAll("%displayName%", hunt.getDisplayName()));

            int count = Math.min(limit, topPlayers.size());
            for (int i = 0; i < count; i++) {
                Map.Entry<PlayerProfileLight, Integer> entry = topPlayers.get(i);
                sender.sendMessage(MessageUtils.colorize(
                        registry.getLanguageService().message("Chat.LineTop", entry.getKey().name())
                                .replaceAll("%pos%", String.valueOf(i + 1))
                                .replaceAll("%count%", String.valueOf(entry.getValue()))));
            }
        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            LogUtil.error("Error retrieving hunt top players: {0}", e.getMessage());
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        String playerName = args[3];
        PlayerProfileLight profile;
        try {
            profile = registry.getStorageService().getPlayerByName(playerName);
        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            return;
        }

        if (profile == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.PlayerNotFound", playerName));
            return;
        }

        try {
            registry.getStorageService().resetPlayerHunt(profile.uuid(), huntId);
        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            LogUtil.error("Error resetting player {0} for hunt {1}: {2}", playerName, huntId, e.getMessage());
            return;
        }

        // Re-sync head visibility if PacketEvents active
        var targetPlayer = Bukkit.getPlayer(profile.uuid());
        if (targetPlayer != null) {
            var packetEventsHook = HeadBlocks.getInstance().getPacketEventsHook();
            if (packetEventsHook != null && packetEventsHook.isEnabled()
                    && packetEventsHook.getHeadHidingListener() != null) {
                packetEventsHook.getHeadHidingListener().showAllPreviousHeads(targetPlayer);
            }
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntPlayerReset")
                .replaceAll("%player%", playerName)
                .replaceAll("%hunt%", huntId));
    }

    // --- Tab completion ---

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Stream.of("create", "delete", "enable", "disable", "list", "info",
                            "select", "active", "set", "assign", "transfer", "progress", "top", "reset")
                    .filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toCollection(ArrayList::new));
        }

        if (args.length == 3) {
            String sub = args[1].toLowerCase();
            switch (sub) {
                case "delete" -> {
                    return registry.getHuntService().getHuntNames().stream()
                            .filter(n -> !n.equals("default"))
                            .filter(n -> n.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toCollection(ArrayList::new));
                }
                case "enable", "disable", "info", "select", "set", "assign", "progress", "top", "reset" -> {
                    return registry.getHuntService().getHuntNames().stream()
                            .filter(n -> n.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toCollection(ArrayList::new));
                }
                case "transfer" -> {
                    return registry.getHeadService().getHeadRawNameOrUuid().stream()
                            .filter(n -> n.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toCollection(ArrayList::new));
                }
            }
        }

        if (args.length >= 4) {
            String sub = args[1].toLowerCase();
            if ("delete".equals(sub)) {
                return getDeleteTabCompletions(args);
            }

            if (args.length == 4) {
                switch (sub) {
                    case "assign" -> {
                        return Stream.of("all", "radius")
                                .filter(s -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toCollection(ArrayList::new));
                    }
                    case "transfer" -> {
                        return registry.getHuntService().getHuntNames().stream()
                                .filter(n -> n.startsWith(args[3].toLowerCase()))
                                .collect(Collectors.toCollection(ArrayList::new));
                    }
                    case "progress", "reset" -> {
                        return Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
                                .collect(Collectors.toCollection(ArrayList::new));
                    }
                }
            }
        }

        return new ArrayList<>();
    }

    private ArrayList<String> getDeleteTabCompletions(String[] args) {
        String huntId = args[2].toLowerCase();
        String current = args[args.length - 1].toLowerCase();

        // Collect already-used flags
        Set<String> usedFlags = new HashSet<>();
        boolean hasKeepHeads = false;

        for (int i = 3; i < args.length - 1; i++) {
            String arg = args[i].toLowerCase();
            switch (arg) {
                case "--confirm" -> usedFlags.add("--confirm");
                case "--keepheads" -> {
                    usedFlags.add("--keepheads");
                    hasKeepHeads = true;
                }
                case "--fallback" -> {
                    usedFlags.add("--fallback");
                    i++;
                } // skip value
            }
        }

        // Check if the previous arg is --fallback (meaning current arg should be a hunt name)
        if (args.length > 4 && args[args.length - 2].equalsIgnoreCase("--fallback")) {
            return registry.getHuntService().getHuntNames().stream()
                    .filter(n -> !n.equals(huntId))
                    .filter(n -> n.startsWith(current))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        // Re-check hasKeepHeads including the last complete arg
        for (int i = 3; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--keepheads")) {
                hasKeepHeads = true;
                break;
            }
        }

        var suggestions = new ArrayList<String>();
        if (!usedFlags.contains("--confirm") && "--confirm".startsWith(current)) {
            suggestions.add("--confirm");
        }
        if (!usedFlags.contains("--keepheads") && "--keepHeads".toLowerCase().startsWith(current)) {
            suggestions.add("--keepHeads");
        }
        if (hasKeepHeads && !usedFlags.contains("--fallback") && "--fallback".startsWith(current)) {
            suggestions.add("--fallback");
        }
        return suggestions;
    }
}
