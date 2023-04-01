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
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Command({"hb", "headblock", "heablocks"})
@CommandPermission("headblocks.admin")
@Usage("/headblocks <give|list|remove|reset|stats|top|...> <args...>")
public class AdminCommands {

    @Subcommand({"exportDatabase"})
    @CommandPermission("headblocks.admin.exportDatabase")
    @Description("Export command")
    @Usage("<SQLite|MySQL>")
    public void export(BukkitCommandActor actor, @Named("database_type") EnumTypeDatabase typeDatabase) {
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
    @Usage("(player) <number>")
    public void give(BukkitCommandActor actor, @Default("me") Player player, @Named("number") @Optional Integer part) {
        var hbHeads = new ArrayList<>(HeadService.getHeads());

        var headNotLoaded = hbHeads.stream()
                .filter(HBHeadHDB.class::isInstance)
                .map(h -> (HBHeadHDB) h)
                .filter(HBHeadHDB::isLoaded)
                .collect(Collectors.toList());

        if (headNotLoaded.size() > 0) {
            actor.reply(LanguageService.getMessage("Messages.HeadNotYetLoaded")
                    .replaceAll("%ids%", headNotLoaded.stream().map(HBHeadHDB::getId).collect(Collectors.joining(", "))));

            // At least one HeadDatabase head not loaded, removing all others to give
            hbHeads.removeIf(hbHead -> hbHead instanceof HBHeadHDB);
        }

        var headsToItemstack = hbHeads.stream().map(HBHead::getItemStack).toArray(ItemStack[]::new);

        var freeSlots = PlayerUtils.getFreeSlots(player, headsToItemstack);
        if (freeSlots == 0) {
            actor.reply(LanguageService.getMessage("Messages.InventoryFull"));
            return;
        }

        if (freeSlots >= hbHeads.size()) {
            player.getInventory().addItem(headsToItemstack);
            actor.reply(LanguageService.getMessage("Messages.HeadGiven")
                    .replaceAll("%headNumber%", String.valueOf(hbHeads.size())));

            return;
        }

        if (part == -1) {
            actor.reply(LanguageService.getMessage("Messages.GiveTooManyHeadInventory"));
            return;
        }

        if (part <= 0)
            part = 1;
        else if (part > hbHeads.size())
            part = hbHeads.size() - 1;

        var headSelected  = hbHeads.get(part);
        if (headSelected != null) {
            player.getInventory().addItem(headSelected.getItemStack());
            actor.reply(LanguageService.getMessage("Messages.HeadGiven")
                    .replaceAll("%headNumber%", "1"));
        }
    }
}
