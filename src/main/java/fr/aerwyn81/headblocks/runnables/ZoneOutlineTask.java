package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.ServiceRegistry;
import org.bukkit.scheduler.BukkitRunnable;

public class ZoneOutlineTask extends BukkitRunnable {

    private final ServiceRegistry registry;

    public ZoneOutlineTask(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run() {
        registry.getGuiService().getZoneConfigManager().renderOutlines();
    }
}
