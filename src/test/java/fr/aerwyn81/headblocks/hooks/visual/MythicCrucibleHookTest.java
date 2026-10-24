package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import io.lumine.mythiccrucible.api.FurniturePlacementResult;
import io.lumine.mythiccrucible.api.MythicCrucibleAPI;
import io.lumine.mythiccrucible.api.MythicCrucibleProvider;
import io.lumine.mythiccrucible.api.furniture.Furniture;
import io.lumine.mythiccrucible.api.furniture.FurnitureItemContext;
import io.lumine.mythiccrucible.api.items.CrucibleItem;
import io.lumine.mythiccrucible.events.MythicCrucibleReloadedEvent;
import io.lumine.mythiccrucible.events.MythicFurnitureRemoveEvent;
import io.lumine.mythiccrucible.events.MythicFurnitureRotateEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MythicCrucibleHookTest {

    private final MythicCrucibleAPI api = mock(MythicCrucibleAPI.class);
    private final World world = mock(World.class);
    private final Block block = mock(Block.class);
    private final Location anchor = new Location(world, 1, 2, 3);
    private final Interaction base = mock(Interaction.class);
    private final Entity frame = mock(Entity.class);
    private final Furniture handle = mock(Furniture.class);
    private final FurnitureItemContext context = mock(FurnitureItemContext.class);
    private final RenderSettings settings = new RenderSettings(2, true, 90);
    private final HeadContent content = HeadContent.external("mythiccrucible", "Chair", Map.of("width", "2", "height", "3"));
    private final MythicCrucibleHook hook = new MythicCrucibleHook();

    @BeforeEach
    void setUp() {
        when(world.getBlockAt(anchor)).thenReturn(block);
        when(world.spawn(anchor, Interaction.class)).thenReturn(base);
        when(base.getUniqueId()).thenReturn(UUID.randomUUID());
        when(frame.getUniqueId()).thenReturn(UUID.randomUUID());
        when(handle.getItemFrame()).thenReturn(frame);
        when(handle.getFurnitureData()).thenReturn(context);
        when(api.isFurnitureItem("Chair")).thenReturn(true);
        when(api.placeFurnitureResult("Chair", block, 90)).thenReturn(FurniturePlacementResult.SUCCESS);
        when(api.getFurniture(block)).thenReturn(Optional.of(handle));
        when(api.getFurniture(frame)).thenReturn(Optional.of(handle));
    }

    @Test
    void furniture_usesNativePlacementHitboxAndFreshRemovalWithoutDrops() {
        try (var provider = mockStatic(MythicCrucibleProvider.class)) {
            provider.when(MythicCrucibleProvider::get).thenReturn(api);
            var entities = hook.spawn(anchor, content, settings);
            assertThat(entities).containsExactly(base, frame);
            verify(base).setInteractionWidth(2f);
            verify(base).setInteractionHeight(3f);
            verify(frame).setGlowing(true);
            assertThat(hook.height(content, settings, entities)).isEqualTo(3);
            hook.spin(entities, 20, settings, 1);
            verify(frame, never()).setRotation(anyFloat(), anyFloat());

            // A reload replaces handles while keeping the frame alive.
            var reloaded = mock(Furniture.class);
            var reloadedContext = mock(FurnitureItemContext.class);
            when(reloaded.getFurnitureData()).thenReturn(reloadedContext);
            when(api.getFurniture(frame)).thenReturn(Optional.of(reloaded));
            hook.despawn(entities);
            verify(reloadedContext).remove(reloaded, null, false, false);
            verify(context, never()).remove(any(), any(), anyBoolean(), anyBoolean());
            verify(base).remove();
            verify(frame).remove();
        }
    }

    @Test
    void rejectedPlacement_removesItsInteractionAndReportsTheReason() {
        when(api.placeFurnitureResult("Chair", block, 90)).thenReturn(FurniturePlacementResult.OBSTRUCTED);
        try (var provider = mockStatic(MythicCrucibleProvider.class)) {
            provider.when(MythicCrucibleProvider::get).thenReturn(api);
            assertThatThrownBy(() -> hook.spawn(anchor, content, settings))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("OBSTRUCTED");
            verify(base).remove();
            verifyNoInteractions(context);
        }
    }

    @Test
    void readinessAndCatalog_followThePublicProvider() {
        try (var provider = mockStatic(MythicCrucibleProvider.class)) {
            provider.when(MythicCrucibleProvider::get).thenThrow(new IllegalStateException("not enabled"));
            assertThat(hook.isReady()).isFalse();
            provider.when(MythicCrucibleProvider::get).thenReturn(api);
            when(api.getItem("Chair")).thenReturn(Optional.of(mock(CrucibleItem.class)));
            assertThat(hook.isReady()).isTrue();
            assertThat(hook.exists("Chair")).isTrue();
            assertThat(hook.exists("Unknown")).isFalse();
        }
    }

    @Test
    void interactions_protectFurnitureAndAllowHeadBlocksCleanup() throws Exception {
        var manager = mock(PluginManager.class);
        var plugin = mock(Plugin.class);
        var interactions = mock(HeadEntityInteractions.class);
        var player = mock(Player.class);
        when(interactions.isHead(frame)).thenReturn(true);
        try (var provider = mockStatic(MythicCrucibleProvider.class);
             var bukkit = mockStatic(Bukkit.class)) {
            provider.when(MythicCrucibleProvider::get).thenReturn(api);
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            hook.listenInteractions(plugin, interactions);
            var captured = ArgumentCaptor.forClass(Listener.class);
            verify(manager).registerEvents(captured.capture(), eq(plugin));
            var listener = captured.getValue();

            var rightClick = new PlayerInteractEntityEvent(player, frame, EquipmentSlot.HAND);
            dispatch(listener, "onInteractEntity", rightClick);
            assertThat(rightClick.isCancelled()).isTrue();
            verify(interactions, never()).use(any(), any()); // generic entity listener handles this click

            var preciseClick = new PlayerInteractAtEntityEvent(player, frame, new Vector(), EquipmentSlot.HAND);
            dispatch(listener, "onInteractAtEntity", preciseClick);
            assertThat(preciseClick.isCancelled()).isTrue();
            verify(interactions).use(player, frame);
            clearInvocations(interactions);

            var blockClick = mock(PlayerInteractEvent.class);
            when(blockClick.getClickedBlock()).thenReturn(block);
            when(blockClick.getHand()).thenReturn(EquipmentSlot.HAND);
            when(blockClick.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
            when(blockClick.getPlayer()).thenReturn(player);
            dispatch(listener, "onInteractBlock", blockClick);
            verify(blockClick).setCancelled(true);
            verify(interactions).use(player, frame);

            clearInvocations(interactions, blockClick);
            when(blockClick.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
            dispatch(listener, "onInteractBlock", blockClick);
            verify(blockClick).setCancelled(true);
            verify(interactions, never()).use(any(), any());
            verify(interactions, never()).attack(any(), any());

            when(blockClick.getHand()).thenReturn(EquipmentSlot.HAND);
            when(blockClick.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
            dispatch(listener, "onInteractBlock", blockClick);
            verify(interactions).attack(player, frame);
            clearInvocations(interactions);

            var rotate = new MythicFurnitureRotateEvent(player, context, handle);
            dispatch(listener, "onRotate", rotate);
            assertThat(rotate.isCancelled()).isTrue();

            var remove = new MythicFurnitureRemoveEvent(player, context, handle);
            dispatch(listener, "onRemove", remove);
            assertThat(remove.isCancelled()).isTrue();
            verify(interactions).attack(player, frame);

            var environmental = new MythicFurnitureRemoveEvent(null, context, handle);
            dispatch(listener, "onRemove", environmental);
            assertThat(environmental.isCancelled()).isTrue();

            var entities = hook.spawn(anchor, content, settings);
            when(context.remove(handle, null, false, false)).thenAnswer(invocation -> {
                var cleanup = new MythicFurnitureRemoveEvent(null, context, handle);
                dispatch(listener, "onRemove", cleanup);
                assertThat(cleanup.isCancelled()).isFalse();
                return true;
            });
            hook.despawn(entities);
            verify(context).remove(handle, null, false, false);
        }
    }

    @Test
    void reload_refreshesTheCatalog() throws Exception {
        var manager = mock(PluginManager.class);
        var plugin = mock(Plugin.class);
        var reload = mock(Runnable.class);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            hook.register(plugin, reload);
            var captured = ArgumentCaptor.forClass(Listener.class);
            verify(manager).registerEvents(captured.capture(), eq(plugin));
            dispatch(captured.getValue(), "onReloaded", new MythicCrucibleReloadedEvent(plugin));
            verify(reload).run();
        }
    }

    private static void dispatch(Listener listener, String name, Object event) throws Exception {
        Method method = listener.getClass().getDeclaredMethod(name, event.getClass());
        method.setAccessible(true);
        method.invoke(listener, event);
    }
}
