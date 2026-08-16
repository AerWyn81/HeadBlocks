package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.ServiceRegistry;

/**
 * Draws the area being edited, so the admin sees its box while the menu is open.
 */
public class AreaOutlineTask implements Runnable {

    private final ServiceRegistry registry;

    public AreaOutlineTask(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run() {
        registry.getGuiService().getRequirementsGui().getAreaEditor().renderOutlines();
    }
}
