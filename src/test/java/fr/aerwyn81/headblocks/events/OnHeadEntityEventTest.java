package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnHeadEntityEventTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HeadVisualService visualService;

    @Mock
    private StorageService storageService;

    @Mock
    private LanguageService languageService;

    @Mock
    private HeadService headService;

    @Mock
    private ConfigService configService;

    @Mock
    private Player player;

    @Mock
    private Entity entity;

    @Mock
    private HeadLocation head;

    @Mock
    private Location location;

    private OnHeadEntityEvent listener;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getVisualService()).thenReturn(visualService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getConfigService()).thenReturn(configService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(head.getLocation()).thenReturn(location);

        listener = new OnHeadEntityEvent(registry);
    }

    @AfterEach
    void tearDown() {
        HeadBlocks.isReloadInProgress = false;
    }

    private void headEntity() {
        when(visualService.isHeadEntity(entity)).thenReturn(true);
        lenient().when(visualService.headOf(entity)).thenReturn(head);
    }

    @Nested
    class Interact {

        private PlayerInteractEntityEvent event(EquipmentSlot hand) {
            PlayerInteractEntityEvent event = mock(PlayerInteractEntityEvent.class);
            when(event.getRightClicked()).thenReturn(entity);
            lenient().when(event.getPlayer()).thenReturn(player);
            lenient().when(event.getHand()).thenReturn(hand);
            return event;
        }

        @Test
        void headEntity_isCancelledAndClaimed() {
            headEntity();
            when(storageService.isStorageError()).thenReturn(true);
            var event = event(EquipmentSlot.HAND);

            listener.onInteract(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.StorageError");
        }

        @Test
        void headEntity_offHand_isCancelledWithoutClaim() {
            headEntity();
            var event = event(EquipmentSlot.OFF_HAND);

            listener.onInteract(event);

            verify(event).setCancelled(true);
            verifyNoInteractions(storageService);
        }

        @Test
        void headEntity_duringReload_isNotClaimed() {
            headEntity();
            HeadBlocks.isReloadInProgress = true;

            listener.onInteract(event(EquipmentSlot.HAND));

            verify(languageService).message("Messages.PluginReloading");
            verifyNoInteractions(storageService);
        }

        @Test
        void otherEntity_withAHeadBlocksSpawnEgg_isCancelled() {
            var event = event(EquipmentSlot.HAND);
            PlayerInventory inventory = mock(PlayerInventory.class);
            ItemStack egg = mock(ItemStack.class);
            when(player.getInventory()).thenReturn(inventory);
            when(inventory.getItemInMainHand()).thenReturn(egg);
            when(egg.getType()).thenReturn(Material.CAT_SPAWN_EGG);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
                headUtils.when(() -> HeadUtils.isHeadBlocksItem(egg)).thenReturn(true);

                listener.onInteract(event);
            }

            verify(event).setCancelled(true);
        }

        @Test
        void otherEntity_withAnyOtherItem_isLeftAlone() {
            var event = event(EquipmentSlot.HAND);
            PlayerInventory inventory = mock(PlayerInventory.class);
            ItemStack head = mock(ItemStack.class);
            when(player.getInventory()).thenReturn(inventory);
            when(inventory.getItemInMainHand()).thenReturn(head);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
                headUtils.when(() -> HeadUtils.isHeadBlocksItem(head)).thenReturn(false);

                listener.onInteract(event);
            }

            verify(event, never()).setCancelled(anyBoolean());
        }

        @Test
        void armorStandManipulation_ofAHeadEntity_isCancelled() {
            PlayerArmorStandManipulateEvent event = mock(PlayerArmorStandManipulateEvent.class);
            when(event.getRightClicked()).thenReturn(mock(ArmorStand.class));
            when(visualService.isHeadEntity(any(ArmorStand.class))).thenReturn(true);

            listener.onArmorStandManipulate(event);

            verify(event).setCancelled(true);
        }
    }

    @Nested
    class Damage {

        private EntityDamageByEntityEvent attackBy(Entity damager) {
            EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
            when(event.getEntity()).thenReturn(entity);
            lenient().when(event.getDamager()).thenReturn(damager);
            return event;
        }

        @Test
        void notAHeadEntity_isIgnored() {
            var event = attackBy(player);

            listener.onDamage(event);

            verify(event, never()).setCancelled(anyBoolean());
        }

        @Test
        void environmentalDamage_isCancelled() {
            headEntity();
            EntityDamageEvent event = mock(EntityDamageEvent.class);
            when(event.getEntity()).thenReturn(entity);

            listener.onDamage(event);

            verify(event).setCancelled(true);
            verifyNoInteractions(storageService);
        }

        @Test
        void survivalPlayer_leftClickClaims() {
            headEntity();
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(storageService.isStorageError()).thenReturn(true);
            var event = attackBy(player);

            listener.onDamage(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.StorageError");
        }

        @Test
        void creativeAdminSneaking_removesTheHead() throws Exception {
            headEntity();
            when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
            when(player.isSneaking()).thenReturn(true);
            when(storageService.isStorageError()).thenReturn(true);

            try (MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

                listener.onDamage(attackBy(player));
            }

            verify(storageService).isStorageError();
            verify(headService, never()).removeHeadLocation(any(), anyBoolean());
        }

        @Test
        void creativeWithoutPermission_doesNothing() throws Exception {
            headEntity();
            when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

            try (MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(false);

                listener.onDamage(attackBy(player));
            }

            verify(headService, never()).removeHeadLocation(any(), anyBoolean());
            verify(storageService, never()).isStorageError();
        }
    }

    @Nested
    class Protections {

        @Test
        void combustion_isCancelled() {
            headEntity();
            EntityCombustEvent event = mock(EntityCombustEvent.class);
            when(event.getEntity()).thenReturn(entity);

            listener.onCombust(event);

            verify(event).setCancelled(true);
        }

        @Test
        void transformation_isCancelled() {
            headEntity();
            EntityTransformEvent event = mock(EntityTransformEvent.class);
            when(event.getEntity()).thenReturn(entity);

            listener.onTransform(event);

            verify(event).setCancelled(true);
        }

        @Test
        void portal_isCancelled() {
            headEntity();
            EntityPortalEvent event = mock(EntityPortalEvent.class);
            when(event.getEntity()).thenReturn(entity);

            listener.onPortal(event);

            verify(event).setCancelled(true);
        }

        @Test
        void targeting_isCancelled() {
            headEntity();
            EntityTargetEvent event = mock(EntityTargetEvent.class);
            when(event.getEntity()).thenReturn(entity);

            listener.onTarget(event);

            verify(event).setCancelled(true);
        }

        @Test
        void otherEntities_areNotProtected() {
            EntityCombustEvent event = mock(EntityCombustEvent.class);
            when(event.getEntity()).thenReturn(entity);

            listener.onCombust(event);

            verify(event, never()).setCancelled(anyBoolean());
        }
    }

    @Nested
    class World {

        @Test
        void chunkLoad_isForwarded() {
            ChunkLoadEvent event = mock(ChunkLoadEvent.class);
            Chunk chunk = mock(Chunk.class);
            when(event.getChunk()).thenReturn(chunk);

            listener.onChunkLoad(event);

            verify(visualService).onChunkLoad(chunk);
        }

        @Test
        void loadedEntities_areCheckedForOrphans() {
            EntitiesLoadEvent event = mock(EntitiesLoadEvent.class);
            Entity other = mock(Entity.class);
            when(event.getEntities()).thenReturn(List.of(entity, other));

            listener.onEntitiesLoad(event);

            verify(visualService).removeOrphan(entity);
            verify(visualService).removeOrphan(other);
        }
    }
}
