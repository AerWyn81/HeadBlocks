package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.utils.bukkit.HeadTargeting;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class ResetBase implements Cmd {
    protected final ServiceRegistry registry;

    protected ResetBase(ServiceRegistry registry) {
        this.registry = registry;
    }

    protected UUID resolveHeadFromArgs(CommandSender sender, String[] args, int startIndex) {
        for (int i = startIndex; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--head")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    final var headIdentifier = args[i + 1];

                    var head = registry.getHeadService().resolveHeadIdentifier(headIdentifier);
                    if (head == null) {
                        sender.sendMessage(registry.getLanguageService().message("Messages.HeadNameNotFound")
                                .replace("%headName%", headIdentifier));
                        return null;
                    }

                    return head.getUuid();
                } else {
                    return resolveTargetedHead(sender);
                }
            }
        }

        return null;
    }

    protected boolean hasHeadParameter(String[] args, int startIndex) {
        for (int i = startIndex; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--head")) {
                return true;
            }
        }
        return false;
    }

    private UUID resolveTargetedHead(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.TargetHeadPlayerOnly"));
            return null;
        }

        var headLocation = HeadTargeting.lookedAt(player, registry, 100);

        if (headLocation == null) {
            player.sendMessage(registry.getLanguageService().message("Messages.NoTargetHeadBlock"));
            return null;
        }

        return headLocation.getUuid();
    }

    protected String getHeadDisplayName(UUID headUuid) {
        HeadLocation headLocation = registry.getHeadService().getHeadByUUID(headUuid);
        return headLocation != null ? headLocation.getNameOrUnnamed(registry.getLanguageService().message("Gui.Unnamed")) : headUuid.toString();
    }
}
