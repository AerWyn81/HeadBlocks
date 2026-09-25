package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.head.visual.VisualForm;
import fr.aerwyn81.headblocks.services.HeadPlacementService;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class OnPlayerPlaceBlockEvent implements Listener {

    private final ServiceRegistry registry;
    private final HeadPlacementService placementService;

    public OnPlayerPlaceBlockEvent(ServiceRegistry registry) {
        this.registry = registry;
        this.placementService = new HeadPlacementService(registry);
    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Block headBlock = e.getBlockPlaced();

        if (registry.getGuiService().getTimedConfigManager().hasPendingPlatePlacement(player.getUniqueId())) {
            if (headBlock.getType().name().contains("PRESSURE_PLATE")) {
                Location plateLoc = headBlock.getLocation().clone().add(0.5, 0, 0.5);
                registry.getGuiService().getTimedConfigManager().handlePlatePlaced(player, plateLoc);
            }
            return;
        }

        if (!HeadUtils.isHeadBlocksItem(player.getInventory().getItemInMainHand())) {
            return;
        }

        Location headLocation = headBlock.getLocation().clone().add(0.5, 0, 0.5);

        var item = e.getItemInHand();
        var content = HeadUtils.getContent(item);
        if (content != null && content.kind() == ContentKind.BLOCK) {
            Map<String, Object> options = new HashMap<>(content.options());
            options.put("data", headBlock.getBlockData().getAsString());
            content = HeadContent.of(ContentKind.BLOCK, content.value(), options);
        } else if (content != null && content.kind() == ContentKind.HEAD && headBlock.getType() == Material.PLAYER_WALL_HEAD) {
            content = HeadContent.withWall(content);
        }

        var huntId = placementService.targetHuntId(player, item);
        placementService.place(player, item, huntId, headLocation, HeadUtils.yawOf(headBlock), content, () -> e.setCancelled(true));
    }

    @EventHandler
    public void onHeadBlocksItemUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        ItemStack item = e.getItem();
        if (HeadUtils.isHeadBlocksItem(item) && !item.getType().isBlock()) {
            e.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityHeadPlace(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) {
            return;
        }

        ItemStack item = e.getItem();
        if (!HeadUtils.isHeadBlocksItem(item)) {
            return;
        }

        Player player = e.getPlayer();
        var content = HeadUtils.getContent(item);
        var huntId = placementService.targetHuntId(player, item);
        var form = content == null ? VisualForm.HEAD_BLOCK : registry.getVisualService().formOf(content, huntId);
        if (form.isBlockBased() && item.getType().isBlock()) {
            return;
        }

        e.setCancelled(true);
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setUseItemInHand(Event.Result.DENY);

        var target = e.getClickedBlock().getRelative(e.getBlockFace());
        if (!target.isPassable() || target.isLiquid()) {
            player.sendMessage(registry.getLanguageService().message("Messages.TargetBlockInvalid"));
            return;
        }

        var anchor = target.getLocation().add(0.5, 0, 0.5);
        placementService.place(player, item, huntId, anchor, facingYaw(player), content, () -> {
        });
    }

    private float facingYaw(Player player) {
        return HeadUtils.snapYaw(player.getLocation().getYaw() + 180f);
    }
}
