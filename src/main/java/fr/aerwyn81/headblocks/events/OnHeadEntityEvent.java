package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.hooks.visual.HeadEntityInteractions;
import fr.aerwyn81.headblocks.services.HeadClaimService;
import fr.aerwyn81.headblocks.services.HeadRemovalService;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;

public class OnHeadEntityEvent implements Listener, HeadEntityInteractions {

    private final ServiceRegistry registry;
    private final HeadClaimService claimService;
    private final HeadRemovalService removalService;

    public OnHeadEntityEvent(ServiceRegistry registry) {
        this.registry = registry;
        this.claimService = new HeadClaimService(registry);
        this.removalService = new HeadRemovalService(registry);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEntityEvent e) {
        var entity = e.getRightClicked();
        var visualService = registry.getVisualService();

        if (!visualService.isHeadEntity(entity)) {
            var item = e.getPlayer().getInventory().getItemInMainHand();
            if (HeadUtils.isHeadBlocksItem(item) && item.getType().name().endsWith("_SPAWN_EGG")) {
                e.setCancelled(true);
            }
            return;
        }

        e.setCancelled(true);

        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        click(e.getPlayer(), entity);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (registry.getVisualService().isHeadEntity(e.getRightClicked())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        var entity = e.getEntity();
        if (!registry.getVisualService().isHeadEntity(entity)) {
            return;
        }

        e.setCancelled(true);

        if (e instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player player) {
            attack(player, entity);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreak(HangingBreakEvent e) {
        if (registry.getVisualService().isHeadEntity(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCombust(EntityCombustEvent e) {
        if (registry.getVisualService().isHeadEntity(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTransform(EntityTransformEvent e) {
        if (registry.getVisualService().isHeadEntity(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPortal(EntityPortalEvent e) {
        if (registry.getVisualService().isHeadEntity(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTarget(EntityTargetEvent e) {
        if (registry.getVisualService().isHeadEntity(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        registry.getVisualService().onChunkLoad(e.getChunk());
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        for (var entity : e.getEntities()) {
            registry.getVisualService().removeOrphan(entity);
        }
    }

    @Override
    public boolean isHead(Entity entity) {
        return registry.getVisualService().isHeadEntity(entity);
    }

    @Override
    public void use(Player player, Entity entity) {
        click(player, entity);
    }

    @Override
    public void attack(Player player, Entity entity) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            removeIfAllowed(player, entity);
            return;
        }

        click(player, entity);
    }

    private void click(Player player, Entity entity) {
        if (HeadBlocks.isReloadInProgress) {
            player.sendMessage(registry.getLanguageService().message("Messages.PluginReloading"));
            return;
        }

        HeadLocation headLocation = registry.getVisualService().headOf(entity);
        if (headLocation == null) {
            return;
        }

        claimService.click(player, headLocation, headLocation.getLocation(), false);
    }

    private void removeIfAllowed(Player player, Entity entity) {
        HeadLocation headLocation = registry.getVisualService().headOf(entity);
        if (headLocation == null || !removalService.canRemove(player)) {
            return;
        }

        removalService.remove(player, headLocation, headLocation.getLocation());
    }
}
