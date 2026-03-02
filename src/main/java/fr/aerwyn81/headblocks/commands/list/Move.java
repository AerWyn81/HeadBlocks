package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.HeadMove;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;

@HBAnnotations(command = "move", permission = "headblocks.admin", isPlayerCommand = true, alias = "m")
public class Move implements Cmd {
    private final ServiceRegistry registry;

    public Move(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        boolean hasConfirmInCommand = args.length > 1 && args[1].equals("--confirm");
        boolean hasCancelInCommand = args.length > 1 && args[1].equals("--cancel");

        if (hasCancelInCommand) {
            HeadMove hd = registry.getHeadService().getHeadMoves().remove(player.getUniqueId());
            if (hd != null) {
                player.sendMessage(registry.getLanguageService().message("Messages.HeadMoveCancel"));
            }
            return true;
        }

        if (!hasConfirmInCommand && registry.getHeadService().getHeadMoves().containsKey(player.getUniqueId())) {
            player.sendMessage(registry.getLanguageService().message("Messages.HeadMoveAlready"));
            return true;
        }

        Location targetLoc = player.getTargetBlock(null, 100).getLocation();

        if (!hasConfirmInCommand) {
            HeadLocation headLocation = registry.getHeadService().getHeadAt(targetLoc);

            if (headLocation == null) {
                player.sendMessage(registry.getLanguageService().message("Messages.NoTargetHeadBlock"));
                return true;
            }

            String message = registry.getLanguageService().message("Messages.TargetBlockInfo")
                    .replace("%uuid%", headLocation.getNameOrUuid());

            registry.getHeadService().getHeadMoves().put(player.getUniqueId(), new HeadMove(headLocation.getUuid(), targetLoc));

            player.sendMessage(LocationUtils.parseLocationPlaceholders(message, targetLoc));
            return true;
        }

        HeadMove headMove = registry.getHeadService().getHeadMoves().getOrDefault(player.getUniqueId(), null);

        if (headMove == null) {
            player.sendMessage(registry.getLanguageService().message("Messages.HeadMoveNoPlayer"));
            return true;
        }

        Location newHeadBlockLoc = targetLoc.clone().add(0, 1, 0);

        if (LocationUtils.areEquals(newHeadBlockLoc, headMove.oldLoc())) {
            player.sendMessage(registry.getLanguageService().message("Messages.HeadMoveOtherLoc"));
            return true;
        }

        if (isTargetBlockInvalid(targetLoc.getBlock()) || !newHeadBlockLoc.getBlock().isEmpty()) {
            player.sendMessage(registry.getLanguageService().message("Messages.TargetBlockInvalid"));
            return true;
        }

        registry.getHeadService().changeHeadLocation(headMove.hUuid(), headMove.oldLoc().getBlock(), newHeadBlockLoc.getBlock());
        registry.getHeadService().getHeadMoves().remove(player.getUniqueId());

        player.sendMessage(LocationUtils.parseLocationPlaceholders(registry.getLanguageService().message("Messages.TargetBlockMoved"), newHeadBlockLoc));
        return true;
    }

    private boolean isTargetBlockInvalid(Block block) {
        return block.isEmpty() || HeadUtils.isPlayerHead(block);
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 && registry.getHeadService().getHeadMoves().containsKey(((Player) sender).getUniqueId()) ?
                new ArrayList<>(Arrays.asList("--confirm", "--cancel")) : new ArrayList<>();
    }
}
