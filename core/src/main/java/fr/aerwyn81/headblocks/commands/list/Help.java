package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.commands.HBCommand;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

@HBAnnotations(command = "help")
public class Help implements Cmd {
    private final LanguageHandler languageHandler;
    private final ArrayList<HBCommand> registeredCommands;

    public Help(HeadBlocks main) {
        this.languageHandler = main.getLanguageHandler();
        this.registeredCommands = new ArrayList<>();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        int pageNumber;
        int pageHeight = sender instanceof Player ? 8 : Integer.MAX_VALUE;
        int totalPage = (registeredCommands.size() / pageHeight) + (registeredCommands.size() % pageHeight == 0 ? 0 : 1);

        if (args.length == 0) {
            pageNumber = 1;
        } else if (NumberUtils.isDigits(args[args.length - 1])) {
            try {
                pageNumber = NumberUtils.createInteger(args[args.length - 1]);
                pageNumber = (totalPage < pageNumber) ? totalPage : Math.max(pageNumber, 1);
            } catch (NumberFormatException exception) {
                pageNumber = 1;
            }
            if (pageNumber <= 0) {
                pageNumber = 1;
            }
        } else {
            pageNumber = 1;
        }

        int firstPos = ((pageNumber - 1) * pageHeight);

        sender.sendMessage(languageHandler.getMessage("Help.LineTop"));

        for (int i = firstPos; i < firstPos + pageHeight && i < registeredCommands.size() ; i++) {
            String command = StringUtils.capitalize(registeredCommands.get(i).getCommand())
                    .replaceAll("all", "All");

            if (!languageHandler.hasMessage("Help." + command)) {
                sender.sendMessage(FormatUtils.translate("&6/hb " + registeredCommands.get(i).getCommand() + " &8: &c&oNo help message found. Please report to developer!"));
                continue;
            }

            sender.sendMessage(languageHandler.getMessage("Help." + command));
        }

        if (registeredCommands.size() > pageHeight) {
            TextComponent c1 = new TextComponent(FormatUtils.translate(languageHandler.getMessage("Chat.PreviousPage")));
            c1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks help " + (pageNumber - 1)));
            c1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.PreviousPage")).create()));

            TextComponent c2 = new TextComponent(FormatUtils.translate(languageHandler.getMessage("Chat.NextPage")));
            c2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks help " + (pageNumber + 1)));
            c2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.NextPage")).create()));

            ((Player) sender).spigot().sendMessage(c1,
                    new TextComponent(languageHandler.getMessage("Chat.PageFooter").replaceAll("%pageNumber%", String.valueOf(pageNumber)).replaceAll("%totalPage%", String.valueOf(totalPage))),
                    c2);
        }

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
