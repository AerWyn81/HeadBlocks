package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HeadDeletedEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class HeadRemovalService {
    private final ServiceRegistry registry;

    public HeadRemovalService(ServiceRegistry registry) {
        this.registry = registry;
    }

    public boolean canRemove(Player player) {
        if (HeadBlocks.isReloadInProgress) {
            player.sendMessage(registry.getLanguageService().message("Messages.PluginReloading"));
            return false;
        }

        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            var message = registry.getLanguageService().message("Messages.NoPermissionBlock");
            if (!message.trim().isEmpty()) {
                player.sendMessage(message);
            }
            return false;
        }

        if (!player.isSneaking() || player.getGameMode() != GameMode.CREATIVE) {
            player.sendMessage(registry.getLanguageService().message("Messages.CreativeSneakRemoveHead"));
            return false;
        }

        return true;
    }

    public boolean remove(Player player, HeadLocation headLocation, Location location) {
        if (registry.getStorageService().isStorageError()) {
            player.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            return false;
        }

        try {
            registry.getHeadService().removeHeadLocation(headLocation, registry.getConfigService().resetPlayerData());
        } catch (InternalException ex) {
            player.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            LogUtil.error("Error while trying to remove a head \"{0}\" from the storage: {1}", headLocation.getNameOrUuid(), ex.getMessage());
            return false;
        }

        player.sendMessage(LocationUtils.parseLocationPlaceholders(registry.getLanguageService().message("Messages.HeadRemoved"), location));
        Bukkit.getPluginManager().callEvent(new HeadDeletedEvent(headLocation.getUuid(), location, headLocation.getHuntId()));
        return true;
    }
}
