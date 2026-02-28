package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HuntCreateEvent;
import fr.aerwyn81.headblocks.api.events.HuntDeleteEvent;
import fr.aerwyn81.headblocks.api.events.HuntStateChangeEvent;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@HBAnnotations(command = "hunt", permission = "headblocks.admin")
public class Hunt implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
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
            default -> sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
        }

        return true;
    }

    // --- E2: CRUD ---

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
            return;
        }

        String name = args[2];

        if (!name.matches("[a-zA-Z0-9-]+")) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntInvalidName"));
            return;
        }

        if (HuntService.getHuntNames().stream().anyMatch(n -> n.equalsIgnoreCase(name))) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntAlreadyExists")
                    .replaceAll("%hunt%", name));
            return;
        }

        String huntId = name.toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = new fr.aerwyn81.headblocks.data.hunt.Hunt(huntId, name, HuntState.ACTIVE, 1, "PLAYER_HEAD");

        HuntCreateEvent createEvent = new HuntCreateEvent(hunt);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (createEvent.isCancelled()) return;

        HuntConfigService.saveHunt(hunt);

        try {
            StorageService.createHuntInDb(hunt.getId(), hunt.getDisplayName(), hunt.getState().name());
        } catch (Exception e) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            return;
        }

        HuntService.registerHunt(hunt);
        StorageService.incrementHuntVersion();

        sender.sendMessage(LanguageService.getMessage("Messages.HuntCreated")
                .replaceAll("%hunt%", hunt.getId()));

        if (sender instanceof Player player) {
            HuntService.setSelectedHunt(player.getUniqueId(), hunt.getId());
            sender.sendMessage(LanguageService.getMessage("Messages.HuntSelected")
                    .replaceAll("%hunt%", hunt.getId()));
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();

        if ("default".equals(huntId)) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntCannotDeleteDefault"));
            return;
        }

        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = HuntService.getHuntById(huntId);
        if (hunt == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        boolean hasConfirm = false;
        for (int i = 3; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--confirm")) {
                hasConfirm = true;
                break;
            }
        }

        if (!hasConfirm) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntDeleteConfirm")
                    .replaceAll("%hunt%", huntId)
                    .replaceAll("%headCount%", String.valueOf(hunt.getHeadCount())));
            return;
        }

        HuntDeleteEvent deleteEvent = new HuntDeleteEvent(huntId);
        Bukkit.getPluginManager().callEvent(deleteEvent);
        if (deleteEvent.isCancelled()) return;

        try {
            // Reassign orphaned heads to default hunt before unlinking
            fr.aerwyn81.headblocks.data.hunt.Hunt defaultHunt = HuntService.getDefaultHunt();
            for (UUID headUUID : hunt.getHeadUUIDs()) {
                if (defaultHunt != null && !defaultHunt.containsHead(headUUID)) {
                    defaultHunt.addHead(headUUID);
                    StorageService.linkHeadToHunt(headUUID, "default");
                }
            }

            StorageService.unlinkAllHeadsFromHuntInDb(huntId);
            StorageService.resetAllPlayersForHunt(huntId);
            StorageService.deleteHuntFromDb(huntId);
        } catch (Exception e) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            return;
        }

        HuntConfigService.deleteHuntFile(huntId);
        HuntService.unregisterHunt(huntId);
        StorageService.incrementHuntVersion();

        sender.sendMessage(LanguageService.getMessage("Messages.HuntDeleted")
                .replaceAll("%hunt%", huntId));
    }

    private void handleEnable(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = HuntService.getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        if (hunt.isActive()) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntAlreadyActive")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        HuntStateChangeEvent stateEvent = new HuntStateChangeEvent(hunt, hunt.getState(), HuntState.ACTIVE);
        Bukkit.getPluginManager().callEvent(stateEvent);
        if (stateEvent.isCancelled()) return;

        hunt.setState(HuntState.ACTIVE);
        HuntConfigService.saveHunt(hunt);

        try {
            StorageService.updateHuntStateInDb(huntId, HuntState.ACTIVE.name());
        } catch (Exception e) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            return;
        }

        StorageService.incrementHuntVersion();

        sender.sendMessage(LanguageService.getMessage("Messages.HuntEnabled")
                .replaceAll("%hunt%", huntId));
    }

    private void handleDisable(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = HuntService.getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        if (!hunt.isActive()) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntAlreadyInactive")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        HuntStateChangeEvent stateEvent = new HuntStateChangeEvent(hunt, hunt.getState(), HuntState.INACTIVE);
        Bukkit.getPluginManager().callEvent(stateEvent);
        if (stateEvent.isCancelled()) return;

        hunt.setState(HuntState.INACTIVE);
        HuntConfigService.saveHunt(hunt);

        try {
            StorageService.updateHuntStateInDb(huntId, HuntState.INACTIVE.name());
        } catch (Exception e) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            return;
        }

        StorageService.incrementHuntVersion();

        sender.sendMessage(LanguageService.getMessage("Messages.HuntDisabled")
                .replaceAll("%hunt%", huntId));
    }

    private void handleList(CommandSender sender) {
        var hunts = HuntService.getAllHunts();

        if (hunts.isEmpty()) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntListEmpty"));
            return;
        }

        sender.sendMessage(LanguageService.getMessage("Messages.HuntListHeader")
                .replaceAll("%count%", String.valueOf(hunts.size())));

        for (fr.aerwyn81.headblocks.data.hunt.Hunt hunt : hunts) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntListEntry")
                    .replaceAll("%hunt%", hunt.getId())
                    .replaceAll("%displayName%", hunt.getDisplayName())
                    .replaceAll("%state%", hunt.getState().getLocalizedName())
                    .replaceAll("%headCount%", String.valueOf(hunt.getHeadCount())));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = HuntService.getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        sender.sendMessage(LanguageService.getMessage("Messages.HuntInfoHeader")
                .replaceAll("%hunt%", hunt.getId()));
        sender.sendMessage(LanguageService.getMessage("Messages.HuntInfoName")
                .replaceAll("%displayName%", hunt.getDisplayName()));
        sender.sendMessage(LanguageService.getMessage("Messages.HuntInfoState")
                .replaceAll("%state%", hunt.getState().getLocalizedName()));
        sender.sendMessage(LanguageService.getMessage("Messages.HuntInfoPriority")
                .replaceAll("%priority%", String.valueOf(hunt.getPriority())));
        sender.sendMessage(LanguageService.getMessage("Messages.HuntInfoHeads")
                .replaceAll("%headCount%", String.valueOf(hunt.getHeadCount())));
        sender.sendMessage(LanguageService.getMessage("Messages.HuntInfoBehaviors")
                .replaceAll("%behaviors%", hunt.getBehaviors().stream()
                        .map(Behavior::getId).collect(Collectors.joining(", "))));

        try {
            int playerCount = StorageService.getTopPlayersForHunt(huntId).size();
            sender.sendMessage(LanguageService.getMessage("Messages.HuntInfoPlayers")
                    .replaceAll("%playerCount%", String.valueOf(playerCount)));
        } catch (InternalException e) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntInfoPlayers")
                    .replaceAll("%playerCount%", "?"));
        }
    }

    // --- E3: Head assignment ---

    private void handleSelect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageService.getMessage("Messages.PlayerOnly"));
            return;
        }

        if (args.length < 3) {
            HuntService.clearSelectedHunt(player.getUniqueId());
            sender.sendMessage(LanguageService.getMessage("Messages.HuntSelectReset"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = HuntService.getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        HuntService.setSelectedHunt(player.getUniqueId(), huntId);
        sender.sendMessage(LanguageService.getMessage("Messages.HuntSelected")
                .replaceAll("%hunt%", huntId));
    }

    private void handleActive(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageService.getMessage("Messages.PlayerOnly"));
            return;
        }

        String huntId = HuntService.getSelectedHunt(player.getUniqueId());
        sender.sendMessage(LanguageService.getMessage("Messages.HuntActiveSelection")
                .replaceAll("%hunt%", huntId));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageService.getMessage("Messages.PlayerOnly"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();

        if (!HuntService.huntExists(huntId)) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        HeadLocation headLocation = HeadService.getHeadAt(player.getTargetBlock(null, 100).getLocation());

        if (headLocation == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.NoTargetHeadBlock"));
            return;
        }

        try {
            HuntService.transferHead(headLocation.getUuid(), huntId);
        } catch (Exception e) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            LogUtil.error("Error transferring head to hunt: {0}", e.getMessage());
            return;
        }

        sender.sendMessage(LanguageService.getMessage("Messages.HuntHeadTransferred")
                .replaceAll("%head%", headLocation.getNameOrUuid())
                .replaceAll("%hunt%", huntId));
    }

    private void handleAssign(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();

        if (!HuntService.huntExists(huntId)) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        String mode = args[3].toLowerCase();
        java.util.List<HeadLocation> headsToAssign;

        switch (mode) {
            case "all" -> {
                fr.aerwyn81.headblocks.data.hunt.Hunt defaultHunt = HuntService.getDefaultHunt();
                if (defaultHunt == null) {
                    sender.sendMessage(LanguageService.getMessage("Messages.HuntListEmpty"));
                    return;
                }
                headsToAssign = HeadService.getHeadLocations().stream()
                        .filter(h -> defaultHunt.containsHead(h.getUuid()))
                        .collect(Collectors.toList());
            }
            case "radius" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(LanguageService.getMessage("Messages.PlayerOnly"));
                    return;
                }

                if (args.length < 5) {
                    sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
                    return;
                }

                int radius;
                try {
                    radius = Integer.parseInt(args[4]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
                    return;
                }

                var playerLoc = player.getLocation();
                headsToAssign = HeadService.getHeadLocations().stream()
                        .filter(h -> h.getLocation() != null
                                && h.getLocation().getWorld() != null
                                && h.getLocation().getWorld().equals(playerLoc.getWorld())
                                && h.getLocation().distance(playerLoc) <= radius)
                        .collect(Collectors.toList());
            }
            default -> {
                sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
                return;
            }
        }

        if (headsToAssign.isEmpty()) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntAssignNoHeads"));
            return;
        }

        int count = 0;
        for (HeadLocation head : headsToAssign) {
            try {
                HuntService.transferHead(head.getUuid(), huntId);
                count++;
            } catch (Exception e) {
                LogUtil.error("Error assigning head {0} to hunt {1}: {2}", head.getUuid(), huntId, e.getMessage());
            }
        }

        sender.sendMessage(LanguageService.getMessage("Messages.HuntAssignSuccess")
                .replaceAll("%count%", String.valueOf(count))
                .replaceAll("%hunt%", huntId));

        if (sender instanceof Player player) {
            HuntService.setSelectedHunt(player.getUniqueId(), huntId);
            sender.sendMessage(LanguageService.getMessage("Messages.HuntSelected")
                    .replaceAll("%hunt%", huntId));
        }
    }

    private void handleTransfer(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
            return;
        }

        UUID headUUID;
        try {
            headUUID = UUID.fromString(args[2]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntHeadNotFound")
                    .replaceAll("%uuid%", args[2]));
            return;
        }

        HeadLocation headLocation = HeadService.getHeadByUUID(headUUID);
        if (headLocation == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntHeadNotFound")
                    .replaceAll("%uuid%", args[2]));
            return;
        }

        String huntId = args[3].toLowerCase();

        if (!HuntService.huntExists(huntId)) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        try {
            HuntService.transferHead(headUUID, huntId);
        } catch (Exception e) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            LogUtil.error("Error transferring head to hunt: {0}", e.getMessage());
            return;
        }

        sender.sendMessage(LanguageService.getMessage("Messages.HuntHeadTransferred")
                .replaceAll("%head%", headLocation.getNameOrUuid())
                .replaceAll("%hunt%", huntId));
    }

    // --- E7: Per-hunt commands ---

    private void handleProgress(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = HuntService.getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        // Resolve target player (self or other)
        PlayerProfileLight profile;
        if (args.length >= 4) {
            try {
                profile = StorageService.getPlayerByName(args[3]);
            } catch (InternalException e) {
                sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
                return;
            }
            if (profile == null) {
                sender.sendMessage(LanguageService.getMessage("Messages.PlayerNotFound", args[3]));
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(LanguageService.getMessage("Messages.PlayerOnly"));
                return;
            }
            profile = new PlayerProfileLight(player.getUniqueId(), player.getName(), player.getDisplayName());
        }

        try {
            ArrayList<UUID> huntHeads = StorageService.getHeadsPlayerForHunt(profile.uuid(), huntId);
            int current = huntHeads.size();
            int total = hunt.getHeadCount();

            String progress = MessageUtils.createProgressBar(current, total,
                    ConfigService.getProgressBarBars(),
                    ConfigService.getProgressBarSymbol(),
                    ConfigService.getProgressBarCompletedColor(),
                    ConfigService.getProgressBarNotCompletedColor());

            sender.sendMessage(LanguageService.getMessage("Messages.HuntProgressDetail")
                    .replaceAll("%player%", profile.name())
                    .replaceAll("%hunt%", hunt.getId())
                    .replaceAll("%displayName%", hunt.getDisplayName())
                    .replaceAll("%current%", String.valueOf(current))
                    .replaceAll("%max%", String.valueOf(total))
                    .replaceAll("%progress%", progress));
        } catch (InternalException e) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            LogUtil.error("Error retrieving hunt progress: {0}", e.getMessage());
        }
    }

    private void handleTop(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = HuntService.getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntNotFound")
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
            var topPlayers = new ArrayList<>(StorageService.getTopPlayersForHunt(huntId).entrySet());

            if (topPlayers.isEmpty()) {
                sender.sendMessage(LanguageService.getMessage("Messages.TopEmpty"));
                return;
            }

            sender.sendMessage(LanguageService.getMessage("Messages.HuntTopHeader")
                    .replaceAll("%hunt%", hunt.getId())
                    .replaceAll("%displayName%", hunt.getDisplayName()));

            int count = Math.min(limit, topPlayers.size());
            for (int i = 0; i < count; i++) {
                Map.Entry<PlayerProfileLight, Integer> entry = topPlayers.get(i);
                sender.sendMessage(MessageUtils.colorize(
                        LanguageService.getMessage("Chat.LineTop", entry.getKey().name())
                                .replaceAll("%pos%", String.valueOf(i + 1))
                                .replaceAll("%count%", String.valueOf(entry.getValue()))));
            }
        } catch (InternalException e) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            LogUtil.error("Error retrieving hunt top players: {0}", e.getMessage());
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        fr.aerwyn81.headblocks.data.hunt.Hunt hunt = HuntService.getHuntById(huntId);

        if (hunt == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.HuntNotFound")
                    .replaceAll("%hunt%", huntId));
            return;
        }

        String playerName = args[3];
        PlayerProfileLight profile;
        try {
            profile = StorageService.getPlayerByName(playerName);
        } catch (InternalException e) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            return;
        }

        if (profile == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.PlayerNotFound", playerName));
            return;
        }

        try {
            StorageService.resetPlayerHunt(profile.uuid(), huntId);
        } catch (InternalException e) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
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

        sender.sendMessage(LanguageService.getMessage("Messages.HuntPlayerReset")
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
                    return HuntService.getHuntNames().stream()
                            .filter(n -> !n.equals("default"))
                            .filter(n -> n.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toCollection(ArrayList::new));
                }
                case "enable", "disable", "info", "select", "set", "assign", "progress", "top", "reset" -> {
                    return HuntService.getHuntNames().stream()
                            .filter(n -> n.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toCollection(ArrayList::new));
                }
                case "transfer" -> {
                    return HeadService.getHeadRawNameOrUuid().stream()
                            .filter(n -> n.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toCollection(ArrayList::new));
                }
            }
        }

        if (args.length == 4) {
            String sub = args[1].toLowerCase();
            switch (sub) {
                case "delete" -> {
                    if (args[3].isEmpty() || "--confirm".startsWith(args[3].toLowerCase())) {
                        return new ArrayList<>(Collections.singletonList("--confirm"));
                    }
                }
                case "assign" -> {
                    return Stream.of("all", "radius")
                            .filter(s -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toCollection(ArrayList::new));
                }
                case "transfer" -> {
                    return HuntService.getHuntNames().stream()
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

        return new ArrayList<>();
    }
}
