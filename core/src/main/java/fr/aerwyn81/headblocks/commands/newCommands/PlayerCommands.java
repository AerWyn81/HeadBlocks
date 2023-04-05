package fr.aerwyn81.headblocks.commands.newCommands;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.GuiService;
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
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.help.CommandHelp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Command({"hb", "headblock", "heablocks"})
@CommandPermission(value = "headblocks.commands", defaultAccess = PermissionDefault.TRUE)
public class PlayerCommands {

    @Subcommand("help")
    @CommandPermission("headblocks.commands.player.help")
    @Description("HeadBlocks commands list")
    public void help(BukkitCommandActor actor, CommandHelp<String> helpEntries, @Default("1") int page) {
        for (String entry : helpEntries.paginate(page, 7))
            actor.reply(entry);
    }

    @Subcommand("version")
    @CommandPermission("headblocks.commands.player.version")
    @Description("Version command")
    public void version(BukkitCommandActor actor) {
        actor.reply(LanguageService.getMessage("Messages.Version")
                .replaceAll("%version%", HeadBlocks.getInstance().getDescription().getVersion()));
    }

    @Subcommand("me")
    @CommandPermission(value = "headblocks.commands.player.me", defaultAccess = PermissionDefault.TRUE)
    @Description("Me command")
    public void me(BukkitCommandActor actor) {
        var player = actor.requirePlayer();

        GuiService.showTracksGuiWithBack(player, null, track -> {
            StringBuilder lore = new StringBuilder().append("\n");

            if (track.getDescription().size() > 0) {
                for (var line : track.getColorizedDescription()) {
                    lore.append(line).append("\n");
                }

                lore.append("\n");
            }

            //TODO
            lore.append(MessageUtils.colorize("&e&lProgression:"))
                    .append("\n")
                    .append(MessageUtils.colorize("&7  [ "))
                    .append(MessageUtils.createProgressBar(5, 100, 25,
                            ConfigService.getProgressBarSymbol(),
                            ConfigService.getProgressBarCompletedColor(),
                            ConfigService.getProgressBarNotCompletedColor())).append(MessageUtils.colorize(" &7]"))
                    .append("\n")
                    .append("\n")
                    .append(LanguageService.getMessage("Gui.ClickShowContent"));
            return new ArrayList<>(List.of(lore.toString().split("\n")));
        });
    }


    @Subcommand("top")
    @CommandPermission(value = "headblocks.commands.player.top", defaultAccess = PermissionDefault.TRUE)
    @Description("Top command")
    public void top(BukkitCommandActor actor, @Default("1") @Named("page") int page) {
        var sender = actor.getSender();

        ChatPageUtils cpu = new ChatPageUtils(sender).currentPage(page);

        ArrayList<Map.Entry<String, Integer>> top = new ArrayList<>();
        try {
            top = new ArrayList<>(StorageService.getTopPlayers().entrySet());
        } catch (InternalException ex) {
            actor.reply(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while retrieving top players from the storage: " + ex.getMessage()));
        }

        if (top.size() == 0) {
            actor.reply(LanguageService.getMessage("Messages.TopEmpty"));
            return;
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
            Map.Entry<String, Integer> currentScore = top.get(i);

            message = LanguageService.getMessage("Chat.LineTop", currentScore.getKey())
                    .replaceAll("%pos%", String.valueOf(pos))
                    .replaceAll("%count%", String.valueOf(currentScore.getValue()));

            if (sender instanceof Player) {
                TextComponent msg = new TextComponent(message);

                if (PlayerUtils.hasPermission(sender, "headblocks.admin")) {
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hb stats " + currentScore.getKey()));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(LanguageService.getMessage("Chat.Hover.LineTop"))));
                }

                cpu.addLine(msg);
            } else {
                sender.sendMessage(MessageUtils.colorize("&6" + message));
            }
        }

        cpu.addPageLine("top");
        cpu.build();
    }
}
