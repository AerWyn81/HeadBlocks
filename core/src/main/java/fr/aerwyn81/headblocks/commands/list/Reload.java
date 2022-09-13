package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.hooks.HeadDatabaseHook;
import fr.aerwyn81.headblocks.runnables.GlobalTask;
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

        plugin.reloadConfig();
        ConfigService.load();

        LanguageService.setLanguage(ConfigService.getLanguage());
        LanguageService.pushMessages();

        StorageService.close();

        StorageService.initialize();
        for (Player player : Collections.synchronizedCollection(Bukkit.getOnlinePlayers())) {
            StorageService.unloadPlayer(player);
            StorageService.loadPlayer(player);
        }

        plugin.getParticlesTask().cancel();

        HologramService.load();
        HeadService.load();

        if (plugin.isHeadDatabaseActive()) {
            if (plugin.getHeadDatabaseHook() == null) {
                plugin.setHeadDatabaseHook(new HeadDatabaseHook());
            }

            plugin.getHeadDatabaseHook().loadHeadsHDB();
        }

        plugin.setParticlesTask(new GlobalTask());
        plugin.getParticlesTask().runTaskTimer(plugin, 0, ConfigService.getDelayGlobalTask());

        if (StorageService.hasStorageError()) {
            sender.sendMessage(LanguageService.getMessage("Messages.ReloadWithErrors"));
            return true;
        }

        HeadBlocks.isReloadInProgress = false;

        sender.sendMessage(LanguageService.getMessage("Messages.ReloadComplete"));
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
