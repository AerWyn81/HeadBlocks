package fr.aerwyn81.headblocks.hooks;

import com.github.thesilentpro.headdb.api.HeadAPI;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHeadDB;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class HeadDBHook extends AbstractHeadDBHook {
    private HeadAPI headAPI;

    public HeadDBHook(PluginProvider pluginProvider, SchedulerAdapter scheduler) {
        super(pluginProvider, scheduler);
    }

    @Override
    public boolean init(ServiceRegistry registry) {
        this.registry = registry;

        try {
            RegisteredServiceProvider<HeadAPI> provider = Bukkit.getServicesManager().getRegistration(HeadAPI.class);
            if (provider == null) {
                LogUtil.error("Error loading HeadDB support: HeadAPI service is not registered. Please try to update HeadDB plugin or report the error on HeadBlocks discord.");
                return false;
            }
            headAPI = provider.getProvider();
        } catch (NoClassDefFoundError ex) {
            LogUtil.error("Error loading HeadDB support: {0}. Please try to update HeadDB plugin or report the error on HeadBlocks discord.", ex.getMessage());
            return false;
        }

        headAPI.onReady().thenRun(() -> scheduler.runTask(this::loadTextures));

        if (headAPI.isReady()) {
            this.loadTextures();
        }

        LogUtil.success("HeadDB successfully hooked!");
        return true;
    }

    @Override
    protected boolean isReady() {
        return headAPI != null;
    }

    @Override
    protected void loadOne(HBHeadHeadDB head) {
        headAPI.findById(head.getId()).thenAccept(optHead -> {
            if (optHead.isEmpty()) {
                LogUtil.error("HeadDB head id {0} is not found. Please check if the head id exists.", head.getId());
                return;
            }

            String texture = optHead.get().getTexture();
            if (texture == null || texture.isEmpty()) {
                LogUtil.error("HeadDB head id {0} has no texture.", head.getId());
                return;
            }

            // NBT/ItemStack mutation must run on the main thread.
            scheduler.runTask(() -> applyTexture(head, texture));
        }).exceptionally(ex -> {
            LogUtil.error("Error loading HeadDB head id {0}: {1}", head.getId(), ex.getMessage());
            return null;
        });
    }
}
