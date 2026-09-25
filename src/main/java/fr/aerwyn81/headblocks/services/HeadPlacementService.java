package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HeadCreatedEvent;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.bukkit.ParticlesUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class HeadPlacementService {
    private final ServiceRegistry registry;

    public HeadPlacementService(ServiceRegistry registry) {
        this.registry = registry;
    }

    public String targetHuntId(Player player, ItemStack item) {
        String huntId = HeadUtils.getHuntId(item);
        return huntId != null ? huntId : registry.getHuntService().getSelectedHunt(player.getUniqueId());
    }

    public boolean place(Player player, ItemStack item, String huntId, Location location, float yaw, HeadContent content, Runnable cancel) {
        if (HeadBlocks.isReloadInProgress) {
            cancel.run();
            player.sendMessage(registry.getLanguageService().message("Messages.PluginReloading"));
            return false;
        }

        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            cancel.run();

            var message = registry.getLanguageService().message("Messages.NoPermissionBlock");
            if (!message.trim().isEmpty()) {
                player.sendMessage(message);
            }

            return false;
        }

        if (!player.isSneaking() || player.getGameMode() != GameMode.CREATIVE) {
            cancel.run();
            player.sendMessage(registry.getLanguageService().message("Messages.CreativeSneakAddHead"));
            return false;
        }

        if (registry.getHeadService().getHeadAt(location) != null) {
            cancel.run();
            player.sendMessage(registry.getLanguageService().message("Messages.HeadAlreadyExistHere"));
            return false;
        }

        if (registry.getStorageService().isStorageError()) {
            cancel.run();
            player.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            return false;
        }

        if (content == null) {
            cancel.run();
            player.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            LogUtil.error("Error, head texture not resolved when trying to save the head for player {0}", player.getName());
            return false;
        }

        String linkedHuntId = HeadUtils.getHuntId(item);
        if (linkedHuntId != null && !registry.getHuntService().huntExists(linkedHuntId)) {
            cancel.run();
            player.sendMessage(registry.getLanguageService().message("Messages.HeadHuntDeleted")
                    .replace("%hunt%", linkedHuntId));
            return false;
        }

        var hunt = registry.getHuntService().getHuntById(huntId);
        if (hunt != null
                && registry.getAreaEnforcementService().isLocationOutsideArea(hunt, location)) {
            cancel.run();
            player.sendMessage(registry.getLanguageService().message("Messages.AreaHeadOutside")
                    .replace("%hunt%", hunt.getDisplayName()));
            return false;
        }

        UUID headUuid;
        try {
            headUuid = registry.getHeadService().saveHeadLocation(location, content, yaw, huntId);
        } catch (InternalException ex) {
            cancel.run();
            player.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            LogUtil.error("Error while trying to create new HeadBlocks from the storage: {0}", ex.getMessage());
            return false;
        }

        ParticlesUtils.spawn(location, ParticlesUtils.resolve("HAPPY_VILLAGER"), 10, null, player);

        player.sendMessage(LocationUtils.parseLocationPlaceholders(registry.getLanguageService().message("Messages.HeadPlaced"), location));

        if (HBHunt.DEFAULT_ID.equals(huntId) && registry.getHuntService().isMultiHunt()) {
            TextComponent msg = new TextComponent(MessageUtils.colorize(
                    registry.getLanguageService().prefix() + " &7Assigned to &edefault&7. "));
            TextComponent clickable = new TextComponent(MessageUtils.colorize("&a&l[Reassign]"));
            clickable.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(MessageUtils.colorize("&7Click to reassign this head"))));
            clickable.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                    "/headblocks hunt transfer " + headUuid + " "));
            msg.addExtra(clickable);
            player.spigot().sendMessage(msg);
        }

        Bukkit.getPluginManager().callEvent(new HeadCreatedEvent(headUuid, location, huntId));
        return true;
    }
}
