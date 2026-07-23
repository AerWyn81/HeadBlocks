package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.utils.scheduler.task.TaskTimer;

public class ZoneOutlineTask extends TaskTimer {

    private final ServiceRegistry registry;

    public ZoneOutlineTask(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run() {
        registry.getGuiService().getZoneConfigManager().renderOutlines();
    }
}
