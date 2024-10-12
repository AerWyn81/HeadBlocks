package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@HBAnnotations(command = "debug", permission = "headblocks.debug", isPlayerCommand = true, isVisible = false, args = { "texture" })
public class Debug implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        var blockView = ((Player)sender).getTargetBlock(null,50);
        var blockLocation = blockView.getLocation();
        var block = blockLocation.getBlock();

        var tempBlock = block.getLocation().clone().add(0, 1 ,0).getBlock();
        if (!tempBlock.isEmpty() && !HeadUtils.isPlayerHead(block))
        {
            sender.sendMessage("Block at " + blockLocation.toVector() + " is not empty: " + block.getType());
            return true;
        }

        if (!HeadUtils.isPlayerHead(blockView)) {
            sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cBlock is not a player head!"));
            return true;
        }

        if (args[1].equals("texture")) {
            if (args.length < 3 || args[2].isEmpty())
            {
                sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cTexture cannot be empty!"));
                return true;
            }

            var headLoc = HeadService.getHeadAt(blockLocation);
            if (headLoc == null) {
                sender.sendMessage(LanguageService.getMessage("Messages.NoTargetHeadBlock"));
                return true;
            }

            var applied = HeadUtils.applyTextureToBlock(blockLocation.getBlock(), args[2]);

            if (applied) {
                try {
                    StorageService.createNewHead(headLoc.getUuid(), args[2]);
                } catch (InternalException e) {
                    HeadBlocks.log.sendMessage("&cError with storage, head new texture not saved: " + e.getMessage());
                    applied = false;
                }
            }

            if (applied) {
                sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &aTexture applied!"));
            } else {
                sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cError trying to apply the texture, check logs."));
            }
        }

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 2 -> new ArrayList<>(List.of("texture"));
            case 3 -> new ArrayList<>(ConfigService.getHeads().stream()
                    .filter(s -> s.startsWith("default"))
                    .map(s -> s.replace("default:", "")).toList());
            default -> new ArrayList<>();
        };

    }
}
