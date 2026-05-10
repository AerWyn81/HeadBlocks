package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.HBHead;
import org.bukkit.inventory.ItemStack;

public interface HeadProviderHook {
    String prefix();

    boolean isAvailable();

    boolean init(ServiceRegistry registry);

    HBHead createHead(ItemStack base, String rawId);

    void loadTextures();
}
