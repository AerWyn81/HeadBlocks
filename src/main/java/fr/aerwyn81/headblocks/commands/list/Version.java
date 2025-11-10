package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

import static org.bukkit.Bukkit.getServer;

@HBAnnotations(command = "version", permission = "headblocks.admin", alias = "v")
public class Version implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        var versionBuilder =
                "\n" +
                        "&7----------------------------------------------------" +
                        "\n" +
                        "&6&lH&e&lead&6&lB&e&llocks &7version: &e" + HeadBlocks.getInstance().getDescription().getVersion() +
                        "\n" +
                        "&7Running on: &e" + Bukkit.getBukkitVersion() + " &8&o(" + getServer().getVersion() + ")" +
                        "\n" +
                        "&7----------------------------------------------------" +
                        "\n&7";

        sender.sendMessage(MessageUtils.colorize(versionBuilder));
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
