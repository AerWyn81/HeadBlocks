package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.HeadBlocksAPI;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.placeholders.InternalPlaceholders;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@HBAnnotations(command = "me", permission = "headblocks.use", isPlayerCommand = true)
public class Me implements Cmd {
    private final ConfigHandler configHandler;
    private final LanguageHandler languageHandler;
    private final HeadBlocksAPI headBlocksAPI;

    public Me(HeadBlocks main) {
        this.configHandler = main.getConfigHandler();
        this.languageHandler = main.getLanguageHandler();
        this.headBlocksAPI = main.getHeadBlocksAPI();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        int max = headBlocksAPI.getTotalHeadSpawnCount();
        if (max == 0) {
            player.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        int current = headBlocksAPI.getPlayerHeads(player.getUniqueId()).size();

        int bars = configHandler.getProgressBarBars();
        String symbol = configHandler.getProgressBarSymbol();
        String completedColor = configHandler.getProgressBarCompletedColor();
        String notCompletedColor = configHandler.getProgressBarNotCompletedColor();

        String progressBar = FormatUtils.createProgressBar(current, max, bars, symbol, completedColor, notCompletedColor);

        List<String> messages = languageHandler.getMessages("Messages.MeCommand");
        if (messages.size() != 0) {
            languageHandler.getMessages("Messages.MeCommand").forEach(msg -> player.sendMessage(InternalPlaceholders
                    .parse(player, msg.replaceAll("%progress%", progressBar))));
        }

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
