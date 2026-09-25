package fr.aerwyn81.headblocks.services.gui;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public abstract class GuiBase {

    private static final HashMap<UUID, ItemStack> headItemCache = new HashMap<>();

    protected final ServiceRegistry registry;

    protected GuiBase(ServiceRegistry registry) {
        this.registry = registry;
    }

    protected ItemStack getHeadItemStackFromCache(HeadLocation headLocation) {
        var headUuid = headLocation.getUuid();

        if (!headItemCache.containsKey(headUuid)) {
            var content = headLocation.getContent();
            if (content != null && content.kind() != ContentKind.HEAD) {
                headItemCache.put(headUuid, registry.getVisualService().iconOf(content));
                return headItemCache.get(headUuid).clone();
            }

            try {
                var texture = registry.getStorageService().getHeadTexture(headUuid);
                headItemCache.put(headUuid, HeadUtils.applyTextureToItemStack(new ItemStack(Material.PLAYER_HEAD), texture));
            } catch (InternalException ignored) {
            }
        }

        return headItemCache.get(headLocation.getUuid()).clone();
    }

    public static void clearSharedCache() {
        headItemCache.clear();
    }
}
