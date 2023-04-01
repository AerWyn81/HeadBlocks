package fr.aerwyn81.headblocks.commands.newCommands;

import fr.aerwyn81.headblocks.services.GuiService;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.help.CommandHelp;

@Command({"hb", "headblock", "heablocks"})
@CommandPermission("headblocks.use")
@Usage("/headblocks <help|list|me|stats|top|...> <args...>")
public class PlayerCommands {

    @Subcommand({"help"})
    @Description("HeadBlocks commands list")
    public void help(BukkitCommandActor actor, CommandHelp<String> helpEntries, @Default("1") int page) {
        for (String entry : helpEntries.paginate(page, 7))
            actor.reply(entry);
    }

    @Subcommand("list")
    @CommandPermission("headblocks.use.list")
    @Description("List command")
    @Usage("")
    public void list(BukkitCommandActor actor) {
        var player = actor.requirePlayer();

        internalShowTracksGui(player);
    }

    private void internalShowTracksGui(Player player) {
        GuiService.showTracksGui(player,
                inventoryCloseEvent -> { },
                (inventoryClickEvent, hbTrack) -> GuiService.showContentTracksGui(player, hbTrack, e -> internalShowTracksGui(player)),
                inventoryClickEvent -> GuiService.closeInventory(inventoryClickEvent.getWhoClicked()),
                false, null, null);
    }
}
