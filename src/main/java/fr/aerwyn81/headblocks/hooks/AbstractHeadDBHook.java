package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHeadDB;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public abstract class AbstractHeadDBHook implements HeadProviderHook {
    private static final String HOOK_PREFIX = "headdb";

    private static final String MINECRAFT_TEXTURE_URL = "http://textures.minecraft.net/texture/";

    private final PluginProvider pluginProvider;
    protected final SchedulerAdapter scheduler;
    protected ServiceRegistry registry;

    protected AbstractHeadDBHook(PluginProvider pluginProvider, SchedulerAdapter scheduler) {
        this.pluginProvider = pluginProvider;
        this.scheduler = scheduler;
    }

    protected abstract boolean isReady();

    protected abstract void loadOne(HBHeadHeadDB head);

    @Override
    public String prefix() {
        return HOOK_PREFIX;
    }

    @Override
    public boolean isAvailable() {
        return pluginProvider.isHeadDBActive();
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
        if (registry == null || !isReady()) {
            return;
        }

        registry.getHeadService().getHeads().stream()
                .filter(HBHeadHeadDB.class::isInstance)
                .map(HBHeadHeadDB.class::cast)
                .filter(h -> !h.isLoaded())
                .forEach(this::loadOne);
    }

    protected void applyTexture(HBHeadHeadDB head, String hash) {
        HeadUtils.createHead(head, toBase64Texture(hash));
        head.setLoaded(true);
        LogUtil.info("Loaded HeadDB head id {0}.", head.getId());
    }

    // HeadDB returns a raw texture hash; HeadUtils expects a base64-encoded JSON profile.
    static String toBase64Texture(String hash) {
        var json = "{\"textures\":{\"SKIN\":{\"url\":\"" + MINECRAFT_TEXTURE_URL + hash + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
