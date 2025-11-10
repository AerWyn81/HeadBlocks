package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;

@HBAnnotations(command = "rename", permission = "headblocks.admin", isPlayerCommand = true, alias = "r", args = {"name"})
public class RenameHead implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        var player = (Player) sender;

        var targetLoc = player.getTargetBlock(null, 100).getLocation();

        var headLocation = HeadService.getHeadAt(targetLoc);

        if (headLocation == null) {
            player.sendMessage(LanguageService.getMessage("Messages.NoTargetHeadBlock"));
            return true;
        }

        args = Arrays.copyOfRange(args, 1, args.length);

        var name = String.join(" ", args);
        if (name.isEmpty()) {
            player.sendMessage(LanguageService.getMessage("Messages.NameCannotBeEmpty"));
            return true;
        }

        headLocation.setName(name);
        HeadService.saveHeadInConfig(headLocation);

        player.sendMessage(LanguageService.getMessage("Messages.HeadRenamed")
                .replaceAll("%name%", MessageUtils.colorize(name)));
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
