package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@HBAnnotations(command = "resetall", permission = "headblocks.admin")
public class ResetAll extends ResetBase {
    private static final String MESSAGES_STORAGE_ERROR = "Messages.StorageError";
    private static final String CONFIRM = "--confirm";
    private static final String PLAYER_COUNT_PLACEHOLDER = "%playerCount%";
    private static final String HEAD_FLAG = "--head";

    public ResetAll(ServiceRegistry registry) {
        super(registry);
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        boolean hasConfirm = hasParameterConfirm(args);

        var headUuid = resolveHeadFromArgs(sender, args, 1);

        if (hasHeadParameter(args, 1) && headUuid == null) {
            return;
        }

        if (headUuid != null) {
            resetHeadForAllPlayers(sender, headUuid, hasConfirm);
        } else {
            resetAllHeadsForAllPlayers(sender, hasConfirm);
        }
    }

    private boolean hasParameterConfirm(String[] args) {
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(CONFIRM)) {
                return true;
            }
        }
        return false;
    }

    private void resetHeadForAllPlayers(CommandSender sender, UUID headUuid, boolean hasConfirm) {
        List<UUID> playersWithHead;

        try {
            playersWithHead = registry.getStorageService().getPlayers(headUuid);
        } catch (InternalException ex) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            LogUtil.error("Error while retrieving players from the storage: {0}", ex.getMessage());
            return;
        }

        if (playersWithHead.isEmpty()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ResetAllNoData"));
            return;
        }

        String headName = getHeadDisplayName(headUuid);

        if (hasConfirm) {
            for (UUID playerUuid : playersWithHead) {
                try {
                    registry.getStorageService().resetPlayerHead(playerUuid, headUuid);

                    Player onlinePlayer = Bukkit.getPlayer(playerUuid);
                    if (onlinePlayer != null) {
                        registry.getVisibilityService().onHeadReset(onlinePlayer, headUuid);
                    }
                } catch (InternalException ex) {
                    sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
                    LogUtil.error("Error while resetting the player UUID \"{0}\" from the storage: {1}", playerUuid.toString(), ex.getMessage());
                    return;
                }
            }

            sender.sendMessage(registry.getLanguageService().message("Messages.ResetAllHeadSuccess")
                    .replace(PLAYER_COUNT_PLACEHOLDER, String.valueOf(playersWithHead.size()))
                    .replace("%headName%", headName));
        } else {
            sender.sendMessage(registry.getLanguageService().message("Messages.ResetAllHeadConfirm")
                    .replace(PLAYER_COUNT_PLACEHOLDER, String.valueOf(playersWithHead.size()))
                    .replace("%headName%", headName));
        }

    }

    private void resetAllHeadsForAllPlayers(CommandSender sender, boolean hasConfirm) {
        List<UUID> allPlayers;

        try {
            allPlayers = registry.getStorageService().getAllPlayers();
        } catch (InternalException ex) {
            sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
            LogUtil.error("Error while retrieving all players from the storage: {0}", ex.getMessage());
            return;
        }

        if (allPlayers.isEmpty()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ResetAllNoData"));
            return;
        }

        if (hasConfirm) {
            for (UUID uuid : allPlayers) {
                try {
                    registry.getStorageService().resetPlayer(uuid);

                    Player onlinePlayer = Bukkit.getPlayer(uuid);
                    if (onlinePlayer != null) {
                        registry.getVisibilityService().onProgressReset(onlinePlayer);
                    }
                } catch (InternalException ex) {
                    sender.sendMessage(registry.getLanguageService().message(MESSAGES_STORAGE_ERROR));
                    LogUtil.error("Error while resetting the player UUID \"{0}\" from the storage: {1}", uuid.toString(), ex.getMessage());
                    return;
                }
            }

            sender.sendMessage(registry.getLanguageService().message("Messages.ResetAllSuccess")
                    .replace(PLAYER_COUNT_PLACEHOLDER, String.valueOf(allPlayers.size())));
        } else {
            sender.sendMessage(registry.getLanguageService().message("Messages.ResetAllConfirm")
                    .replace(PLAYER_COUNT_PLACEHOLDER, String.valueOf(allPlayers.size())));
        }

    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            ArrayList<String> options = new ArrayList<>();
            options.add(CONFIRM);
            options.add(HEAD_FLAG);
            return options;
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase(HEAD_FLAG)) {
                ArrayList<String> options = new ArrayList<>();
                options.add(CONFIRM);
                options.addAll(registry.getHeadService().getHeadRawNameOrUuid());
                return new ArrayList<>(options.stream()
                        .filter(s -> s.startsWith(args[2])).toList());
            } else if (args[1].equalsIgnoreCase(CONFIRM)) {
                return new ArrayList<>(Collections.singletonList(HEAD_FLAG));
            }
        } else if (args.length == 4) {
            if (args[1].equalsIgnoreCase(HEAD_FLAG) && args[3].equalsIgnoreCase(CONFIRM)) {
                return new ArrayList<>();
            } else if (args[1].equalsIgnoreCase(HEAD_FLAG) && !args[2].equalsIgnoreCase(CONFIRM)) {
                return new ArrayList<>(Collections.singletonList(CONFIRM));
            } else if (args[2].equalsIgnoreCase(HEAD_FLAG)) {
                return new ArrayList<>(registry.getHeadService().getHeadRawNameOrUuid()
                        .stream().filter(s -> s.startsWith(args[3])).toList());
            }
        } else if (args.length == 5 && args[2].equalsIgnoreCase(HEAD_FLAG)) {
            return new ArrayList<>(Collections.singletonList(CONFIRM));
        }

        return new ArrayList<>();
    }
}
