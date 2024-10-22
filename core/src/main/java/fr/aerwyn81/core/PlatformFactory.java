package fr.aerwyn81.core;

import fr.aerwyn81.common.IServerPlatform;
import fr.aerwyn81.paper.PaperPlatform;
import fr.aerwyn81.spigot.SpigotPlatform;
import org.bukkit.plugin.java.JavaPlugin;

public class PlatformFactory {

    public static IServerPlatform createPlatform(JavaPlugin plugin) {
        var isPaper = isPaper();

        if (isPaper) {
            return new PaperPlatform(plugin);
        } else {
            return new SpigotPlatform(plugin);
        }
    }

    private static boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
