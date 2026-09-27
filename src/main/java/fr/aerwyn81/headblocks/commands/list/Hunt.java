package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HuntCreateEvent;
import fr.aerwyn81.headblocks.api.events.HuntDeleteEvent;
import fr.aerwyn81.headblocks.api.events.HuntStateChangeEvent;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.commands.list.schedule.ScheduleCommandHandler;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.data.head.visual.RenderMode;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.TimedBehavior;
import fr.aerwyn81.headblocks.utils.bukkit.HeadTargeting;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@HBAnnotations(command = "hunt", permission = "headblocks.admin")
public class Hunt implements Cmd {
    private static final String RENDERING = "rendering";
    private static final String RENDERING_PLACEHOLDER = "%rendering%";
    private static final String SCHEDULE = "schedule";
    private static final String MESSAGES_HUNT_USAGE = "Messages.HuntUsage";
    private static final String MESSAGES_STORAGE_ERROR = "Messages.StorageError";
    private static final String MESSAGES_HUNT_NOT_FOUND = "Messages.HuntNotFound";
    private static final String MESSAGES_PLAYER_ONLY = "Messages.PlayerOnly";
    private static final String CONFIRM = "--confirm";
    private static final String KEEP_HEADS = "--keepheads";
    private static final String FALLBACK = "--fallback";
    private static final String HEAD_COUNT_PLACEHOLDER = "%headCount%";
    private static final String DELETE = "delete";
    private static final String ENABLE = "enable";
    private static final String DISABLE = "disable";
    private static final String SELECT = "select";
    private static final String ASSIGN = "assign";
    private static final String TRANSFER = "transfer";
    private static final String PROGRESS = "progress";
    private static final String RESET = "reset";
    private static final String HUNT_PLACEHOLDER = "%hunt%";
    private static final String COUNT_PLACEHOLDER = "%count%";
    private static final String DISPLAY_NAME_PLACEHOLDER = "%displayName%";

    private final ServiceRegistry registry;
    private final ScheduleCommandHandler scheduleHandler;

    public Hunt(ServiceRegistry registry) {
        this.registry = registry;
        this.scheduleHandler = new ScheduleCommandHandler(registry);
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case DELETE -> handleDelete(sender, args);
            case ENABLE -> handleEnable(sender, args);
            case DISABLE -> handleDisable(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case SELECT -> handleSelect(sender, args);
            case "active" -> handleActive(sender);
            case "set" -> handleSet(sender, args);
            case ASSIGN -> handleAssign(sender, args);
            case TRANSFER -> handleTransfer(sender, args);
            case PROGRESS -> handleProgress(sender, args);
            case "top" -> handleTop(sender, args);
            case RESET -> handleReset(sender, args);
            case SCHEDULE -> handleSchedule(sender, args);
            case RENDERING -> handleRendering(sender, args);
            default -> sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
        }
    }

    // --- E2: CRUD ---

    private void handleCreate(CommandSender sender, String[] args) {
        String name = args.length >= 3 ? args[2] : nextFreeHuntName();

        if (!name.matches("[a-zA-Z0-9-]+")) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntInvalidName"));
            return;
        }

        if (registry.getHuntService().getHuntNames().stream().anyMatch(n -> n.equalsIgnoreCase(name))) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntAlreadyExists")
                    .replace(HUNT_PLACEHOLDER, name));
            return;
        }

        // If the sender is a player, open the behavior selection GUI
        if (sender instanceof Player player) {
            registry.getGuiService().getBehaviorSelectionManager().open(player, name);
            return;
        }

        // Console: create directly with FreeBehavior
        String huntId = name.toLowerCase();
        HBHunt hunt = new HBHunt(registry.getConfigService(), huntId, name, HuntState.ACTIVE, 1, "PLAYER_HEAD");

        HuntCreateEvent createEvent = new HuntCreateEvent(hunt);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (createEvent.isCancelled()) {
            return;
        }

        registry.getHuntConfigService().saveHunt(hunt);

        try {
            registry.getStorageService().createHuntInDb(hunt.getId(), hunt.getDisplayName(), hunt.getState().name());
        } catch (Exception e) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            return;
        }

        registry.getHuntService().registerHunt(hunt);
        registry.getStorageService().incrementHuntVersion();

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntCreated")
                .replace(HUNT_PLACEHOLDER, hunt.getId()));
    }

    private String nextFreeHuntName() {
        Set<String> taken = registry.getHuntService().getHuntNames().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        int index = 1;
        while (taken.contains(String.valueOf(index))) {
            index++;
        }

        return String.valueOf(index);
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return;
        }

        String huntId = args[2].toLowerCase();

        if (HBHunt.DEFAULT_ID.equals(huntId)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntCannotDeleteDefault"));
            return;
        }

        HBHunt hunt = registry.getHuntService().getHuntById(huntId);
        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_NOT_FOUND)
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        var flags = parseDeleteFlags(args);
        boolean keepHeads = flags.keepHeads();
        String fallbackHuntId = flags.fallbackHuntId();

        // Validate: --fallback requires --keepHeads
        if (fallbackHuntId != null && !keepHeads) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleteFallbackRequiresKeepHeads"));
            return;
        }

        String resolvedFallback = resolveFallback(keepHeads, fallbackHuntId);

        if (!isFallbackKnown(sender, keepHeads, fallbackHuntId)) {
            return;
        }

        // Show confirmation message if --confirm not provided
        if (!flags.confirm()) {
            sendDeleteConfirmation(sender, hunt, huntId, keepHeads, resolvedFallback);
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

    private static int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private record DeleteFlags(boolean confirm, boolean keepHeads, String fallbackHuntId) {
    }

    private static DeleteFlags parseDeleteFlags(String[] args) {
        boolean hasConfirm = false;
        boolean keepHeads = false;
        String fallbackHuntId = null;

        int i = 3;
        while (i < args.length) {
            String arg = args[i].toLowerCase();
            if (CONFIRM.equals(arg)) {
                hasConfirm = true;
            } else if (KEEP_HEADS.equals(arg)) {
                keepHeads = true;
            } else if (FALLBACK.equals(arg) && i + 1 < args.length) {
                i++;
                fallbackHuntId = args[i].toLowerCase();
            }
            i++;
        }

        return new DeleteFlags(hasConfirm, keepHeads, fallbackHuntId);
    }

    private static String resolveFallback(boolean keepHeads, String fallbackHuntId) {
        if (!keepHeads) {
            return null;
        }
        return fallbackHuntId != null ? fallbackHuntId : HBHunt.DEFAULT_ID;
    }

    private boolean isFallbackKnown(CommandSender sender, boolean keepHeads, String fallbackHuntId) {
        if (keepHeads && fallbackHuntId != null && registry.getHuntService().getHuntById(fallbackHuntId) == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleteFallbackNotFound")
                    .replace(HUNT_PLACEHOLDER, fallbackHuntId));
            return false;
        }
        return true;
    }

    private void sendDeleteConfirmation(CommandSender sender, HBHunt hunt, String huntId, boolean keepHeads, String resolvedFallback) {
        if (keepHeads) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleteKeepHeadsConfirm")
                    .replace(HUNT_PLACEHOLDER, huntId)
                    .replace(HEAD_COUNT_PLACEHOLDER, String.valueOf(hunt.getHeadCount()))
                    .replace("%fallback%", resolvedFallback));
        } else {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleteConfirm")
                    .replace(HUNT_PLACEHOLDER, huntId)
                    .replace(HEAD_COUNT_PLACEHOLDER, String.valueOf(hunt.getHeadCount())));
        }
    }

    private void handleDeleteWithHeads(CommandSender sender, HBHunt hunt, String huntId) {
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleteInProgress")
                .replace(HUNT_PLACEHOLDER, huntId));

        removeStartPlate(hunt);
        registry.getSpawnService().deleteHunt(huntId);

        // Collect HeadLocation objects for this hunt
        var headsToRemove = new ArrayList<HeadLocation>();
        for (UUID headUUID : hunt.getHeadUUIDs()) {
            HeadLocation hl = registry.getHeadService().getHeadByUUID(headUUID);
            if (hl != null) {
                headsToRemove.add(hl);
            }
        }

        // Remove heads physically (world + DB + hunt YAML) async
        registry.getHeadService().removeAllHeadLocationsAsync(headsToRemove, true, headRemoved -> {
            try {
                registry.getStorageService().deletePlayerProgressForHunt(huntId);
                registry.getStorageService().deleteHuntFromDb(huntId);
            } catch (Exception e) {
                sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
                LogUtil.error("Error during hunt delete cleanup: {0}", e.getMessage());
                return;
            }

            registry.getHuntConfigService().deleteHuntFile(huntId);
            registry.getHuntService().unregisterHunt(huntId);
            registry.getStorageService().incrementHuntVersion();

            sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleted")
                    .replace(HUNT_PLACEHOLDER, huntId));
        });
    }

    private void handleDeleteKeepHeads(CommandSender sender, HBHunt hunt, String huntId, String fallbackHuntId) {
        removeStartPlate(hunt);
        registry.getSpawnService().deleteHunt(huntId);

        try {
            // Transfer heads to fallback hunt via YAML
            for (UUID headUUID : new ArrayList<>(hunt.getHeadUUIDs())) {
                HeadLocation hl = registry.getHeadService().getHeadByUUID(headUUID);
                if (hl != null) {
                    registry.getHuntService().transferHead(hl, fallbackHuntId);
                }
            }

            // Transfer player progress to fallback
            registry.getStorageService().transferPlayerProgress(huntId, fallbackHuntId);

            registry.getStorageService().deleteHuntFromDb(huntId);
        } catch (Exception e) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            LogUtil.error("Error during hunt delete (keepHeads): {0}", e.getMessage());
            return;
        }

        registry.getHuntConfigService().deleteHuntFile(huntId);
        registry.getHuntService().unregisterHunt(huntId);
        registry.getStorageService().incrementHuntVersion();

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntDeleted")
                .replace(HUNT_PLACEHOLDER, huntId));
    }

    private void removeStartPlate(HBHunt hunt) {
        for (Behavior behavior : hunt.getBehaviors()) {
            if (behavior instanceof TimedBehavior tb && tb.startPlateLocation() != null
                    && tb.startPlateLocation().getWorld() != null) {
                tb.startPlateLocation().getBlock().setType(Material.AIR);
            }
        }
    }

    private void handleEnable(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return;
        }

        String huntId = args[2].toLowerCase();
        HBHunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_NOT_FOUND)
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        if (hunt.isActive()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntAlreadyActive")
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        HuntStateChangeEvent stateEvent = new HuntStateChangeEvent(hunt, hunt.getState(), HuntState.ACTIVE);
        Bukkit.getPluginManager().callEvent(stateEvent);
        if (stateEvent.isCancelled()) {
            return;
        }

        try {
            registry.getHuntService().changeState(hunt, HuntState.ACTIVE);
        } catch (Exception e) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntEnabled")
                .replace(HUNT_PLACEHOLDER, huntId));
    }

    private void handleDisable(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return;
        }

        String huntId = args[2].toLowerCase();
        HBHunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_NOT_FOUND)
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        if (!hunt.isActive()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntAlreadyInactive")
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        HuntStateChangeEvent stateEvent = new HuntStateChangeEvent(hunt, hunt.getState(), HuntState.INACTIVE);
        Bukkit.getPluginManager().callEvent(stateEvent);
        if (stateEvent.isCancelled()) {
            return;
        }

        try {
            registry.getHuntService().changeState(hunt, HuntState.INACTIVE);
        } catch (Exception e) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntDisabled")
                .replace(HUNT_PLACEHOLDER, huntId));
    }

    private void handleList(CommandSender sender) {
        var hunts = registry.getHuntService().getAllHunts();

        if (hunts.isEmpty()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntListEmpty"));
            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntListHeader")
                .replace(COUNT_PLACEHOLDER, String.valueOf(hunts.size())));

        for (HBHunt hunt : hunts) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntListEntry")
                    .replace(HUNT_PLACEHOLDER, hunt.getId())
                    .replace(DISPLAY_NAME_PLACEHOLDER, hunt.getDisplayName())
                    .replace("%state%", hunt.getState().getLocalizedName(registry.getLanguageService()))
                    .replace(HEAD_COUNT_PLACEHOLDER, String.valueOf(hunt.getHeadCount())));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return;
        }

        String huntId = args[2].toLowerCase();
        HBHunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_NOT_FOUND)
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoHeader")
                .replace(HUNT_PLACEHOLDER, hunt.getId()));
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoName")
                .replace(DISPLAY_NAME_PLACEHOLDER, hunt.getDisplayName()));
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoState")
                .replace("%state%", hunt.getState().getLocalizedName(registry.getLanguageService())));
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoPriority")
                .replace("%priority%", String.valueOf(hunt.getPriority())));
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoHeads")
                .replace(HEAD_COUNT_PLACEHOLDER, String.valueOf(hunt.getHeadCount())));
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoBehaviors")
                .replace("%behaviors%", hunt.getBehaviors().stream()
                        .map(Behavior::getId).collect(Collectors.joining(", "))));
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoRendering")
                .replace(RENDERING_PLACEHOLDER, hunt.getConfig().getRenderMode().name().toLowerCase()));

        try {
            int playerCount = registry.getStorageService().getTopPlayersForHunt(huntId).size();
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoPlayers")
                    .replace("%playerCount%", String.valueOf(playerCount)));
        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntInfoPlayers")
                    .replace("%playerCount%", "?"));
        }
    }

    private void handleRendering(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntRenderingUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        HBHunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_NOT_FOUND)
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntRenderingCurrent")
                    .replace(HUNT_PLACEHOLDER, hunt.getId())
                    .replace(RENDERING_PLACEHOLDER, hunt.getConfig().getRenderMode().name().toLowerCase()));
            return;
        }

        RenderMode mode = RenderMode.of(args[3]);
        if (mode == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntRenderingUsage"));
            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntRenderingInProgress")
                .replace(HUNT_PLACEHOLDER, hunt.getId())
                .replace(RENDERING_PLACEHOLDER, mode.name().toLowerCase()));

        registry.getVisualService().convertHunt(hunt, mode, report -> {
            registry.getStorageService().incrementHuntVersion();
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntRenderingDone")
                    .replace(HUNT_PLACEHOLDER, hunt.getId())
                    .replace(RENDERING_PLACEHOLDER, mode.name().toLowerCase())
                    .replace("%converted%", String.valueOf(report.converted()))
                    .replace("%unchanged%", String.valueOf(report.unchanged()))
                    .replace("%failed%", String.valueOf(report.failed() + report.skipped())));
        });
    }

    // --- E3: Head assignment ---


    private void handleSelect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_PLAYER_ONLY));
            return;
        }

        if (args.length < 3) {
            registry.getHuntService().clearSelectedHunt(player.getUniqueId());
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntSelectReset"));
            return;
        }

        String huntId = args[2].toLowerCase();
        HBHunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_NOT_FOUND)
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        registry.getHuntService().setSelectedHunt(player.getUniqueId(), huntId);
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntSelected")
                .replace(HUNT_PLACEHOLDER, huntId));
    }

    private void handleActive(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_PLAYER_ONLY));
            return;
        }

        String huntId = registry.getHuntService().getSelectedHunt(player.getUniqueId());
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntActiveSelection")
                .replace(HUNT_PLACEHOLDER, huntId));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_PLAYER_ONLY));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return;
        }

        String huntId = args[2].toLowerCase();

        if (!registry.getHuntService().huntExists(huntId)) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_NOT_FOUND)
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        HeadLocation headLocation = HeadTargeting.lookedAt(player, registry, 100);

        if (headLocation == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.NoTargetHeadBlock"));
            return;
        }

        if (registry.getHeadService().isSpawned(headLocation.getUuid())) {
            sender.sendMessage(registry.getLanguageService().message("Messages.SpawnHeadNotEditable"));
            return;
        }

        HBHunt targetHunt = registry.getHuntService().getHuntById(huntId);
        if (registry.getAreaEnforcementService().isLocationOutsideArea(targetHunt, headLocation.getLocation())) {
            sender.sendMessage(registry.getLanguageService().message("Messages.AreaHeadOutsideAssign")
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        try {
            registry.getHuntService().transferHead(headLocation, huntId);
        } catch (Exception e) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            LogUtil.error("Error transferring head to hunt: {0}", e.getMessage());
            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntHeadTransferred")
                .replace("%head%", headLocation.getNameOrUuid())
                .replace(HUNT_PLACEHOLDER, huntId));
    }

    private java.util.List<HeadLocation> radiusCandidates(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_PLAYER_ONLY));
            return null;
        }

        if (args.length < 5) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return null;
        }

        int radius;
        try {
            radius = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return null;
        }

        var playerLoc = player.getLocation();
        return registry.getHeadService().getHeadLocations().stream()
                .filter(h -> h.getLocation() != null
                        && h.getLocation().getWorld() != null
                        && h.getLocation().getWorld().equals(playerLoc.getWorld())
                        && h.getLocation().distance(playerLoc) <= radius)
                .toList();
    }

    private void handleAssign(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return;
        }

        String huntId = args[2].toLowerCase();

        if (!registry.getHuntService().huntExists(huntId)) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_NOT_FOUND)
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        String mode = args[3].toLowerCase();
        java.util.List<HeadLocation> candidates;

        switch (mode) {
            case "all" -> candidates = new ArrayList<>(registry.getHeadService().getHeadLocations());
            case "radius" -> {
                candidates = radiusCandidates(sender, args);
                if (candidates == null) {
                    return;
                }
            }
            default -> {
                sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
                return;
            }
        }

        if (candidates.isEmpty()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntAssignNoHeads"));
            return;
        }

        HBHunt targetHunt = registry.getHuntService().getHuntById(huntId);
        java.util.List<HeadLocation> headsToAssign = candidates.stream()
                .filter(h -> !targetHunt.containsHead(h.getUuid()))
                .toList();

        int skipped = candidates.size() - headsToAssign.size();

        java.util.List<HeadLocation> insideArea = headsToAssign.stream()
                .filter(h -> !registry.getAreaEnforcementService().isLocationOutsideArea(targetHunt, h.getLocation()))
                .toList();
        int outsideArea = headsToAssign.size() - insideArea.size();
        headsToAssign = insideArea;

        if (outsideArea > 0) {
            sender.sendMessage(registry.getLanguageService().message("Messages.AreaAssignSkipped")
                    .replace(COUNT_PLACEHOLDER, String.valueOf(outsideArea))
                    .replace(HUNT_PLACEHOLDER, huntId));
        }

        if (headsToAssign.isEmpty()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntAssignAllAlreadyInHunt")
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        int count = 0;
        for (HeadLocation head : headsToAssign) {
            try {
                registry.getHuntService().transferHead(head, huntId);
                count++;
            } catch (Exception e) {
                LogUtil.error("Error assigning head {0} to hunt {1}: {2}", head.getUuid(), huntId, e.getMessage());
            }
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntAssignSuccess")
                .replace(COUNT_PLACEHOLDER, String.valueOf(count))
                .replace("%skipped%", String.valueOf(skipped))
                .replace(HUNT_PLACEHOLDER, huntId));

        if (sender instanceof Player player) {
            registry.getHuntService().setSelectedHunt(player.getUniqueId(), huntId);
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntSelected")
                    .replace(HUNT_PLACEHOLDER, huntId));
        }
    }

    private void handleTransfer(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return;
        }

        UUID headUUID;
        try {
            headUUID = UUID.fromString(args[2]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntHeadNotFound")
                    .replace("%uuid%", args[2]));
            return;
        }

        HeadLocation headLocation = registry.getHeadService().getHeadByUUID(headUUID);
        if (headLocation == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntHeadNotFound")
                    .replace("%uuid%", args[2]));
            return;
        }

        if (registry.getHeadService().isSpawned(headLocation.getUuid())) {
            sender.sendMessage(registry.getLanguageService().message("Messages.SpawnHeadNotEditable"));
            return;
        }

        String huntId = args[3].toLowerCase();

        if (!registry.getHuntService().huntExists(huntId)) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_NOT_FOUND)
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        HBHunt targetHunt = registry.getHuntService().getHuntById(huntId);
        if (registry.getAreaEnforcementService().isLocationOutsideArea(targetHunt, headLocation.getLocation())) {
            sender.sendMessage(registry.getLanguageService().message("Messages.AreaHeadOutsideAssign")
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        try {
            registry.getHuntService().transferHead(headLocation, huntId);
        } catch (Exception e) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            LogUtil.error("Error transferring head to hunt: {0}", e.getMessage());
            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntHeadTransferred")
                .replace("%head%", headLocation.getNameOrUuid())
                .replace(HUNT_PLACEHOLDER, huntId));
    }

    // --- E7: Per-hunt commands ---

    private void handleProgress(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return;
        }

        String huntId = args[2].toLowerCase();
        HBHunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_NOT_FOUND)
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        // Resolve target player (self or other)
        PlayerProfileLight profile;
        if (args.length >= 4) {
            try {
                profile = registry.getStorageService().getPlayerByName(args[3]);
            } catch (InternalException e) {
                sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
                return;
            }
            if (profile == null) {
                sender.sendMessage(registry.getLanguageService().message("Messages.PlayerNotFound", args[3]));
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(registry.getLanguageService().message(MESSAGES_PLAYER_ONLY));
                return;
            }
            profile = new PlayerProfileLight(player.getUniqueId(), player.getName(), player.getDisplayName());
        }

        try {
            var huntHeads = registry.getStorageService().getHeadsPlayerForHunt(profile.uuid(), huntId);
            int current = huntHeads.size();
            int total = hunt.getTargetCount();

            String progress = MessageUtils.createProgressBar(current, total,
                    registry.getConfigService().progressBarBars(),
                    registry.getConfigService().progressBarSymbol(),
                    registry.getConfigService().progressBarCompletedColor(),
                    registry.getConfigService().progressBarNotCompletedColor());

            sender.sendMessage(registry.getLanguageService().message("Messages.HuntProgressDetail")
                    .replace("%player%", profile.name())
                    .replace(HUNT_PLACEHOLDER, hunt.getId())
                    .replace(DISPLAY_NAME_PLACEHOLDER, hunt.getDisplayName())
                    .replace("%current%", String.valueOf(current))
                    .replace("%max%", String.valueOf(total))
                    .replace("%progress%", progress));
        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            LogUtil.error("Error retrieving hunt progress: {0}", e.getMessage());
        }
    }

    private void showTopScores(CommandSender sender, HBHunt hunt, int limit) {
        try {
            var scores = new ArrayList<>(registry.getStorageService().getTopScoresForHunt(hunt.getId()).entrySet());
            if (scores.isEmpty()) {
                sender.sendMessage(registry.getLanguageService().message("Messages.TopEmpty"));
                return;
            }

            sender.sendMessage(registry.getLanguageService().message("Messages.HuntTopHeader")
                    .replace(HUNT_PLACEHOLDER, hunt.getId())
                    .replace(DISPLAY_NAME_PLACEHOLDER, hunt.getDisplayName()));

            for (int i = 0; i < Math.min(limit, scores.size()); i++) {
                var entry = scores.get(i);
                sender.sendMessage(MessageUtils.colorize(
                        registry.getLanguageService().message("Chat.LineTop", entry.getKey().name())
                                .replace("%pos%", String.valueOf(i + 1))
                                .replace(COUNT_PLACEHOLDER, MessageUtils.formatScore(entry.getValue()))));
            }
        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            LogUtil.error("Error retrieving hunt top scores: {0}", e.getMessage());
        }
    }

    private void handleTop(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return;
        }

        String huntId = args[2].toLowerCase();
        HBHunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_NOT_FOUND)
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        int limit = 10;
        if (args.length >= 4) {
            limit = parseIntOrDefault(args[3], limit);
        }

        if (hunt.scoresPoints()) {
            showTopScores(sender, hunt, limit);
            return;
        }

        try {
            var topPlayers = new ArrayList<>(registry.getStorageService().getTopPlayersForHunt(huntId).entrySet());

            if (topPlayers.isEmpty()) {
                sender.sendMessage(registry.getLanguageService().message("Messages.TopEmpty"));
                return;
            }

            sender.sendMessage(registry.getLanguageService().message("Messages.HuntTopHeader")
                    .replace(HUNT_PLACEHOLDER, hunt.getId())
                    .replace(DISPLAY_NAME_PLACEHOLDER, hunt.getDisplayName()));

            int count = Math.min(limit, topPlayers.size());
            for (int i = 0; i < count; i++) {
                Map.Entry<PlayerProfileLight, Integer> entry = topPlayers.get(i);
                sender.sendMessage(MessageUtils.colorize(
                        registry.getLanguageService().message("Chat.LineTop", entry.getKey().name())
                                .replace("%pos%", String.valueOf(i + 1))
                                .replace(COUNT_PLACEHOLDER, String.valueOf(entry.getValue()))));
            }
        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            LogUtil.error("Error retrieving hunt top players: {0}", e.getMessage());
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_USAGE));
            return;
        }

        String huntId = args[2].toLowerCase();
        HBHunt hunt = registry.getHuntService().getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_HUNT_NOT_FOUND)
                    .replace(HUNT_PLACEHOLDER, huntId));
            return;
        }

        String playerName = args[3];
        PlayerProfileLight profile;
        try {
            profile = registry.getStorageService().getPlayerByName(playerName);
        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            return;
        }

        if (profile == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.PlayerNotFound", playerName));
            return;
        }

        try {
            registry.getStorageService().resetPlayerHunt(profile.uuid(), huntId);
        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            LogUtil.error("Error resetting player {0} for hunt {1}: {2}", playerName, huntId, e.getMessage());
            return;
        }

        // Re-sync head visibility if PacketEvents active
        var targetPlayer = Bukkit.getPlayer(profile.uuid());
        if (targetPlayer != null) {
            registry.getVisibilityService().onHuntReset(targetPlayer, hunt);
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntPlayerReset")
                .replace("%player%", playerName)
                .replace(HUNT_PLACEHOLDER, huntId));
    }

    private void handleSchedule(CommandSender sender, String[] args) {
        scheduleHandler.handle(sender, args);
    }

    // --- Tab completion ---

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Stream.of("create", DELETE, ENABLE, DISABLE, "list", "info",
                            SELECT, "active", "set", ASSIGN, TRANSFER, PROGRESS, "top", RESET, SCHEDULE, RENDERING)
                    .filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toCollection(ArrayList::new));
        }

        if (args.length == 3) {
            String sub = args[1].toLowerCase();
            switch (sub) {
                case DELETE -> {
                    return registry.getHuntService().getHuntNames().stream()
                            .filter(n -> !n.equals(HBHunt.DEFAULT_ID))
                            .filter(n -> n.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toCollection(ArrayList::new));
                }
                case ENABLE, DISABLE, "info", SELECT, "set", ASSIGN, PROGRESS, "top", RESET, SCHEDULE,
                     RENDERING -> {
                    return registry.getHuntService().getHuntNames().stream()
                            .filter(n -> n.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toCollection(ArrayList::new));
                }
                case TRANSFER -> {
                    return registry.getHeadService().getHeadRawNameOrUuid().stream()
                            .filter(n -> n.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toCollection(ArrayList::new));
                }
                default -> {
                    return new ArrayList<>();
                }
            }
        }

        if (args.length >= 4) {
            String sub = args[1].toLowerCase();
            if (DELETE.equals(sub)) {
                return getDeleteTabCompletions(args);
            }

            if (SCHEDULE.equals(sub)) {
                return new ArrayList<>(scheduleHandler.tabComplete(args));
            }

            if (args.length == 4) {
                switch (sub) {
                    case ASSIGN -> {
                        return Stream.of("all", "radius")
                                .filter(s -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toCollection(ArrayList::new));
                    }
                    case TRANSFER -> {
                        return registry.getHuntService().getHuntNames().stream()
                                .filter(n -> n.startsWith(args[3].toLowerCase()))
                                .collect(Collectors.toCollection(ArrayList::new));
                    }
                    case RENDERING -> {
                        return Stream.of("block", "display")
                                .filter(s -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toCollection(ArrayList::new));
                    }
                    case PROGRESS, RESET -> {
                        return Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
                                .collect(Collectors.toCollection(ArrayList::new));
                    }
                    default -> {
                        return new ArrayList<>();
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

        int index = 3;
        while (index < args.length - 1) {
            String arg = args[index].toLowerCase();
            if (CONFIRM.equals(arg)) {
                usedFlags.add(CONFIRM);
            } else if (KEEP_HEADS.equals(arg)) {
                usedFlags.add(KEEP_HEADS);
                hasKeepHeads = true;
            } else if (FALLBACK.equals(arg)) {
                usedFlags.add(FALLBACK);
                index++;
            }
            index++;
        }

        // Check if the previous arg is --fallback (meaning current arg should be a hunt name)
        if (args.length > 4 && args[args.length - 2].equalsIgnoreCase(FALLBACK)) {
            return registry.getHuntService().getHuntNames().stream()
                    .filter(n -> !n.equals(huntId))
                    .filter(n -> n.startsWith(current))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        // Re-check hasKeepHeads including the last complete arg
        for (int i = 3; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(KEEP_HEADS)) {
                hasKeepHeads = true;
                break;
            }
        }

        return deleteFlagSuggestions(usedFlags, hasKeepHeads, current);
    }

    private static ArrayList<String> deleteFlagSuggestions(Set<String> usedFlags, boolean hasKeepHeads, String current) {
        var suggestions = new ArrayList<String>();
        if (!usedFlags.contains(CONFIRM) && CONFIRM.startsWith(current)) {
            suggestions.add(CONFIRM);
        }
        if (!usedFlags.contains(KEEP_HEADS) && "--keepHeads".toLowerCase().startsWith(current)) {
            suggestions.add("--keepHeads");
        }
        if (hasKeepHeads && !usedFlags.contains(FALLBACK) && FALLBACK.startsWith(current)) {
            suggestions.add(FALLBACK);
        }
        return suggestions;
    }
}
