package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.chat.ChatPageUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Map;

@HBAnnotations(command = "top", permission = "headblocks.commands.top", alias = "t")
public class Top implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        ChatPageUtils cpu = new ChatPageUtils(sender).currentPage(args);

        ArrayList<Map.Entry<PlayerProfileLight, Integer>> top;
        try {
            top = new ArrayList<>(StorageService.getTopPlayers().entrySet());
        } catch (InternalException ex) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while retrieving top players from the storage: " + ex.getMessage()));
            return true;
        }

        if (top.isEmpty()) {
            sender.sendMessage(LanguageService.getMessage("Messages.TopEmpty"));
            return true;
        }

        cpu.entriesCount(top.size());

        String message = LanguageService.getMessage("Chat.TopTitle");
        if (sender instanceof Player) {
            TextComponent titleComponent = new TextComponent(message);
            cpu.addTitleLine(titleComponent);
        } else {
            sender.sendMessage(message);
        }

        for (int i = cpu.getFirstPos(); i < cpu.getFirstPos() + cpu.getPageHeight() && i < cpu.getSize(); i++) {
            int pos = i + 1;
            Map.Entry<PlayerProfileLight, Integer> currentScore = top.get(i);

            message = LanguageService.getMessage("Chat.LineTop", currentScore.getKey().name())
                    .replaceAll("%pos%", String.valueOf(pos))
                    .replaceAll("%count%", String.valueOf(currentScore.getValue()));

            if (sender instanceof Player) {
                TextComponent msg = new TextComponent(message);

                if (PlayerUtils.hasPermission(sender, "headblocks.admin")) {
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks stats " + currentScore.getKey().name()));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(LanguageService.getMessage("Chat.Hover.LineTop"))));
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
        return new ArrayList<>();
    }
}
