package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

@HBAnnotations(command = "remove", permission = "headblocks.admin")
public class Remove implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (args.length > 2) {
            sender.sendMessage(LanguageService.getMessage("Messages.ErrorCommand"));
            return true;
        }

        HeadLocation head;

        if (args.length == 1) {
            if (sender instanceof ConsoleCommandSender) {
                sender.sendMessage(LanguageService.getMessage("Messages.PlayerOnly"));
                return true;
            }

            Player player = (Player) sender;

            var targetHead = HeadService.getHeadAt(player.getTargetBlock(null, 25).getLocation());

            if (targetHead == null) {
                player.sendMessage(LanguageService.getMessage("Messages.TargetBlockNotHead"));
                return true;
            }

            head = targetHead;
        } else {
            try {
                head = HeadService.getHeadByUUID(UUID.fromString(args[1]));
            } catch (Exception ex) {
                head = HeadService.getHeadByName(args[1]);
            }

            if (head == null) {
                sender.sendMessage(LanguageService.getMessage("Messages.RemoveLocationError"));
                return true;
            }
        }

        Location loc = head.getLocation();

        try {
            HeadService.removeHeadLocation(head, ConfigService.shouldResetPlayerData());
        } catch (InternalException ex) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while removing the head (" + head.getNameOrUuid() + " at " + loc.toString() + ") from the storage: " + ex.getMessage()));
            return true;
        }

        sender.sendMessage(LocationUtils.parseLocationPlaceholders(LanguageService.getMessage("Messages.HeadRemoved"), loc));
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? HeadService.getChargedHeadLocations().stream()
                .map(HeadLocation::getRawNameOrUuid)
                .collect(Collectors.toCollection(ArrayList::new)) : new ArrayList<>();
    }
}
