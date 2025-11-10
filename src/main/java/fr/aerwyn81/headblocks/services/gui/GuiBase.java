package fr.aerwyn81.headblocks.services.gui;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public abstract class GuiBase {

    private static final HashMap<UUID, ItemStack> headItemCache = new HashMap<>();

    protected ItemStack getHeadItemStackFromCache(HeadLocation headLocation) {
        var headUuid = headLocation.getUuid();

        if (!headItemCache.containsKey(headUuid)) {
            try {
                var texture = StorageService.getHeadTexture(headUuid);
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
