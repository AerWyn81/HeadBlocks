package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.ConfigService;
import fr.aerwyn81.headblocks.handlers.HeadService;
import fr.aerwyn81.headblocks.handlers.HologramService;
import fr.aerwyn81.headblocks.handlers.LanguageService;
import fr.aerwyn81.headblocks.runnables.GlobalTask;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

@HBAnnotations(command = "reload", permission = "headblocks.admin")
public class Reload implements Cmd {
    private final HeadBlocks main;

    public Reload(HeadBlocks main) {
        this.main = main;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        HeadBlocks.isReloadInProgress = true;

        main.reloadConfig();
        ConfigService.load();

        LanguageService.setLanguage(ConfigService.getLanguage());
        LanguageService.pushMessages();

        HologramService.load();

        HeadService.load();

        if (HeadBlocks.isHeadDatabaseActive) {
            main.loadHeadsHDB();
        }

        main.getStorageHandler().close();

        main.getStorageHandler().init();
        for (Player player : Bukkit.getOnlinePlayers()) {
            main.getStorageHandler().unloadPlayer(player);
            main.getStorageHandler().loadPlayer(player);
        }

        main.getParticlesTask().cancel();

        main.setParticlesTask(new GlobalTask(main));
        main.getParticlesTask().runTaskTimer(main, 0, ConfigService.getDelayGlobalTask());

        if (main.getStorageHandler().hasStorageError()) {
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
