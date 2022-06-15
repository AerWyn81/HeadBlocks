package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.HeadMove;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.LocationUtils;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;

@HBAnnotations(command = "move", permission = "headblocks.admin", isPlayerCommand = true)
public class Move implements Cmd {
    private final LanguageHandler languageHandler;
    private final HeadHandler headHandler;

    public Move(HeadBlocks main) {
        this.languageHandler = main.getLanguageHandler();
        this.headHandler = main.getHeadHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        boolean hasConfirmInCommand = args.length > 1 && args[1].equals("--confirm");
        boolean hasCancelInCommand = args.length > 1 && args[1].equals("--cancel");

        if (hasCancelInCommand) {
            HeadMove hd = headHandler.getHeadMoves().remove(player.getUniqueId());
            if (hd != null) {
                player.sendMessage(languageHandler.getMessage("Messages.HeadMoveCancel"));
            }
            return true;
        }

        if (!hasConfirmInCommand && headHandler.getHeadMoves().containsKey(player.getUniqueId())) {
            player.sendMessage(languageHandler.getMessage("Messages.HeadMoveAlready"));
            return true;
        }

        Location targetLoc = player.getTargetBlock(null, 100).getLocation();

        if (!hasConfirmInCommand) {
            HeadLocation headLocation = headHandler.getHeadAt(targetLoc);

            if (headLocation == null) {
                player.sendMessage(languageHandler.getMessage("Messages.NoTargetHeadBlock"));
                return true;
            }

            String message = languageHandler.getMessage("Messages.TargetBlockInfo")
                    .replaceAll("%uuid%", headLocation.getUuid().toString());

            headHandler.getHeadMoves().put(player.getUniqueId(), new HeadMove(headLocation.getUuid(), targetLoc));

            player.sendMessage(MessageUtils.parseLocationPlaceholders(message, targetLoc));
            return true;
        }

        HeadMove headMove = headHandler.getHeadMoves().getOrDefault(player.getUniqueId(), null);

        if (headMove == null) {
            player.sendMessage(languageHandler.getMessage("Messages.HeadMoveNoPlayer"));
            return true;
        }

        Location newHeadBlockLoc = targetLoc.clone().add(0, 1, 0);

        if (LocationUtils.areEquals(newHeadBlockLoc, headMove.getOldLoc())) {
            player.sendMessage(languageHandler.getMessage("Messages.HeadMoveOtherLoc"));
            return true;
        }

        if (isTargetBlockInvalid(targetLoc.getBlock()) || !newHeadBlockLoc.getBlock().isEmpty()) {
            player.sendMessage(languageHandler.getMessage("Messages.TargetBlockInvalid"));
            return true;
        }

        headHandler.changeHeadLocation(headMove.gethUuid(), headMove.getOldLoc().getBlock(), newHeadBlockLoc.getBlock());
        headHandler.getHeadMoves().remove(player.getUniqueId());

        player.sendMessage(MessageUtils.parseLocationPlaceholders(languageHandler.getMessage("Messages.TargetBlockMoved"), newHeadBlockLoc));
        return true;
    }

    private boolean isTargetBlockInvalid(Block block) {
        return block.isEmpty() || block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 && headHandler.getHeadMoves().containsKey(((Player) sender).getUniqueId()) ?
                new ArrayList<>(Arrays.asList("--confirm", "--cancel")) : new ArrayList<>();
    }
}