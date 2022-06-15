package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.runnables.GlobalTask;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

@HBAnnotations(command = "reload", permission = "headblocks.admin")
public class Reload implements Cmd {
    private final HeadBlocks main;
    private final ConfigHandler configHandler;
    private final LanguageHandler languageHandler;

    public Reload(HeadBlocks main) {
        this.main = main;
        this.configHandler = main.getConfigHandler();
        this.languageHandler = main.getLanguageHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        HeadBlocks.isReloadInProgress = true;

        main.reloadConfig();
        main.getConfigHandler().loadConfiguration();

        main.getLanguageHandler().setLanguage(main.getConfigHandler().getLanguage());
        main.getLanguageHandler().pushMessages();

        if (configHandler.isHologramsEnabled()) {
            main.getHologramHandler().unload();
        }

        main.getHeadHandler().loadConfiguration();
        main.getHeadHandler().loadLocations();

        main.getHeadHandler().getHeadMoves().clear();

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
        main.getParticlesTask().runTaskTimer(main, 0, configHandler.getDelayGlobalTask());

        if (main.getStorageHandler().hasStorageError()) {
            sender.sendMessage(languageHandler.getMessage("Messages.ReloadWithErrors"));
            return true;
        }

        HeadBlocks.isReloadInProgress = false;

        sender.sendMessage(languageHandler.getMessage("Messages.ReloadComplete"));
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
