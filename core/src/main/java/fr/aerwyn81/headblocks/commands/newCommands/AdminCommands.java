package fr.aerwyn81.headblocks.commands.newCommands;

import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.internal.ExportSQLHelper;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.EntitySelector;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.help.CommandHelp;

@Command({"hb"})
@CommandPermission("headblocks.admin")
public class AdminCommands {

    @Subcommand({"help"})
    @Description("HeadBlocks admin commands list")
    public void help(BukkitCommandActor actor, CommandHelp<String> helpEntries, @Default("1") int page) {
        for (String entry : helpEntries.paginate(page, 7))
            actor.reply(entry);
    }

    @Subcommand({"exportDatabase"})
    @CommandPermission("headblocks.admin.exportDatabase")
    @Description("Export command")
    @Usage("<SQLite|MySQL>")
    public void export(BukkitCommandActor actor, EnumTypeDatabase typeDatabase) {
        actor.reply(MessageUtils.colorize(LanguageService.getMessage("Messages.ExportInProgress")));

        ExportSQLHelper.generateFile(typeDatabase)
                .exceptionally((ex) -> {
                    actor.reply(LanguageService.getMessage("Messages.ExportError") + ex.getMessage());
                    return null;
                })
                .thenAcceptAsync(fileName -> actor.reply(LanguageService.getMessage("Messages.ExportSuccess")
                        .replaceAll("%fileName%", fileName)));
    }

    @Subcommand("give")
    @CommandPermission("headblocks.admin.give")
    @Description("Give command")
    @Usage("<player> <*|n>")
    @AutoComplete("@giveCommand *")
    public void give(BukkitCommandActor actor, @Default("self") EntitySelector<Player> players, String t) {

    }
}
