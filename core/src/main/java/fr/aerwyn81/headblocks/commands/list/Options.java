package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

@HBAnnotations(command = "options", permission = "headblocks.admin", isPlayerCommand = true, isVisible = false)
public class Options implements Cmd {
    //Todo: Ordering, Removing, Per-head actions, One-time global head click

    @Override
    public boolean perform(CommandSender sender, String[] args) {

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}