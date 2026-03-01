package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.hooks.HeadDatabaseHook;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;

@HBAnnotations(command = "reload", permission = "headblocks.admin")
public class Reload implements Cmd {
    private final ServiceRegistry registry;

    public Reload(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        var plugin = HeadBlocks.getInstance();
        HeadBlocks.isReloadInProgress = true;

        plugin.reloadConfig();

        registry.reload();

        if (HeadBlocks.isHeadDatabaseActive) {
            if (plugin.getHeadDatabaseHook() == null) {
                var headDatabaseApi = new HeadDatabaseHook(registry);
                if (headDatabaseApi.init()) {
                    plugin.setHeadDatabaseHook(headDatabaseApi);
                }
            }

            plugin.getHeadDatabaseHook().loadHeadsHDB();
        }

        var players = Collections.synchronizedCollection(Bukkit.getOnlinePlayers());
        registry.getStorageService().loadPlayers(players.toArray(new Player[0]));

        plugin.startInternalTaskTimer();

        HeadBlocks.isReloadInProgress = false;

        sender.sendMessage(registry.getLanguageService().message("Messages.ReloadComplete"));
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
