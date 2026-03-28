package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

public class PlaceholderHook extends PlaceholderExpansion {

    private final ServiceRegistry registry;

    public PlaceholderHook(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean canRegister() {
        return Bukkit.getPluginManager().getPlugin("HeadBlocks") != null;
    }

    @Override
    public boolean register() {
        if (!canRegister()) {
            return false;
        }

        return super.register();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "headblocks";
    }

    @Override
    public @NotNull String getAuthor() {
        return "AerWyn81";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // %headblocks_current% | %headblocks_left%
        if (identifier.equals("current") || identifier.equals("left")) {
            if (registry.getHuntService().isMultiHunt()) {
                return "Use %headblocks_hunt_<name>_found% or %headblocks_hunt_<name>_left%";
            }

            var future = registry.getStorageService().getHeadsPlayer(player.getUniqueId()).asFuture();

            try {
                var current = future.get().size();

                if (identifier.equals("current")) {
                    return "" + current;
                } else {
                    return "" + (registry.getStorageService().getHeads().size() - current);
                }
            } catch (Exception ex) {
                return "Future error get heads";
            }
        }

        // %headblocks_leaderboard_position%
        // %headblocks_leaderboard_<position>_<name|custom|value>%
        if (identifier.contains("leaderboard")) {
            var str = identifier.split("_");
            try {
                var top = new ArrayList<>(registry.getStorageService().getTopPlayers().entrySet());

                var positionParam = str[str.length - (str.length == 2 ? 1 : 2)];

                if (positionParam.equals("position")) {
                    var findPlayerPos = top.stream()
                            .filter(p -> p.getKey().uuid().equals(player.getUniqueId()))
                            .findFirst()
                            .orElse(null);

                    var playerPosition = top.indexOf(findPlayerPos);

                    if (playerPosition == -1) {
                        return "-";
                    }

                    return String.valueOf(playerPosition + 1);
                }

                var position = Integer.parseInt(positionParam);
                if (position < 1) {
                    position = 1;
                }

                if (position > top.size()) {
                    return "-";
                }

                var p = top.get(position - 1);

                var elt = str[str.length - 1];

                switch (elt) {
                    case "name" -> {
                        return p.getKey().name();
                    }
                    case "custom" -> {
                        var displayName = p.getKey().customDisplay();
                        return displayName.isEmpty() ? p.getKey().name() : displayName;
                    }
                    case "value" -> {
                        return String.valueOf(p.getValue());
                    }
                    default -> {
                        return p.getKey().customDisplay() + " (" + p.getKey().name() + ") " + ": " + p.getValue();
                    }
                }
            } catch (Exception ex) {
                return "Cannot parse the leaderboard placeholder. Use %headblocks_leaderboard_<position>_<name|custom|value>%.";
            }
        }

        // %headblocks_max%
        if (identifier.equals("max")) {
            if (registry.getHuntService().isMultiHunt()) {
                return "Use %headblocks_hunt_<name>_total%";
            }

            try {
                return "" + registry.getStorageService().getHeads().size();
            } catch (InternalException e) {
                return "";
            }
        }

        // %headblocks_hasHead_<uuid|name>%
        if (identifier.contains("hasHead")) {
            var str = identifier.split("_");

            try {
                var hUUID = UUID.fromString(str[str.length - 1]);
                return String.valueOf(registry.getStorageService().hasHead(player.getUniqueId(), hUUID));
            } catch (IllegalArgumentException ignored) {
                // Not a valid UUID, try name-based lookup below
            } catch (Exception ex) {
                return "Storage error retrieving heads";
            }

            try {
                var name = identifier.replace("hasHead_", "");
                name = name.replaceAll("_", " ").trim();

                var head = registry.getHeadService().getHeadByName(name);

                if (head == null) {
                    return "Unknown head " + name;
                }

                return String.valueOf(registry.getStorageService().hasHead(player.getUniqueId(), head.getUuid()));
            } catch (Exception ex) {
                return "Storage error retrieving heads";
            }
        }

        // %headblocks_hunt_<huntId>_found% | %headblocks_hunt_<huntId>_total% | %headblocks_hunt_<huntId>_progress% | %headblocks_hunt_<huntId>_left%
        if (identifier.startsWith("hunt_")) {
            var knownSuffixes = Set.of("found", "total", "left", "progress", "name", "state",
                    "besttime", "timedcount", "timeposition", "timetop");

            // Strip leading "hunt_"
            String remainder = identifier.substring("hunt_".length());

            // Scan left-to-right for the first known suffix keyword
            String huntId = null;
            String subType = null;
            int searchFrom = 0;
            while (searchFrom < remainder.length()) {
                int underscorePos = remainder.indexOf('_', searchFrom);
                if (underscorePos < 0) {
                    break;
                }
                String afterUnderscore = remainder.substring(underscorePos + 1);
                int nextUnderscore = afterUnderscore.indexOf('_');
                String firstWord = nextUnderscore >= 0 ? afterUnderscore.substring(0, nextUnderscore) : afterUnderscore;

                if (knownSuffixes.contains(firstWord)) {
                    huntId = remainder.substring(0, underscorePos).toLowerCase();
                    subType = afterUnderscore;
                    break;
                }
                searchFrom = underscorePos + 1;
            }

            // Fallback: if no known suffix found, use simple split (first segment = huntId)
            if (huntId == null) {
                int firstUnderscore = remainder.indexOf('_');
                if (firstUnderscore > 0) {
                    huntId = remainder.substring(0, firstUnderscore).toLowerCase();
                    subType = remainder.substring(firstUnderscore + 1);
                }
            }

            if (huntId == null || huntId.isEmpty()) {
                return "";
            }

            HBHunt hunt = registry.getHuntService().getHuntById(huntId);
            if (hunt == null) {
                return "";
            }

            switch (subType) {
                case "found" -> {
                    try {
                        return String.valueOf(registry.getStorageService().getHeadsPlayerForHunt(player.getUniqueId(), huntId).size());
                    } catch (InternalException e) {
                        return "0";
                    }
                }
                case "total" -> {
                    return String.valueOf(hunt.getHeadCount());
                }
                case "left" -> {
                    try {
                        int found = registry.getStorageService().getHeadsPlayerForHunt(player.getUniqueId(), huntId).size();
                        return String.valueOf(hunt.getHeadCount() - found);
                    } catch (InternalException e) {
                        return String.valueOf(hunt.getHeadCount());
                    }
                }
                case "progress" -> {
                    try {
                        int found = registry.getStorageService().getHeadsPlayerForHunt(player.getUniqueId(), huntId).size();
                        int total = hunt.getHeadCount();
                        return MessageUtils.createProgressBar(found, total,
                                registry.getConfigService().progressBarBars(),
                                registry.getConfigService().progressBarSymbol(),
                                registry.getConfigService().progressBarCompletedColor(),
                                registry.getConfigService().progressBarNotCompletedColor());
                    } catch (InternalException e) {
                        return "";
                    }
                }
                case "name" -> {
                    return hunt.getDisplayName();
                }
                case "state" -> {
                    return hunt.getState().getLocalizedName(registry.getLanguageService());
                }
                case "besttime" -> {
                    try {
                        Long best = registry.getStorageService().getBestTime(player.getUniqueId(), huntId);
                        if (best == null) {
                            return "-";
                        }
                        return TimedRunManager.formatTime(best);
                    } catch (InternalException e) {
                        return "-";
                    }
                }
                case "timedcount" -> {
                    try {
                        return String.valueOf(registry.getStorageService().getTimedRunCount(player.getUniqueId(), huntId));
                    } catch (InternalException e) {
                        return "0";
                    }
                }
                case "timeposition" -> {
                    try {
                        var leaderboard = new ArrayList<>(registry.getStorageService().getTimedLeaderboard(huntId, 50).entrySet());
                        for (int i = 0; i < leaderboard.size(); i++) {
                            if (leaderboard.get(i).getKey().uuid().equals(player.getUniqueId())) {
                                return String.valueOf(i + 1);
                            }
                        }
                        return "-";
                    } catch (InternalException e) {
                        return "-";
                    }
                }
                default -> {
                    // Handle timetop_<pos>_<name|time>
                    if (subType.startsWith("timetop_")) {
                        try {
                            String[] ttParts = subType.split("_"); // timetop, <pos>, <name|time>
                            if (ttParts.length < 3) {
                                return "";
                            }

                            int pos = Integer.parseInt(ttParts[1]);
                            String field = ttParts[2];

                            var leaderboard = new ArrayList<>(registry.getStorageService().getTimedLeaderboard(huntId, pos).entrySet());
                            if (pos < 1 || pos > leaderboard.size()) {
                                return "-";
                            }

                            var entry = leaderboard.get(pos - 1);
                            return switch (field) {
                                case "name" -> entry.getKey().name();
                                case "time" -> TimedRunManager.formatTime(entry.getValue());
                                default -> "-";
                            };
                        } catch (Exception e) {
                            return "-";
                        }
                    }
                    return "";
                }
            }
        }

        // %headblocks_order_<previous|current|next>%
        if (identifier.contains("order")) {
            var str = identifier.split("_");

            if (str.length != 2) {
                return "Placeholder not found!";
            }

            var subIdentifier = str[1];

            var heads = new ArrayList<>(registry.getHeadService().getChargedHeadLocations());
            heads.sort(Comparator.comparingInt(HeadLocation::getOrderIndex));

            if (heads.isEmpty()) {
                return "No loaded heads";
            }

            var future = registry.getStorageService().getHeadsPlayer(player.getUniqueId()).asFuture();

            try {
                var playerHeadLocations = new ArrayList<HeadLocation>();
                for (var headLocation : heads) {
                    var optHead = future.get().stream().filter(uuid -> uuid.equals(headLocation.getUuid())).findFirst();
                    if (optHead.isPresent()) {
                        playerHeadLocations.add(headLocation);
                    }
                }

                switch (subIdentifier) {
                    case "current" -> {
                        if (playerHeadLocations.size() - 1 < 0) {
                            return "-";
                        }

                        return playerHeadLocations.get(playerHeadLocations.size() - 1).getNameOrUuid();
                    }
                    case "previous" -> {
                        if (playerHeadLocations.isEmpty() || playerHeadLocations.size() - 2 < 0) {
                            return "-";
                        }

                        return playerHeadLocations.get(playerHeadLocations.size() - 2).getNameOrUuid();
                    }
                    case "next" -> {
                        if (playerHeadLocations.size() >= heads.size()) {
                            return "-";
                        }

                        return heads.get(playerHeadLocations.size()).getNameOrUuid();
                    }
                }

            } catch (Exception ex) {
                return "Future error get heads";
            }
        }

        return null;
    }
}
