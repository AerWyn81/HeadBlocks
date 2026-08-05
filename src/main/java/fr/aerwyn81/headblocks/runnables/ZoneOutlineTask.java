package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.ServiceRegistry;

public class ZoneOutlineTask implements Runnable {

    private final ServiceRegistry registry;

    public ZoneOutlineTask(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run() {
        registry.getGuiService().getZoneConfigManager().renderOutlines();
    }
}
