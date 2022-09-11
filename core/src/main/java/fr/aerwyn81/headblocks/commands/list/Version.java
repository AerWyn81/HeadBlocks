package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

@HBAnnotations(command = "version", permission = "headblocks.use")
public class Version implements Cmd {
    private final HeadBlocks main;
    private final LanguageHandler languageHandler;

    public Version(HeadBlocks main) {
        this.main = main;
        this.languageHandler = main.getLanguageHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        sender.sendMessage(languageHandler.getMessage("Messages.Version")
                .replaceAll("%version%", main.getDescription().getVersion()));

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
