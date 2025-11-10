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
import java.util.stream.Collectors;

@HBAnnotations(command = "reset", permission = "headblocks.admin", args = {"player"}, isPlayerCommand = true)
public class Reset extends ResetBase {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        var player = (Player) sender;

        var headUuid = resolveHeadFromArgs(player, args, 2);

        if (hasHeadParameter(args, 2) && headUuid == null) {
            return true;
        }

        try {
            if (headUuid != null) {
                StorageService.resetPlayerHead(player.getUniqueId(), headUuid);
                String headName = getHeadDisplayName(headUuid);
                sender.sendMessage(LanguageService.getMessage("Messages.PlayerHeadReset", args[1])
                        .replaceAll("%headName%", headName));

                var packetEventsHook = HeadBlocks.getInstance().getPacketEventsHook();
                if (packetEventsHook != null && packetEventsHook.isEnabled() && packetEventsHook.getHeadHidingListener() != null) {
                    packetEventsHook.getHeadHidingListener().removeFoundHead(player, headUuid);
                }
            } else {
                StorageService.resetPlayer(player.getUniqueId());
                sender.sendMessage(LanguageService.getMessage("Messages.PlayerReset", args[1]));

                var packetEventsHook = HeadBlocks.getInstance().getPacketEventsHook();
                if (packetEventsHook != null && packetEventsHook.isEnabled() && packetEventsHook.getHeadHidingListener() != null) {
                    packetEventsHook.getHeadHidingListener().invalidatePlayerCache(player.getUniqueId());
                }
            }
        } catch (InternalException ex) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while resetting the player " + args[1] + " from the storage: " + ex.getMessage()));
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
            return getHeadCompletions();
        }
        return new ArrayList<>();
    }
}
