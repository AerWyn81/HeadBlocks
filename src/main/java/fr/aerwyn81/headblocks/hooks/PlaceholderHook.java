package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

public class PlaceholderHook extends PlaceholderExpansion {

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
        if (player == null) return "";

        // %headblocks_current% | %headblocks_left%
        if (identifier.equals("current") || identifier.equals("left")) {
            var future = StorageService.getHeadsPlayer(player.getUniqueId()).asFuture();

            try {
                var current = future.get().size();

                if (identifier.equals("current")) {
                    return "" + current;
                } else {
                    return "" + (StorageService.getHeads().size() - current);
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
                var top = new ArrayList<>(StorageService.getTopPlayers().entrySet());

                var positionParam = str[str.length - (str.length == 2 ? 1 : 2)];

                if (positionParam.equals("position")) {
                    var findPlayerPos = top.stream()
                            .filter(p -> p.getKey().uuid().equals(player.getUniqueId()))
                            .findFirst()
                            .orElse(null);

                    var playerPosition = top.indexOf(findPlayerPos);

                    if (playerPosition == -1)
                        return "-";

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
            try {
                return "" + StorageService.getHeads().size();
            } catch (InternalException e) {
                return "";
            }
        }

        // %headblocks_hasHead_<uuid|name>%
        if (identifier.contains("hasHead")) {
            var str = identifier.split("_");

            try {
                var hUUID = UUID.fromString(str[str.length - 1]);
                return String.valueOf(StorageService.hasHead(player.getUniqueId(), hUUID));
            } catch (Exception ignored) {
            }

            try {
                var name = identifier.replace("hasHead_", "");
                name = name.replaceAll("_", " ").trim();

                var head = HeadService.getHeadByName(name);

                if (head == null) {
                    return "Unknown head " + name;
                }

                return String.valueOf(StorageService.hasHead(player.getUniqueId(), head.getUuid()));
            } catch (Exception ignored) {
            }
        }

        // %headblocks_order_<previous|current|next>%
        if (identifier.contains("order")) {
            var str = identifier.split("_");

            if (str.length != 2)
                return "Placeholder not found!";

            var subIdentifier = str[1];

            var heads = new ArrayList<>(HeadService.getChargedHeadLocations());
            heads.sort(Comparator.comparingInt(HeadLocation::getOrderIndex));

            if (heads.isEmpty()) {
                return "No loaded heads";
            }

            var future = StorageService.getHeadsPlayer(player.getUniqueId()).asFuture();

            try {
                var playerHeadLocations = new ArrayList<HeadLocation>();
                for (var headLocation : heads) {
                    var optHead = future.get().stream().filter(uuid -> uuid.equals(headLocation.getUuid())).findFirst();
                    if (optHead.isPresent())
                        playerHeadLocations.add(headLocation);
                }

                if (subIdentifier.equals("current")) {
                    if (playerHeadLocations.size() - 1 < 0) {
                        return "-";
                    }

                    return playerHeadLocations.get(playerHeadLocations.size() - 1).getNameOrUuid();
                }

                if (subIdentifier.equals("previous")) {
                    if (playerHeadLocations.isEmpty() || playerHeadLocations.size() - 2 < 0) {
                        return "-";
                    }

                    return playerHeadLocations.get(playerHeadLocations.size() - 2).getNameOrUuid();
                }

                if (subIdentifier.equals("next")) {
                    if (playerHeadLocations.size() >= heads.size()) {
                        return "-";
                    }

                    return heads.get(playerHeadLocations.size()).getNameOrUuid();
                }
            } catch (Exception ex) {
                return "Future error get heads";
            }
        }

        return null;
    }
}