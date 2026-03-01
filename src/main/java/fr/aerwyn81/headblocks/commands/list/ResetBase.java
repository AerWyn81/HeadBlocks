package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.data.HeadLocation;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class ResetBase implements Cmd {
    protected final ServiceRegistry registry;

    protected ResetBase(ServiceRegistry registry) {
        this.registry = registry;
    }

    protected UUID resolveHeadFromArgs(Player player, String[] args, int startIndex) {
        for (int i = startIndex; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--head")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    final var headIdentifier = args[i + 1];

                    var head = registry.getHeadService().resolveHeadIdentifier(headIdentifier);
                    if (head == null) {
                        player.sendMessage(registry.getLanguageService().message("Messages.HeadNameNotFound")
                                .replaceAll("%headName%", headIdentifier));
                        return null;
                    }

                    return head.getUuid();
                } else {
                    return resolveTargetedHead(player);
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

    private UUID resolveTargetedHead(Player player) {
        var targetLoc = player.getTargetBlock(null, 100).getLocation();
        var headLocation = registry.getHeadService().getHeadAt(targetLoc);

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
