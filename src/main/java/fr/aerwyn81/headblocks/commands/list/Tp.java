package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

@HBAnnotations(command = "tp", permission = "headblocks.tp", isPlayerCommand = true, isVisible = false)
public class Tp implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        try {
            Location loc = new Location(
                    Bukkit.getWorld(args[1]),
                    Double.parseDouble(args[2]),
                    Double.parseDouble(args[3]),
                    Double.parseDouble(args[4]),
                    Float.parseFloat(args[5]),
                    Float.parseFloat(args[6]));

            // Use FoliaLib's async teleport for cross-platform compatibility
            // On Folia: teleports asynchronously
            // On Paper: teleports asynchronously (if supported)
            // On Spigot: falls back to next tick teleport
            HeadBlocks.getInstance().getFoliaLib().getScheduler().teleportAsync(player, loc, null)
                    .thenAccept(success -> {
                        if (!success) {
                            player.sendMessage("Teleportation failed!");
                        }
                    });
        } catch (Exception ignored) {
        }

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}