package fr.aerwyn81.headblocks.commands;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.PlayerUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class HBTabCompleter implements TabCompleter {
    private final HeadBlocks main;

    public HBTabCompleter(HeadBlocks main) {
        this.main = main;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return null;
        }

        Player player = (Player) sender;

        List<String> list = new ArrayList<>();
        List<String> auto = new ArrayList<>();

        if (args.length == 1) {
            list.add("help");
            list.add("version");

            if (PlayerUtils.hasPermission(player, "headblocks.use")) {
                list.add("me");
            }

            if (PlayerUtils.hasPermission(player, "headblocks.admin")) {
                list.add("give");
                list.add("remove");
                list.add("reset");
                list.add("resetall");
                list.add("list");
                list.add("stats");
                list.add("reload");
            }
        } else if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("stats")) {
            for (Player target : main.getServer().getOnlinePlayers()) {
                list.add(target.getName());
            }
        } else if (args[0].equalsIgnoreCase("remove")) {
            list.addAll(main.getHeadHandler().getHeadLocations().stream()
                    .map(Pair::getValue0)
                    .map(UUID::toString)
                    .collect(Collectors.toList()));
        } else if (args[0].equalsIgnoreCase("resetall")) {
            list.add("--confirm");
        }

        for (String s : list) {
            if (s.startsWith(args[args.length - 1])) {
                auto.add(s);
            }
        }

        return auto.isEmpty() ? Collections.emptyList() : auto;
    }
}
