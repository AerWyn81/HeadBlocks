package fr.aerwyn81.headblocks.commands.newCommands;

import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.ExportSQLHelper;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.help.CommandHelp;

import java.util.ArrayList;

@Command({"hb", "headblock", "heablocks"})
@CommandPermission("headblocks.admin")
@Usage("/headblocks <give|list|remove|reset|stats|top|...> <args...>")
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
    @Usage("(player)")
    public void give(BukkitCommandActor actor, @Default("me") Player player) {
        ArrayList<HBHead> hbHeads = HeadService.getHeads();

        if (!PlayerUtils.hasEnoughInventorySpace(player, hbHeads.size())) {
            actor.reply(LanguageService.getMessage("Messages.NotEnoughInventorySpace"));
            return;
        }

        int headGiven = 0;
        for (HBHead head : hbHeads) {
            if (head instanceof HBHeadHDB) {
                HBHeadHDB headHDB = (HBHeadHDB) head;

                if (!headHDB.isLoaded()) {
                    actor.reply(LanguageService.getMessage("Messages.HeadNotYetLoaded")
                            .replaceAll("%id%", String.valueOf(headHDB.getId())));
                    continue;
                }
            }

            player.getInventory().addItem(head.getItemStack());
            headGiven++;
        }

        if (headGiven != 0) {
            actor.reply(LanguageService.getMessage("Messages.HeadGiven")
                    .replaceAll("%headNumber%", String.valueOf(headGiven)));
        }
    }
}
