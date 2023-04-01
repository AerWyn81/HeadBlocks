package fr.aerwyn81.headblocks.commands.newCommands;

import fr.aerwyn81.headblocks.data.head.HBTrack;
import fr.aerwyn81.headblocks.services.GuiService;
import fr.aerwyn81.headblocks.services.LanguageService;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.help.CommandHelp;

import java.util.ArrayList;
import java.util.List;

@Command({"hb", "headblock", "heablocks"})
@CommandPermission("headblocks.use")
@Usage("/headblocks <help|tracks|me|stats|top|...> <args...>")
public class PlayerCommands {

    @Subcommand({"help"})
    @Description("HeadBlocks commands list")
    public void help(BukkitCommandActor actor, CommandHelp<String> helpEntries, @Default("1") int page) {
        for (String entry : helpEntries.paginate(page, 7))
            actor.reply(entry);
    }

    @Subcommand({"tracks"})
    @CommandPermission("headblocks.use.list")
    @Description("List command")
    @Usage("(track_name)")
    public void list(BukkitCommandActor actor, @Optional HBTrack track) {
        var player = actor.requirePlayer();

        internalShowTracksGui(player, track);
    }

    private void internalShowTracksGui(Player player, HBTrack chosenTrack) {
        if (chosenTrack == null) {
            GuiService.showTracksGui(player,
                    inventoryCloseEvent -> { },
                    (inventoryClickEvent, hbTrack) -> GuiService.showContentTracksGui(player, hbTrack, e -> internalShowTracksGui(player, null)),
                    inventoryClickEvent -> GuiService.closeInventory(inventoryClickEvent.getWhoClicked()),
                    track -> {
                        StringBuilder lore = new StringBuilder();

                        var message = LanguageService.getMessage("Gui.TrackItemHeadCount")
                                .replaceAll("%headCount%", String.valueOf(track.getHeadCount()));
                        if (message.trim().length() > 0) {
                            lore.append(message).append("\n");
                        }

                        lore.append("\n");

                        if (track.getDescription().size() > 0) {
                            for (var line : track.getColorizedDescription()) {
                                lore.append(line).append("\n");
                            }

                            lore.append("\n");
                        }

                        lore.append(LanguageService.getMessage("Gui.ClickShowContent"));
                        return new ArrayList<>(List.of(lore.toString().split("\n")));
                    }, false,null, null);

            return;
        }

        GuiService.showContentTracksGui(player, chosenTrack, e -> internalShowTracksGui(player, null));
    }

    @Subcommand("me")
    @CommandPermission("headblocks.use.me")
    @Description("Me command")
    public void me(BukkitCommandActor actor) {
        var player = actor.requirePlayer();


    }
}
