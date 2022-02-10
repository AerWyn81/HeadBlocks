package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.runnables.ParticlesTask;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import fr.aerwyn81.headblocks.utils.Version;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

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
        main.reloadConfig();
        main.getConfigHandler().loadConfiguration();

        main.getLanguageHandler().setLanguage(main.getConfigHandler().getLanguage());
        main.getLanguageHandler().pushMessages();

        main.getHeadHandler().loadConfiguration();
        main.getHeadHandler().loadLocations();

        if (HeadBlocks.isHeadDatabaseActive) {
            main.loadHeadsHDB();
        }

        main.getStorageHandler().getStorage().close();
        main.getStorageHandler().getDatabase().close();

        main.getStorageHandler().initStorage();
        main.getStorageHandler().getStorage().init();

        main.getStorageHandler().openConnection();
        main.getStorageHandler().getDatabase().load();

        Bukkit.getScheduler().cancelTasks(main);
        if (configHandler.isParticlesEnabled()) {
            if (Version.getCurrent().isOlderThan(Version.v1_13)) {
                HeadBlocks.log.sendMessage(FormatUtils.translate("&cParticles is enabled but not supported before 1.13 included."));
            } else {
                main.setParticlesTask(new ParticlesTask(main));
                main.getParticlesTask().runTaskTimer(main, 0, configHandler.getParticlesDelay());
            }
        }

        sender.sendMessage(languageHandler.getMessage("Messages.ReloadComplete"));
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
