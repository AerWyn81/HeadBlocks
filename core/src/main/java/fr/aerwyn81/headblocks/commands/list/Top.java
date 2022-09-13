package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.LanguageService;
import fr.aerwyn81.headblocks.utils.ChatPageUtils;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import fr.aerwyn81.headblocks.utils.PlayerUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Map;

@HBAnnotations(command = "top", permission = "headblocks.use")
public class Top implements Cmd {
    private final HeadBlocks main;

    public Top(HeadBlocks main) {
        this.main = main;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        int limit;
        if (args.length == 1) {
            limit = 10;
        } else if (NumberUtils.isDigits(args[args.length - 1])) {
            try {
                limit = NumberUtils.createInteger(args[args.length - 1]);
            } catch (NumberFormatException exception) {
                limit = 10;
            }
            if (limit <= 0) {
                limit = 10;
            }
        } else {
            limit = 10;
        }

        ArrayList<Map.Entry<String, Integer>> top;
        try {
            top = new ArrayList<>(main.getStorageHandler().getTopPlayers(limit).entrySet());
        } catch (InternalException ex) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while retrieving top players from the storage: " + ex.getMessage()));
            return true;
        }

        if (top.size() == 0) {
            sender.sendMessage(LanguageService.getMessage("Messages.TopEmpty"));
            return true;
        }

        ChatPageUtils cpu = new ChatPageUtils(sender)
                .entriesCount(top.size())
                .currentPage(args);

        String message = LanguageService.getMessage("Chat.TopTitle");
        if (sender instanceof Player) {
            TextComponent titleComponent = new TextComponent(message);
            cpu.addTitleLine(titleComponent);
        } else {
            sender.sendMessage(message);
        }

        for (int i = cpu.getFirstPos(); i < cpu.getFirstPos() + cpu.getPageHeight() && i < cpu.getSize(); i++) {
            int pos = i + 1;
            Map.Entry<String, Integer> currentScore = top.get(i);

            message = LanguageService.getMessage("Chat.LineTop")
                    .replaceAll("%pos%", String.valueOf(pos))
                    .replaceAll("%player%", currentScore.getKey())
                    .replaceAll("%count%", String.valueOf(currentScore.getValue()));

            if (sender instanceof Player) {
                TextComponent msg = new TextComponent(message);

                if (PlayerUtils.hasPermission(sender, "headblocks.admin")) {
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hb stats " + currentScore.getKey()));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(LanguageService.getMessage("Chat.Hover.LineTop")).create()));
                }

                cpu.addLine(msg);
            } else {
                sender.sendMessage(MessageUtils.colorize("&6" + message));
            }
        }

        cpu.addPageLine("top");
        cpu.build();
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
