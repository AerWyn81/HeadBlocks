package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythiccrucible.api.MythicCrucibleProvider;
import io.lumine.mythiccrucible.events.MythicCrucibleReloadedEvent;
import io.lumine.mythiccrucible.events.MythicFurnitureRemoveEvent;
import io.lumine.mythiccrucible.events.MythicFurnitureRotateEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class MythicCrucibleHook extends ItemProviderHook {

    private final MythicCrucibleFurnitureRenderer furniture = new MythicCrucibleFurnitureRenderer();

    @Override
    public String prefix() {
        return "mythiccrucible";
    }

    @Override
    public String pluginName() {
        return "MythicCrucible";
    }

    @Override
    public boolean isReady() {
        try {
            MythicCrucibleProvider.get();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public void register(Plugin plugin, Runnable onReload) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onReloaded(MythicCrucibleReloadedEvent e) {
                onReload.run();
            }
        }, plugin);
    }

    @Override
    public void listenInteractions(Plugin plugin, HeadEntityInteractions interactions) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.LOW)
            public void onInteractEntity(PlayerInteractEntityEvent e) {
                interactEntity(e, false);
            }

            @EventHandler(priority = EventPriority.LOW)
            public void onInteractAtEntity(PlayerInteractAtEntityEvent e) {
                interactEntity(e, true);
            }

            private void interactEntity(PlayerInteractEntityEvent e, boolean claimDirect) {
                MythicCrucibleProvider.get().getFurniture(e.getRightClicked()).ifPresent(handle -> {
                    var frame = handle.getItemFrame();
                    if (interactions.isHead(frame)) {
                        e.setCancelled(true);
                        // Direct frame clicks are already claimed by OnHeadEntityEvent.
                        if ((claimDirect || !interactions.isHead(e.getRightClicked())) && e.getHand() == EquipmentSlot.HAND) {
                            interactions.use(e.getPlayer(), frame);
                        }
                    }
                });
            }

            @EventHandler(priority = EventPriority.LOW)
            public void onInteractBlock(PlayerInteractEvent e) {
                if (e.getClickedBlock() == null) {
                    return;
                }
                MythicCrucibleProvider.get().getFurniture(e.getClickedBlock()).ifPresent(handle -> {
                    var frame = handle.getItemFrame();
                    if (interactions.isHead(frame)) {
                        e.setCancelled(true);
                        if (e.getHand() != EquipmentSlot.HAND) {
                            return;
                        }
                        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            interactions.use(e.getPlayer(), frame);
                        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                            interactions.attack(e.getPlayer(), frame);
                        }
                    }
                });
            }

            @EventHandler(priority = EventPriority.LOW)
            public void onRemove(MythicFurnitureRemoveEvent e) {
                var frame = e.getFurniture().getItemFrame();
                if (interactions.isHead(frame) && !furniture.isRemoving(frame)) {
                    e.setCancelled(true);
                    if (e.getBreaker() instanceof Player player) {
                        interactions.attack(player, frame);
                    }
                }
            }

            @EventHandler(priority = EventPriority.LOW)
            public void onRotate(MythicFurnitureRotateEvent e) {
                if (interactions.isHead(e.getFurniture().getItemFrame())) {
                    e.setCancelled(true);
                }
            }
        }, plugin);
    }

    @Override
    public boolean exists(String id) {
        return MythicCrucibleProvider.get().getItem(id).isPresent();
    }

    @Override
    protected ItemStack item(String id) {
        return MythicCrucibleProvider.get().getItem(id)
                .map(item -> BukkitAdapter.adapt(item.getMythicItem().generateItemStack(1)))
                .orElse(null);
    }

    @Override
    public List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings) {
        return furniture.exists(content.value())
                ? furniture.spawn(anchor, content, settings)
                : super.spawn(anchor, content, settings);
    }

    @Override
    public void despawn(List<Entity> entities) {
        if (furniture.handles(entities)) {
            furniture.despawn(entities);
        } else {
            super.despawn(entities);
        }
    }

    @Override
    public double height(HeadContent content, RenderSettings settings, List<Entity> entities) {
        return furniture.handles(entities)
                ? furniture.height(content, settings, entities)
                : super.height(content, settings, entities);
    }

    @Override
    public void spin(List<Entity> entities, float angle, RenderSettings settings, int periodTicks) {
        if (!furniture.handles(entities)) {
            super.spin(entities, angle, settings, periodTicks);
        }
    }
}
