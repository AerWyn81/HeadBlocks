package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.stream.Stream;

@HBAnnotations(command = "options", permission = "headblocks.admin", isPlayerCommand = true, alias = "o")
public class Options implements Cmd {
    private final ServiceRegistry registry;

    public Options(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (args.length > 1) {
            switch (args[1]) {
                case "order":
                    registry.getGuiService().getOrderManager().openOrderGui((Player) sender);
                    return true;
                case "hint":
                    registry.getGuiService().getHintManager().openHintGui((Player) sender);
                    return true;
                case "rewards":
                    var argHead = args.length > 2 ? args[2] : null;

                    HeadLocation head = null;
                    if (argHead != null) {
                        head = registry.getHeadService().resolveHeadIdentifier(argHead);
                        if (head == null) {
                            sender.sendMessage(registry.getLanguageService().message("Messages.HeadNameNotFound")
                                    .replace("%headName%", argHead));
                            return true;
                        }
                    }

                    registry.getGuiService().getRewardsManager().openRewardsSelectionGui((Player) sender, head);
                    return true;
            }
        }

        registry.getGuiService().openOptionsGui((Player) sender);
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 2 -> new ArrayList<>(Stream.of("hint", "order", "rewards")
                    .filter(s -> s.startsWith(args[1])).toList());
            case 3 -> new ArrayList<>(registry.getHeadService().getHeadRawNameOrUuid()
                    .stream().filter(s -> s.startsWith(args[2])).toList());
            default -> new ArrayList<>();
        };
    }
}
