package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.HeadBlocksAPI;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.ChatPageUtils;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.UUID;

@HBAnnotations(command = "top", permission = "headblocks.admin")
public class Top implements Cmd {
    private final LanguageHandler languageHandler;
    private final HeadBlocksAPI headBlocksAPI;

    public Top(HeadBlocks main) {
        this.languageHandler = main.getLanguageHandler();
        this.headBlocksAPI = main.getHeadBlocksAPI();
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

        ArrayList<Pair<UUID, Integer>> top = headBlocksAPI.getTopPlayers(limit);

        if (top.size() == 0) {
            sender.sendMessage(languageHandler.getMessage("Messages.TopEmpty"));
            return true;
        }

        ChatPageUtils cpu = new ChatPageUtils(languageHandler, sender)
                .entriesCount(top.size())
                .currentPage(args);

        String message = languageHandler.getMessage("Chat.TopTitle");
        if (sender instanceof Player) {
            TextComponent titleComponent = new TextComponent(message);
            cpu.addTitleLine(titleComponent);
        } else {
            sender.sendMessage(message);
        }

        for (int i = cpu.getFirstPos(); i < cpu.getFirstPos() + cpu.getPageHeight() && i < cpu.getSize(); i++) {
            int pos = i + 1;
            Pair<UUID, Integer> currentScore = top.get(i);
            Player player = Bukkit.getOfflinePlayer(currentScore.getValue0()).getPlayer();

            message = languageHandler.getMessage("Chat.LineTop")
                    .replaceAll("%pos%", String.valueOf(pos))
                    .replaceAll("%player%", player == null ? FormatUtils.translate("&cUnknownPlayer") : player.getName())
                    .replaceAll("%count%", String.valueOf(currentScore.getValue1()));

            if (sender instanceof Player) {
                TextComponent msg = new TextComponent(message);
                if (player != null) {
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hb stats " + player.getName()));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.LineTop")).create()));
                }

                cpu.addLine(msg);
            } else {
                sender.sendMessage(FormatUtils.translate("&6" + message));
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
