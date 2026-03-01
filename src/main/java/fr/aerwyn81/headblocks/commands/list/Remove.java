package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

@HBAnnotations(command = "remove", permission = "headblocks.admin")
public class Remove implements Cmd {
    private final ServiceRegistry registry;

    public Remove(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (args.length > 2) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ErrorCommand"));
            return true;
        }

        HeadLocation head;

        if (args.length == 1) {
            if (sender instanceof ConsoleCommandSender) {
                sender.sendMessage(registry.getLanguageService().message("Messages.PlayerOnly"));
                return true;
            }

            Player player = (Player) sender;

            var targetHead = registry.getHeadService().getHeadAt(player.getTargetBlock(null, 25).getLocation());

            if (targetHead == null) {
                player.sendMessage(registry.getLanguageService().message("Messages.TargetBlockNotHead"));
                return true;
            }

            head = targetHead;
        } else {
            try {
                head = registry.getHeadService().getHeadByUUID(UUID.fromString(args[1]));
            } catch (Exception ex) {
                head = registry.getHeadService().getHeadByName(args[1]);
            }

            if (head == null) {
                sender.sendMessage(registry.getLanguageService().message("Messages.RemoveLocationError"));
                return true;
            }
        }

        Location loc = head.getLocation();

        try {
            registry.getHeadService().removeHeadLocation(head, registry.getConfigService().resetPlayerData());
        } catch (InternalException ex) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            LogUtil.error("Error while removing the head \"{0}\" at {1} from storage: {2}", head.getNameOrUuid(), loc.toString(), ex.getMessage());
            return true;
        }

        sender.sendMessage(LocationUtils.parseLocationPlaceholders(registry.getLanguageService().message("Messages.HeadRemoved"), loc));
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? new ArrayList<>(registry.getHeadService().getHeadRawNameOrUuid()
                .stream().filter(s -> s.startsWith(args[1])).toList()) : new ArrayList<>();
    }
}
