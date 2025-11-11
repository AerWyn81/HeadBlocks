package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.GuiService;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.stream.Stream;

@HBAnnotations(command = "options", permission = "headblocks.admin", isPlayerCommand = true, alias = "o")
public class Options implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (args.length > 1) {
            switch (args[1]) {
                case "order":
                    GuiService.getOrderManager().openOrderGui((Player) sender);
                    return true;
                case "counter":
                    GuiService.getClickCounterManager().openClickCounterGui((Player) sender);
                    return true;
                case "hint":
                    GuiService.getHintManager().openHintGui((Player) sender);
                    return true;
                case "rewards":
                    var argHead = args.length > 2 ? args[2] : null;

                    HeadLocation head = null;
                    if (argHead != null) {
                        head = HeadService.resolveHeadIdentifier(argHead);
                        if (head == null) {
                            sender.sendMessage(LanguageService.getMessage("Messages.HeadNameNotFound")
                                    .replaceAll("%headName%", argHead));
                            return true;
                        }
                    }

                    GuiService.getRewardsManager().openRewardsSelectionGui((Player) sender, head);
                    return true;
            }
        }

        GuiService.openOptionsGui((Player) sender);
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 2 -> new ArrayList<>(Stream.of("counter", "hint", "order", "rewards")
                    .filter(s -> s.startsWith(args[1])).toList());
            case 3 -> new ArrayList<>(HeadService.getHeadRawNameOrUuid()
                    .stream().filter(s -> s.startsWith(args[2])).toList());
            default -> new ArrayList<>();
        };
    }
}