package fr.aerwyn81.headblocks.utils.bukkit;

import org.bukkit.Bukkit;

public class BukkitCommandDispatcher implements CommandDispatcher {
    @Override
    public void dispatchConsoleCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
