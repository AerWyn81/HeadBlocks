package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.handlers.PlaceholdersHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@HBAnnotations(command = "me", permission = "headblocks.use", isPlayerCommand = true)
public class Me implements Cmd {
    private final HeadBlocks main;
    private final LanguageHandler languageHandler;

    public Me(HeadBlocks main) {
        this.main = main;
        this.languageHandler = main.getLanguageHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        int max = main.getHeadHandler().getChargedHeadLocations().size();
        if (max == 0) {
            player.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        List<String> messages = languageHandler.getMessages("Messages.MeCommand");
        if (messages.size() != 0) {
            languageHandler.getMessages("Messages.MeCommand").forEach(msg ->
                    player.sendMessage(PlaceholdersHandler.parse(player, msg)));
        }

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
