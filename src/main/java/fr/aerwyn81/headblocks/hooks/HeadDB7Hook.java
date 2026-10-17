package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHeadDB;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import fr.aerwyn81.headblocks.utils.scheduler.Task;
import io.github.silentdevelopment.headdb.HeadDBService;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

public class HeadDB7Hook extends AbstractHeadDBHook {
    public static final String SERVICE_CLASS = "io.github.silentdevelopment.headdb.HeadDBService";

    private HeadDBService service;
    private Task readyTask = Task.NONE;

    public HeadDB7Hook(PluginProvider pluginProvider, SchedulerAdapter scheduler) {
        super(pluginProvider, scheduler);
    }

    @Override
    public boolean init(ServiceRegistry registry) {
        this.registry = registry;

        try {
            RegisteredServiceProvider<HeadDBService> provider = Bukkit.getServicesManager().getRegistration(HeadDBService.class);
            if (provider == null) {
                LogUtil.error("Error loading HeadDB support: HeadDBService is not registered. Please try to update HeadDB plugin or report the error on HeadBlocks discord.");
                return false;
            }
            service = provider.getProvider();
        } catch (NoClassDefFoundError ex) {
            LogUtil.error("Error loading HeadDB support: {0}. Please try to update HeadDB plugin or report the error on HeadBlocks discord.", ex.getMessage());
            return false;
        }

        readyTask = scheduler.runTaskTimer(this::awaitDatabase, 1, 20);

        LogUtil.success("HeadDB successfully hooked!");
        return true;
    }

    private void awaitDatabase() {
        if (!isReady()) {
            return;
        }

        readyTask.cancel();
        loadTextures();
    }

    @Override
    protected boolean isReady() {
        return service != null && service.status().available();
    }

    @Override
    protected void loadOne(HBHeadHeadDB head) {
        if (head.getId() <= 0) {
            LogUtil.error("HeadDB head id {0} is not found. Please check if the head id exists.", head.getId());
            return;
        }

        Optional<Head> optHead = service.find(HeadId.remote(head.getId()));
        if (optHead.isEmpty()) {
            LogUtil.error("HeadDB head id {0} is not found. Please check if the head id exists.", head.getId());
            return;
        }

        applyTexture(head, optHead.get().texture().hash());
    }
}
