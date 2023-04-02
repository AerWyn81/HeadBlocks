package fr.aerwyn81.headblocks.commands.newCommands;

import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.GuiService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.permissions.PermissionDefault;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.help.CommandHelp;

import java.util.ArrayList;
import java.util.List;

@Command({"hb", "headblock", "heablocks"})
@CommandPermission(value = "headblocks.use", defaultAccess = PermissionDefault.TRUE)
public class PlayerCommands {

    @Subcommand({"help"})
    @Description("HeadBlocks commands list")
    public void help(BukkitCommandActor actor, CommandHelp<String> helpEntries, @Default("1") int page) {
        for (String entry : helpEntries.paginate(page, 7))
            actor.reply(entry);
    }

    @Subcommand("me")
    @CommandPermission(value = "headblocks.use.me", defaultAccess = PermissionDefault.TRUE)
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
}
