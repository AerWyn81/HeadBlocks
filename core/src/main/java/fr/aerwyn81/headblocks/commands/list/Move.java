package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.HeadMove;
import fr.aerwyn81.headblocks.handlers.HeadService;
import fr.aerwyn81.headblocks.handlers.LanguageService;
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

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        boolean hasConfirmInCommand = args.length > 1 && args[1].equals("--confirm");
        boolean hasCancelInCommand = args.length > 1 && args[1].equals("--cancel");

        if (hasCancelInCommand) {
            HeadMove hd = HeadService.getHeadMoves().remove(player.getUniqueId());
            if (hd != null) {
                player.sendMessage(LanguageService.getMessage("Messages.HeadMoveCancel"));
            }
            return true;
        }

        if (!hasConfirmInCommand && HeadService.getHeadMoves().containsKey(player.getUniqueId())) {
            player.sendMessage(LanguageService.getMessage("Messages.HeadMoveAlready"));
            return true;
        }

        Location targetLoc = player.getTargetBlock(null, 100).getLocation();

        if (!hasConfirmInCommand) {
            HeadLocation headLocation = HeadService.getHeadAt(targetLoc);

            if (headLocation == null) {
                player.sendMessage(LanguageService.getMessage("Messages.NoTargetHeadBlock"));
                return true;
            }

            String message = LanguageService.getMessage("Messages.TargetBlockInfo")
                    .replaceAll("%uuid%", headLocation.getUuid().toString());

            HeadService.getHeadMoves().put(player.getUniqueId(), new HeadMove(headLocation.getUuid(), targetLoc));

            player.sendMessage(MessageUtils.parseLocationPlaceholders(message, targetLoc));
            return true;
        }

        HeadMove headMove = HeadService.getHeadMoves().getOrDefault(player.getUniqueId(), null);

        if (headMove == null) {
            player.sendMessage(LanguageService.getMessage("Messages.HeadMoveNoPlayer"));
            return true;
        }

        Location newHeadBlockLoc = targetLoc.clone().add(0, 1, 0);

        if (LocationUtils.areEquals(newHeadBlockLoc, headMove.getOldLoc())) {
            player.sendMessage(LanguageService.getMessage("Messages.HeadMoveOtherLoc"));
            return true;
        }

        if (isTargetBlockInvalid(targetLoc.getBlock()) || !newHeadBlockLoc.getBlock().isEmpty()) {
            player.sendMessage(LanguageService.getMessage("Messages.TargetBlockInvalid"));
            return true;
        }

        HeadService.changeHeadLocation(headMove.gethUuid(), headMove.getOldLoc().getBlock(), newHeadBlockLoc.getBlock());
        HeadService.getHeadMoves().remove(player.getUniqueId());

        player.sendMessage(MessageUtils.parseLocationPlaceholders(LanguageService.getMessage("Messages.TargetBlockMoved"), newHeadBlockLoc));
        return true;
    }

    private boolean isTargetBlockInvalid(Block block) {
        return block.isEmpty() || block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 && HeadService.getHeadMoves().containsKey(((Player) sender).getUniqueId()) ?
                new ArrayList<>(Arrays.asList("--confirm", "--cancel")) : new ArrayList<>();
    }
}