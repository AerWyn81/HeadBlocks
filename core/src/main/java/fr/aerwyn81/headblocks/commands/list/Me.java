package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.PlaceholdersService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@HBAnnotations(command = "me", permission = "headblocks.use", isPlayerCommand = true)
public class Me implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        int max = HeadService.getChargedHeadLocations().size();
        if (max == 0) {
            player.sendMessage(LanguageService.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        List<String> messages = LanguageService.getMessages("Messages.MeCommand");
        if (!messages.isEmpty()) {
            LanguageService.getMessages("Messages.MeCommand").forEach(msg ->
                    player.sendMessage(PlaceholdersService.parse(player.getName(), player.getUniqueId(), msg)));
        }

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
