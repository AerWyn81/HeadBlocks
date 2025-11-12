package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.stream.Collectors;

@HBAnnotations(command = "reset", permission = "headblocks.admin", args = {"player"}, isPlayerCommand = true)
public class Reset extends ResetBase {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        var player = (Player) sender;

        PlayerProfileLight profile;

        try {
            profile = StorageService.getPlayerByName(args[1]);
        } catch (Exception ex) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            LogUtil.error("Error while retrieving player {0} from the storage: {1}", args[1], ex.getMessage());
            return true;
        }

        if (profile == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.PlayerNotFound", args[1]));
            return true;
        }

        var headUuid = resolveHeadFromArgs(player, args, 2);

        if (hasHeadParameter(args, 2) && headUuid == null) {
            return true;
        }

        var targetPlayer = Bukkit.getPlayer(profile.uuid());

        try {
            if (headUuid != null) {
                StorageService.resetPlayerHead(profile.uuid(), headUuid);
                String headName = getHeadDisplayName(headUuid);
                sender.sendMessage(LanguageService.getMessage("Messages.PlayerHeadReset", args[1])
                        .replaceAll("%headName%", headName));

                if (targetPlayer != null) {
                    var packetEventsHook = HeadBlocks.getInstance().getPacketEventsHook();
                    if (packetEventsHook != null && packetEventsHook.isEnabled() && packetEventsHook.getHeadHidingListener() != null) {
                        packetEventsHook.getHeadHidingListener().removeFoundHead(targetPlayer, headUuid);
                    }
                }
            } else {
                StorageService.resetPlayer(profile.uuid());
                sender.sendMessage(LanguageService.getMessage("Messages.PlayerReset", args[1]));

                if (targetPlayer != null) {
                    var packetEventsHook = HeadBlocks.getInstance().getPacketEventsHook();
                    if (packetEventsHook != null && packetEventsHook.isEnabled() && packetEventsHook.getHeadHidingListener() != null) {
                        packetEventsHook.getHeadHidingListener().showAllPreviousHeads(targetPlayer);
                    }
                }
            }
        } catch (InternalException ex) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            LogUtil.error("Error while resetting the player {0} from the storage: {1}", args[1], ex.getMessage());
            return true;
        }

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toCollection(ArrayList::new));
        } else if (args.length == 3) {
            return new ArrayList<>(java.util.Collections.singletonList("--head"));
        } else if (args.length == 4 && args[2].equalsIgnoreCase("--head")) {
            return new ArrayList<>(HeadService.getHeadRawNameOrUuid().stream()
                    .filter(s -> s.startsWith(args[3])).toList());
        }
        return new ArrayList<>();
    }
}
