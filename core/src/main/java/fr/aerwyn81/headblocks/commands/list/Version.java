package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.LanguageService;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

@HBAnnotations(command = "version", permission = "headblocks.use")
public class Version implements Cmd {
    private final HeadBlocks main;

    public Version(HeadBlocks main) {
        this.main = main;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        sender.sendMessage(LanguageService.getMessage("Messages.Version")
                .replaceAll("%version%", main.getDescription().getVersion()));

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
