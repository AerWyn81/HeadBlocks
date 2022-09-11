package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.commands.HBCommand;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.ChatPageUtils;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

@HBAnnotations(command = "help", permission = "headblocks.use")
public class Help implements Cmd {
    private final LanguageHandler languageHandler;
    private final ArrayList<HBCommand> registeredCommands;

    public Help(HeadBlocks main) {
        this.languageHandler = main.getLanguageHandler();
        this.registeredCommands = new ArrayList<>();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        ChatPageUtils cpu = new ChatPageUtils(languageHandler, sender)
                .entriesCount(registeredCommands.size())
                .currentPage(args);

        String message = languageHandler.getMessage("Chat.LineTitle");
        if (sender instanceof Player) {
            TextComponent titleComponent = new TextComponent(message);
            cpu.addTitleLine(titleComponent);
        } else {
            sender.sendMessage(message);
        }

        for (int i = cpu.getFirstPos(); i < cpu.getFirstPos() + cpu.getPageHeight() && i < cpu.getSize() ; i++) {
            String command = StringUtils.capitalize(registeredCommands.get(i).getCommand())
                    .replaceAll("all", "All");

            if (!languageHandler.hasMessage("Help." + command)) {
                sender.sendMessage(MessageUtils.colorize("&6/hb " + registeredCommands.get(i).getCommand() + " &8: &c&oNo help message found. Please report to developer!"));
            } else {
                message = languageHandler.getMessage("Help." + command);
                if (sender instanceof Player) {
                    cpu.addLine(new TextComponent(message));
                } else {
                    sender.sendMessage(message);
                }
            }
        }

        cpu.addPageLine("help");
        cpu.build();
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    public void addCommand(HBCommand command) {
        registeredCommands.add(command);
    }
}
