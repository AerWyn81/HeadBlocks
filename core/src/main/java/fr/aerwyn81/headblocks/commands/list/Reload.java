package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.hooks.HeadDatabaseHook;
import fr.aerwyn81.headblocks.services.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;

@HBAnnotations(command = "reload", permission = "headblocks.admin")
public class Reload implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        var plugin = HeadBlocks.getInstance();
        HeadBlocks.isReloadInProgress = true;

        GuiService.clearCache();

        HologramService.unload();

        StorageService.close();

        plugin.reloadConfig();
        ConfigService.load();

        LanguageService.setLanguage(ConfigService.getLanguage());
        LanguageService.pushMessages();

        StorageService.initialize();
        HeadService.load();

        if (HeadBlocks.isHeadDatabaseActive) {
            if (plugin.getHeadDatabaseHook() == null) {
                var headDatabaseApi = new HeadDatabaseHook();
                if (headDatabaseApi.init()) {
                    plugin.setHeadDatabaseHook(headDatabaseApi);
                }
            }

            plugin.getHeadDatabaseHook().loadHeadsHDB();
        }

        HologramService.load();

        var players = Collections.synchronizedCollection(Bukkit.getOnlinePlayers());
        StorageService.loadPlayers(players.toArray(new Player[0]));

        plugin.startInternalTaskTimer();

        HeadBlocks.isReloadInProgress = false;

        sender.sendMessage(LanguageService.getMessage("Messages.ReloadComplete"));
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
