package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@HBAnnotations(command = "resetall", permission = "headblocks.admin", isPlayerCommand = true)
public class ResetAll extends ResetBase {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        var player = (Player) sender;

        boolean hasConfirm = hasParameterConfirm(args);

        UUID headUuid = resolveHeadFromArgs(player, args, 1);

        if (hasHeadParameter(args, 1) && headUuid == null) {
            return true;
        }

        if (headUuid != null) {
            resetHeadForAllPlayers(player, headUuid, hasConfirm);
        } else {
            resetAllHeadsForAllPlayers(player, hasConfirm);
        }

        return true;
    }

    private boolean hasParameterConfirm(String[] args) {
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--confirm")) {
                return true;
            }
        }
        return false;
    }

    private void resetHeadForAllPlayers(Player player, UUID headUuid, boolean hasConfirm) {
        List<UUID> playersWithHead;

        try {
            playersWithHead = StorageService.getPlayers(headUuid);
        } catch (InternalException ex) {
            player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while retrieving players from the storage: " + ex.getMessage()));
            return;
        }

        if (playersWithHead.isEmpty()) {
            player.sendMessage(LanguageService.getMessage("Messages.ResetAllNoData"));
            return;
        }

        String headName = getHeadDisplayName(headUuid);

        if (hasConfirm) {
            for (UUID playerUuid : playersWithHead) {
                try {
                    StorageService.resetPlayerHead(playerUuid, headUuid);

                    Player onlinePlayer = Bukkit.getPlayer(playerUuid);
                    if (onlinePlayer != null) {
                        var packetEventsHook = HeadBlocks.getInstance().getPacketEventsHook();
                        if (packetEventsHook != null && packetEventsHook.isEnabled() && packetEventsHook.getHeadHidingListener() != null) {
                            packetEventsHook.getHeadHidingListener().removeFoundHead(onlinePlayer, headUuid);
                        }
                    }
                } catch (InternalException ex) {
                    player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while resetting the player UUID " + playerUuid.toString() + " from the storage: " + ex.getMessage()));
                    return;
                }
            }

            player.sendMessage(LanguageService.getMessage("Messages.ResetAllHeadSuccess")
                    .replaceAll("%playerCount%", String.valueOf(playersWithHead.size()))
                    .replaceAll("%headName%", headName));
        } else {
            player.sendMessage(LanguageService.getMessage("Messages.ResetAllHeadConfirm")
                    .replaceAll("%playerCount%", String.valueOf(playersWithHead.size()))
                    .replaceAll("%headName%", headName));
        }

    }

    private void resetAllHeadsForAllPlayers(Player player, boolean hasConfirm) {
        List<UUID> allPlayers;

        try {
            allPlayers = StorageService.getAllPlayers();
        } catch (InternalException ex) {
            player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while retrieving all players from the storage: " + ex.getMessage()));
            return;
        }

        if (allPlayers.isEmpty()) {
            player.sendMessage(LanguageService.getMessage("Messages.ResetAllNoData"));
            return;
        }

        if (hasConfirm) {
            for (UUID uuid : allPlayers) {
                try {
                    StorageService.resetPlayer(uuid);

                    Player onlinePlayer = Bukkit.getPlayer(uuid);
                    if (onlinePlayer != null) {
                        var packetEventsHook = HeadBlocks.getInstance().getPacketEventsHook();
                        if (packetEventsHook != null && packetEventsHook.isEnabled() && packetEventsHook.getHeadHidingListener() != null) {
                            packetEventsHook.getHeadHidingListener().invalidatePlayerCache(uuid);
                        }
                    }
                } catch (InternalException ex) {
                    player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while resetting the player UUID " + uuid.toString() + " from the storage: " + ex.getMessage()));
                    return;
                }
            }

            player.sendMessage(LanguageService.getMessage("Messages.ResetAllSuccess")
                    .replaceAll("%playerCount%", String.valueOf(allPlayers.size())));
        } else {
            player.sendMessage(LanguageService.getMessage("Messages.ResetAllConfirm")
                    .replaceAll("%playerCount%", String.valueOf(allPlayers.size())));
        }

    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            ArrayList<String> options = new ArrayList<>();
            options.add("--confirm");
            options.add("--head");
            return options;
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("--head")) {
                ArrayList<String> options = new ArrayList<>();
                options.add("--confirm");
                options.addAll(getHeadCompletions());
                return options;
            } else if (args[1].equalsIgnoreCase("--confirm")) {
                return new ArrayList<>(Collections.singletonList("--head"));
            }
        } else if (args.length == 4) {
            if (args[1].equalsIgnoreCase("--head") && args[3].equalsIgnoreCase("--confirm")) {
                return new ArrayList<>();
            } else if (args[1].equalsIgnoreCase("--head") && !args[2].equalsIgnoreCase("--confirm")) {
                return new ArrayList<>(Collections.singletonList("--confirm"));
            } else if (args[2].equalsIgnoreCase("--head")) {
                return getHeadCompletions();
            }
        } else if (args.length == 5 && args[2].equalsIgnoreCase("--head")) {
            return new ArrayList<>(Collections.singletonList("--confirm"));
        }

        return new ArrayList<>();
    }
}
