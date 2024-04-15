package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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
            int current;
            try {
                current = StorageService.getHeadsPlayer(player.getUniqueId(), player.getName()).size();
            } catch (InternalException ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("Error while retrieving heads of " + player.getName() + ": " + ex.getMessage()));
                return "-1";
            }

            if (identifier.equals("current")) {
                return "" + current;
            } else {
                return "" + (HeadService.getChargedHeadLocations().size() - current);
            }
        }

        // %headblocks_leaderboard_<position>_<name|value>%
        if (identifier.contains("leaderboard")) {
            var str = identifier.split("_");
            try {
                var position = Integer.parseInt(str[str.length - (str.length == 2 ? 1 : 2)]);
                if (position < 1) {
                    position = 1;
                }

                var top = new ArrayList<>(StorageService.getTopPlayers().entrySet());

                if (position > top.size()) {
                    return "-";
                }

                var p = top.get(position - 1);

                var elt = str[str.length - 1];
                if (elt.equals("name")) {
                    return p.getKey();
                } else if (elt.equals("value")) {
                    return String.valueOf(p.getValue());
                } else {
                    return p.getKey() + ": " + p.getValue();
                }
            } catch (Exception ex) {
                return "Cannot parse the leaderboard placeholder. Use %headblocks_leaderboard_<position>_<name|value>%.";
            }
        }

        // %headblocks_max%
        if (identifier.equals("max")) {
            return "" +  HeadService.getChargedHeadLocations().size();
        }

        // %headblocks_hasHead_uuid%
        if (identifier.contains("hasHead")) {
            var str = identifier.split("_");

            try {
                var hUUID = UUID.fromString(str[str.length - 1]);
                return String.valueOf(StorageService.hasHead(player.getUniqueId(), hUUID));
            } catch (Exception ignored) { }
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

            List<UUID> playerHeads;

            try {
                playerHeads = StorageService.getHeadsPlayer(player.getUniqueId(), player.getName());
            } catch (Exception ex) {
                return "Internal error when retrieving player heads:" + ex.getMessage();
            }

            var playerHeadLocations = new ArrayList<HeadLocation>();
            for (var headLocation : heads) {
                var optHead = playerHeads.stream().filter(uuid -> uuid.equals(headLocation.getUuid())).findFirst();
                if (optHead.isPresent())
                    playerHeadLocations.add(headLocation);
            }

            if (subIdentifier.equals("current")) {
                if (playerHeadLocations.size() - 1 < 0) {
                    return "-";
                }

                return playerHeadLocations.get(playerHeadLocations.size() - 1).getDisplayedName();
            }

            if (subIdentifier.equals("previous")) {
                if (playerHeadLocations.isEmpty() || playerHeadLocations.size() - 2 < 0) {
                    return "-";
                }

                return playerHeadLocations.get(playerHeadLocations.size() - 2).getDisplayedName();
            }

            if (subIdentifier.equals("next")) {
                if (playerHeadLocations.size() >= heads.size()) {
                    return "-";
                }

                return heads.get(playerHeadLocations.size()).getDisplayedName();
            }
        }

        return null;
    }
}