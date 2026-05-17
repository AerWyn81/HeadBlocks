package fr.aerwyn81.headblocks.hooks;

import com.github.thesilentpro.headdb.api.HeadAPI;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHeadDB;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HeadDBHook implements HeadProviderHook {
    public static final String PREFIX = "headdb";

    private static final String MINECRAFT_TEXTURE_URL = "http://textures.minecraft.net/texture/";

    private final PluginProvider pluginProvider;
    private final SchedulerAdapter scheduler;
    private ServiceRegistry registry;
    private HeadAPI headAPI;

    public HeadDBHook(PluginProvider pluginProvider, SchedulerAdapter scheduler) {
        this.pluginProvider = pluginProvider;
        this.scheduler = scheduler;
    }

    @Override
    public String prefix() {
        return PREFIX;
    }

    @Override
    public boolean isAvailable() {
        return pluginProvider.isHeadDBActive();
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
    public HBHead createHead(ItemStack base, String rawId) {
        int id;
        try {
            id = Integer.parseInt(rawId.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("HeadDB id must be a number, got: " + rawId, ex);
        }
        return new HBHeadHeadDB(base, id);
    }

    @Override
    public void loadTextures() {
        if (registry == null || headAPI == null) {
            return;
        }

        registry.getHeadService().getHeads().stream()
                .filter(HBHeadHeadDB.class::isInstance)
                .map(HBHeadHeadDB.class::cast)
                .filter(h -> !h.isLoaded())
                .forEach(this::loadOne);
    }

    private void loadOne(HBHeadHeadDB head) {
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
            scheduler.runTaskGlobal(() -> applyTexture(head, toBase64Texture(texture)));
        }).exceptionally(ex -> {
            LogUtil.error("Error loading HeadDB head id {0}: {1}", head.getId(), ex.getMessage());
            return null;
        });
    }

    private void applyTexture(HBHeadHeadDB head, String texture) {
        HeadUtils.createHead(head, texture);
        head.setLoaded(true);
        LogUtil.info("Loaded HeadDB head id {0}.", head.getId());
    }

    // HeadDB returns a raw texture hash; HeadUtils expects a base64-encoded JSON profile.
    static String toBase64Texture(String hash) {
        var json = "{\"textures\":{\"SKIN\":{\"url\":\"" + MINECRAFT_TEXTURE_URL + hash + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
