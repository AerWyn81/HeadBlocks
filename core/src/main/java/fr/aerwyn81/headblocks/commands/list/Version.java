package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.services.LanguageService;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

@HBAnnotations(command = "version", permission = "headblocks.admin")
public class Version implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        sender.sendMessage(LanguageService.getMessage("Messages.Version")
                .replaceAll("%version%", HeadBlocks.getInstance().getDescription().getVersion()));

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
