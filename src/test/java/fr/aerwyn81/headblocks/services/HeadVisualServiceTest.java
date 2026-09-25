package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.head.visual.RenderMode;
import fr.aerwyn81.headblocks.data.head.visual.VisualForm;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.hooks.visual.VisualProviderHook;
import fr.aerwyn81.headblocks.platform.Platform;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeadVisualServiceTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private ConfigService configService;

    @Mock
    private HuntService huntService;

    @Mock
    private HeadService headService;

    @Mock
    private HuntConfigService huntConfigService;

    @Mock
    private SchedulerAdapter scheduler;

    private HeadVisualService visualService;
    private HBHunt hunt;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getConfigService()).thenReturn(configService);
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getHuntConfigService()).thenReturn(huntConfigService);
        lenient().when(registry.getScheduler()).thenReturn(scheduler);
        lenient().when(configService.renderingMode()).thenReturn(RenderMode.BLOCK);

        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(scheduler).runTask(any(Location.class), any(Runnable.class));
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(scheduler).runTask(any(Runnable.class));
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(scheduler).runTaskLater(any(Location.class), any(Runnable.class), anyLong());

        hunt = new HBHunt(configService, "halloween", "Halloween", HuntState.ACTIVE, 1, "STONE");
        lenient().when(huntService.configOf(any())).thenAnswer(invocation ->
                "halloween".equals(invocation.getArgument(0)) ? hunt.getConfig() : new HuntConfig(configService));

        visualService = new HeadVisualService(registry, Map.of());
    }

    private HeadLocation head(HeadContent content, Location location) {
        var head = new HeadLocation("", UUID.randomUUID(), "halloween", "world", 0.5, 64, 0.5, -1, false, false, new ArrayList<>());
        head.setContent(content);
        if (location != null) {
            head.setLocation(location);
        }
        return head;
    }

    @Nested
    class Usage {

        private HeadLocation placed(HeadContent content, RenderMode mode) {
            var head = head(content, null);
            head.setRenderMode(mode);
            return head;
        }

        @Test
        void visualTypes_nameEveryRenderInUse() {
            var heads = new ArrayList<>(List.of(
                    placed(null, null),
                    placed(HeadContent.head("t"), RenderMode.BLOCK),
                    placed(HeadContent.head("t"), RenderMode.DISPLAY),
                    placed(HeadContent.of(ContentKind.BLOCK, "STONE", null), RenderMode.BLOCK),
                    placed(HeadContent.of(ContentKind.BLOCK, "STONE", null), RenderMode.DISPLAY),
                    placed(HeadContent.of(ContentKind.ITEM, "DIAMOND", null), RenderMode.BLOCK),
                    placed(HeadContent.of(ContentKind.TEXT, "hi", null), RenderMode.BLOCK),
                    placed(HeadContent.of(ContentKind.FRAME, "DIAMOND", null), RenderMode.BLOCK),
                    placed(HeadContent.of(ContentKind.MOB, "CAT", null), RenderMode.BLOCK),
                    placed(HeadContent.external("fancynpcs", "Notch", null), RenderMode.BLOCK),
                    placed(HeadContent.external("mythicmobs", "Boss", null), RenderMode.DISPLAY)));
            when(headService.getChargedHeadLocations()).thenReturn(heads);

            assertThat(visualService.visualTypesInUse()).containsExactlyInAnyOrder(
                    "Head", "Head (display)", "Block", "Block (display)", "Item", "Text", "Frame", "Mob",
                    "FancyNpcs", "MythicMobs");
        }

        @Test
        void visualTypes_withoutHeads_isEmpty() {
            when(headService.getChargedHeadLocations()).thenReturn(new ArrayList<>());

            assertThat(visualService.visualTypesInUse()).isEmpty();
        }
    }

    @Nested
    class Resolution {

        @Test
        void renderMode_isTheOneStoredOnTheHead() {
            hunt.getConfig().setRenderMode(RenderMode.BLOCK);
            var head = head(null, null);
            head.setRenderMode(RenderMode.DISPLAY);

            assertThat(visualService.renderModeOf(head)).isEqualTo(RenderMode.DISPLAY);
        }

        @Test
        void renderMode_ofALegacyHead_isBlock() {
            hunt.getConfig().setRenderMode(RenderMode.DISPLAY);

            assertThat(visualService.renderModeOf(head(null, null))).isEqualTo(RenderMode.BLOCK);
        }

        @Test
        void huntModeChange_doesNotChangeHeadsAlreadyPlaced() {
            var head = head(HeadContent.head("t"), null);
            head.setRenderMode(RenderMode.BLOCK);

            hunt.getConfig().setRenderMode(RenderMode.DISPLAY);

            assertThat(visualService.formOf(head)).isEqualTo(VisualForm.HEAD_BLOCK);
        }

        @Test
        void form_mobIsAlwaysAnEntity() {
            var head = head(HeadContent.of(ContentKind.MOB, "CAT", null), null);

            assertThat(visualService.formOf(head)).isEqualTo(VisualForm.MOB);
            assertThat(visualService.isEntityRendered(head)).isTrue();
            assertThat(visualService.isBlockRendered(head)).isFalse();
        }

        @Test
        void form_forContentAndHunt() {
            hunt.getConfig().setRenderMode(RenderMode.DISPLAY);

            assertThat(visualService.formOf(HeadContent.head("t"), "halloween")).isEqualTo(VisualForm.ITEM_DISPLAY);
            assertThat(visualService.formOf(HeadContent.of(ContentKind.BLOCK, "LANTERN", null), "unknown"))
                    .isEqualTo(VisualForm.BLOCK);
        }

        @Test
        void provider_onlyWhenAvailable() {
            VisualProviderHook available = mock(VisualProviderHook.class);
            VisualProviderHook missing = mock(VisualProviderHook.class);
            when(available.isAvailable()).thenReturn(true);
            when(missing.isAvailable()).thenReturn(false);
            var service = new HeadVisualService(registry, Map.of("nexo", available, "mythicmobs", missing));

            assertThat(service.getProvider("nexo")).isSameAs(available);
            assertThat(service.getProvider("mythicmobs")).isNull();
            assertThat(service.getProvider("other")).isNull();
        }
    }

    @Nested
    class Lifecycle {

        private World world;
        private Location location;
        private HeadLocation mob;
        private MockedStatic<HeadBlocks> headBlocks;

        @BeforeEach
        void setUpWorld() {
            HeadBlocks plugin = mock(HeadBlocks.class);
            lenient().when(plugin.getName()).thenReturn("HeadBlocks");
            headBlocks = mockStatic(HeadBlocks.class);
            headBlocks.when(HeadBlocks::getInstance).thenReturn(plugin);

            world = mock(World.class);
            lenient().when(world.getName()).thenReturn("world");
            lenient().when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
            location = mock(Location.class);
            lenient().when(location.getWorld()).thenReturn(world);
            lenient().when(location.clone()).thenReturn(location);

            mob = head(HeadContent.of(ContentKind.MOB, "CAT", null), location);
            lenient().when(headService.getHeadByUUID(mob.getUuid())).thenReturn(mob);
            lenient().when(registry.getVisibilityService()).thenReturn(mock(HeadVisibilityService.class));
            lenient().when(registry.getPlatform()).thenReturn(mock(Platform.class));

            lenient().doAnswer(invocation -> {
                ((Runnable) invocation.getArgument(1)).run();
                return null;
            }).when(scheduler).runNow(any(Location.class), any(Runnable.class));
            lenient().doAnswer(invocation -> {
                ((Runnable) invocation.getArgument(1)).run();
                return null;
            }).when(scheduler).runNow(any(Entity.class), any(Runnable.class));
        }

        @AfterEach
        void closeStatics() {
            headBlocks.close();
        }

        private Cat spawnableCat() {
            Cat cat = mock(Cat.class);
            lenient().when(cat.isValid()).thenReturn(true);
            lenient().when(cat.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
            lenient().when(cat.getWorld()).thenReturn(world);
            lenient().when(cat.getLocation()).thenReturn(location);
            return cat;
        }

        @Test
        void ensureSpawned_spawnsTagsAndTracksTheMob() {
            Cat cat = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(cat);

            visualService.ensureSpawned(mob);

            assertThat(visualService.entitiesOf(mob)).containsExactly(cat);
            verify(cat).setAI(false);
            verify(cat, never()).setInvulnerable(anyBoolean());
            verify(cat).setPersistent(false);
            verify(registry.getVisibilityService()).onSpawned(eq(mob), anyList());
        }

        @Test
        void ensureSpawned_mob_refreshesItsHologram() {
            Cat cat = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(cat);
            HologramService hologramService = mock(HologramService.class);
            when(registry.getHologramService()).thenReturn(hologramService);
            when(hologramService.isEnabled()).thenReturn(true);

            visualService.ensureSpawned(mob);

            verify(hologramService).removeHolograms(location);
            verify(hologramService).createHolograms(eq(location), any());
        }

        @Test
        void conversion_isSpreadOverTicks() {
            ArrayList<HeadLocation> heads = new ArrayList<>();
            for (int i = 0; i < 45; i++) {
                heads.add(head(HeadContent.of(ContentKind.MOB, "CAT", null), location));
            }
            when(headService.getHeadLocationsForHunt(hunt)).thenReturn(heads);

            visualService.convertHunt(hunt, RenderMode.DISPLAY, report -> {
            });

            verify(scheduler, times(20)).runTaskLater(any(Location.class), any(Runnable.class), eq(0L));
            verify(scheduler, times(20)).runTaskLater(any(Location.class), any(Runnable.class), eq(1L));
            verify(scheduler, times(5)).runTaskLater(any(Location.class), any(Runnable.class), eq(2L));
        }

        @Test
        void ensureSpawned_mob_hologramServiceInactive_createsNoHologram() {
            Cat cat = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(cat);
            HologramService hologramService = mock(HologramService.class);
            when(registry.getHologramService()).thenReturn(hologramService);
            when(hologramService.isEnabled()).thenReturn(false);

            visualService.ensureSpawned(mob);

            verify(hologramService, never()).createHolograms(any(), any());
        }

        @Test
        void ensureSpawned_alreadyAlive_doesNotSpawnTwice() {
            Cat cat = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(cat);

            visualService.ensureSpawned(mob);
            visualService.ensureSpawned(mob);

            verify(world, times(1)).spawnEntity(location, EntityType.CAT);
        }

        @Test
        void tick_respawnsAKilledMob() {
            Cat first = spawnableCat();
            Cat second = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(first, second);
            visualService.ensureSpawned(mob);

            when(first.isValid()).thenReturn(false);
            visualService.tick();

            assertThat(visualService.entitiesOf(mob)).containsExactly(second);
        }

        @Test
        void tick_withNothingSpawned_touchesNoHead() {
            visualService.tick();

            verify(headService, never()).getHeadByUUID(any());
            verify(headService, never()).getHeadLocations();
        }

        @Test
        void failedSpawn_isNotRetriedImmediately() {
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(null);

            try (var ignored = mockStatic(fr.aerwyn81.headblocks.utils.internal.LogUtil.class)) {
                visualService.ensureSpawned(mob);
                visualService.tick();
                visualService.ensureSpawned(mob);
            }

            verify(world, times(1)).spawnEntity(location, EntityType.CAT);
        }

        @Test
        void chunkLoad_onlyLooksAtTheHeadsOfThatChunk() {
            Chunk chunk = mock(Chunk.class);
            when(chunk.getWorld()).thenReturn(world);
            when(chunk.getX()).thenReturn(3);
            when(chunk.getZ()).thenReturn(-2);
            when(headService.getHeadsInChunk("world", 3, -2)).thenReturn(List.of(mob));
            Cat cat = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(cat);

            visualService.onChunkLoad(chunk);

            verify(headService, never()).getHeadLocations();
            assertThat(visualService.entitiesOf(mob)).hasSize(1);
        }

        @Test
        void chunkUnloaded_dropsTheTrackedEntities() {
            Cat cat = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(cat);
            visualService.ensureSpawned(mob);

            when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
            visualService.ensureSpawned(mob);

            assertThat(visualService.entitiesOf(mob)).isEmpty();
        }

        @Test
        void spin_delegatesToTheRendererWithAGrowingAngle() {
            Cat cat = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(cat);
            mob.setYaw(10f);
            visualService.ensureSpawned(mob);

            visualService.spin(mob, 20);
            visualService.spin(mob, 20);

            verify(cat).setRotation(32.5f, 0);
            verify(cat).setRotation(55f, 0);
        }

        @Test
        void visualHeight_followsTheForm() {
            Cat cat = spawnableCat();
            when(cat.getHeight()).thenReturn(0.7);
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(cat);
            visualService.ensureSpawned(mob);

            assertThat(visualService.visualHeight(mob)).isEqualTo(0.7);
            assertThat(visualService.visualHeight(head(HeadContent.head("t"), location))).isEqualTo(0.5);
            hunt.getConfig().setRenderScale(4.0);
            var display = head(HeadContent.head("t"), location);
            display.setRenderMode(RenderMode.DISPLAY);
            assertThat(visualService.visualHeight(display)).isEqualTo(2.0);
            assertThat(visualService.visualHeight(head(HeadContent.external("nexo", "tree", null), location))).isEqualTo(1.0);
        }

        @Test
        void headOf_readsTheTag() {
            Cat cat = spawnableCat();
            PersistentDataContainer pdc = cat.getPersistentDataContainer();
            when(pdc.get(any(), eq(org.bukkit.persistence.PersistentDataType.STRING))).thenReturn(mob.getUuid().toString());
            when(pdc.has(any(), eq(org.bukkit.persistence.PersistentDataType.STRING))).thenReturn(true);

            assertThat(visualService.isHeadEntity(cat)).isTrue();
            assertThat(visualService.headOf(cat)).isSameAs(mob);
            assertThat(visualService.headOf(null)).isNull();
            assertThat(visualService.isHeadEntity(null)).isFalse();
        }

        @Test
        void headOf_malformedTag_isNull() {
            Cat cat = spawnableCat();
            when(cat.getPersistentDataContainer().get(any(), eq(org.bukkit.persistence.PersistentDataType.STRING))).thenReturn("nope");

            assertThat(visualService.headOf(cat)).isNull();
        }

        @Test
        void removeOrphan_removesUntrackedTaggedEntities() {
            Cat tracked = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(tracked);
            visualService.ensureSpawned(mob);
            Cat orphan = spawnableCat();
            for (var cat : List.of(tracked, orphan)) {
                var pdc = cat.getPersistentDataContainer();
                when(pdc.has(any(), eq(org.bukkit.persistence.PersistentDataType.STRING))).thenReturn(true);
                when(pdc.get(any(), eq(org.bukkit.persistence.PersistentDataType.STRING))).thenReturn(mob.getUuid().toString());
            }

            visualService.removeOrphan(tracked);
            visualService.removeOrphan(orphan);
            visualService.removeOrphan(spawnableCat());

            verify(tracked, never()).remove();
            verify(orphan).remove();
        }

        @Test
        void despawnAll_andShutdown_forgetEverything() {
            Cat cat = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(cat);
            visualService.ensureSpawned(mob);

            visualService.despawnAll();
            assertThat(visualService.entitiesOf(mob)).isEmpty();

            visualService.ensureSpawned(mob);
            visualService.shutdown();
            assertThat(visualService.entitiesOf(mob)).isEmpty();
            verify(cat, times(2)).remove();
        }

        @Test
        void respawn_replacesTheEntities() {
            Cat first = spawnableCat();
            Cat second = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(first, second);
            visualService.ensureSpawned(mob);

            visualService.respawn(mob);

            verify(first).remove();
            assertThat(visualService.entitiesOf(mob)).containsExactly(second);
        }

        @Test
        void spawnLoaded_spawnsEntityHeadsOnly() {
            var blockHead = head(HeadContent.head("t"), location);
            when(headService.getHeadLocations()).thenReturn(List.of(mob, blockHead));
            Cat cat = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(cat);

            visualService.spawnLoaded();

            assertThat(visualService.entitiesOf(mob)).containsExactly(cat);
            assertThat(visualService.entitiesOf(blockHead)).isEmpty();
        }

        @Test
        void tick_forgetsHeadsThatNoLongerExist() {
            Cat cat = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(cat);
            visualService.ensureSpawned(mob);
            when(headService.getHeadByUUID(mob.getUuid())).thenReturn(null);

            visualService.tick();

            verify(cat).remove();
            assertThat(visualService.entitiesOf(mob)).isEmpty();
        }

        @Test
        void externalHead_withoutItsPlugin_isNotSpawned() {
            var external = head(HeadContent.external("nexo", "tree", null), location);

            try (var ignored = mockStatic(fr.aerwyn81.headblocks.utils.internal.LogUtil.class)) {
                visualService.ensureSpawned(external);
            }

            assertThat(visualService.entitiesOf(external)).isEmpty();
        }

        @Test
        void clear_blockHead_removesTheBlock() {
            Block block = mock(Block.class);
            when(location.getBlock()).thenReturn(block);

            visualService.clear(head(HeadContent.head("t"), location));

            verify(block).setType(Material.AIR);
        }

        @Test
        void lookedAtHead_usesARayTraceOnTaggedEntities() {
            Player player = mock(Player.class);
            Location eye = mock(Location.class);
            when(player.getEyeLocation()).thenReturn(eye);
            when(player.getWorld()).thenReturn(world);
            Cat cat = spawnableCat();
            when(cat.getPersistentDataContainer().get(any(), eq(org.bukkit.persistence.PersistentDataType.STRING))).thenReturn(mob.getUuid().toString());
            when(world.rayTrace(eq(eye), any(), eq(10.0), any(), eq(true), eq(0.1), any()))
                    .thenReturn(new org.bukkit.util.RayTraceResult(new org.bukkit.util.Vector(), cat));

            assertThat(visualService.lookedAtHead(player, 10)).isSameAs(mob);
        }

        @Test
        void lookedAtHead_nothingHit_isNull() {
            Player player = mock(Player.class);
            when(player.getWorld()).thenReturn(world);
            when(player.getEyeLocation()).thenReturn(mock(Location.class));

            assertThat(visualService.lookedAtHead(player, 10)).isNull();
        }

        @Test
        void ensureContent_legacyHead_readsTheBlockThenTheDatabase() throws Exception {
            Block block = mock(Block.class);
            when(location.getBlock()).thenReturn(block);
            StorageService storageService = mock(StorageService.class);
            when(registry.getStorageService()).thenReturn(storageService);
            var legacy = head(null, location);

            try (var headUtils = mockStatic(fr.aerwyn81.headblocks.utils.bukkit.HeadUtils.class)) {
                headUtils.when(() -> fr.aerwyn81.headblocks.utils.bukkit.HeadUtils.isPlayerHead(block)).thenReturn(false);
                when(storageService.getHeadTexture(legacy.getUuid())).thenReturn("from-db");

                assertThat(visualService.ensureContent(legacy)).isEqualTo(HeadContent.head("from-db"));
            }

            verify(headService).saveHeadInConfig(legacy);
        }

        @Test
        void ensureContent_noTextureAnywhere_isNull() throws Exception {
            StorageService storageService = mock(StorageService.class);
            when(registry.getStorageService()).thenReturn(storageService);
            when(storageService.getHeadTexture(any())).thenThrow(new fr.aerwyn81.headblocks.utils.internal.InternalException("down"));
            var legacy = head(null, null);

            assertThat(visualService.ensureContent(legacy)).isNull();
        }

        @Test
        void convert_blockHeadToDisplay_removesTheBlockAndSpawns() {
            Block block = mock(Block.class);
            when(location.getBlock()).thenReturn(block);
            var head = head(HeadContent.head("t"), location);
            when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>(List.of(head)));
            org.bukkit.entity.ItemDisplay display = mock(org.bukkit.entity.ItemDisplay.class);
            org.bukkit.entity.Interaction interaction = mock(org.bukkit.entity.Interaction.class);
            for (Entity entity : List.of(display, interaction)) {
                when(entity.isValid()).thenReturn(true);
                when(entity.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
            }
            when(world.spawn(location, org.bukkit.entity.ItemDisplay.class)).thenReturn(display);
            when(world.spawn(location, org.bukkit.entity.Interaction.class)).thenReturn(interaction);
            AtomicReference<HeadVisualService.ConversionReport> report = new AtomicReference<>();

            try (var headUtils = mockStatic(fr.aerwyn81.headblocks.utils.bukkit.HeadUtils.class)) {
                headUtils.when(() -> fr.aerwyn81.headblocks.utils.bukkit.HeadUtils.isPlayerHead(block)).thenReturn(true);
                visualService.convertHunt(hunt, RenderMode.DISPLAY, report::set);
            }

            verify(block).setType(Material.AIR);
            assertThat(visualService.entitiesOf(head)).containsExactly(display, interaction);
            assertThat(report.get().converted()).isEqualTo(1);
            assertThat(head.getRenderMode()).isEqualTo(RenderMode.DISPLAY);
        }

        @Test
        void despawn_removesTheEntities() {
            Cat cat = spawnableCat();
            when(world.spawnEntity(location, EntityType.CAT)).thenReturn(cat);
            visualService.ensureSpawned(mob);

            visualService.despawn(mob);

            verify(cat).remove();
            assertThat(visualService.entitiesOf(mob)).isEmpty();
        }

        private Entity liveEntity() {
            Entity entity = mock(Entity.class);
            lenient().when(entity.isValid()).thenReturn(true);
            lenient().when(entity.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
            return entity;
        }

        private VisualProviderHook provider() {
            VisualProviderHook provider = mock(VisualProviderHook.class);
            lenient().when(provider.isAvailable()).thenReturn(true);
            lenient().when(provider.isReady()).thenReturn(true);
            visualService = new HeadVisualService(registry, Map.of("fake", provider));
            return provider;
        }

        private HeadLocation npcHead() {
            var npc = head(HeadContent.external("fake", "Notch", null), location);
            lenient().when(headService.getHeadByUUID(npc.getUuid())).thenReturn(npc);
            return npc;
        }

        @Test
        void provider_notReady_waitsSilentlyThenSpawnsOnRetry() {
            var provider = provider();
            var npc = npcHead();
            var entity = liveEntity();
            when(provider.isReady()).thenReturn(false);

            try (var logUtil = mockStatic(fr.aerwyn81.headblocks.utils.internal.LogUtil.class)) {
                visualService.ensureSpawned(npc);
                visualService.tick();

                verify(provider, never()).spawn(any(), any(), any());
                logUtil.verify(() -> fr.aerwyn81.headblocks.utils.internal.LogUtil.error(anyString(), any(Object[].class)), never());
            }

            when(provider.isReady()).thenReturn(true);
            when(provider.spawn(any(), any(), any())).thenReturn(List.of(entity));
            visualService.retryNow();
            visualService.tick();

            assertThat(visualService.entitiesOf(npc)).containsExactly(entity);
        }

        @Test
        void unloadedChunk_releasesTheRenderThroughItsProvider() {
            var provider = provider();
            var npc = npcHead();
            var entity = liveEntity();
            when(provider.spawn(any(), any(), any())).thenReturn(List.of(entity));
            visualService.ensureSpawned(npc);

            when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
            visualService.ensureSpawned(npc);

            verify(provider).despawn(List.of(entity));
            assertThat(visualService.entitiesOf(npc)).isEmpty();
        }

        @Test
        void deadRender_isReleasedThroughItsProviderBeforeRespawning() {
            var provider = provider();
            var npc = npcHead();
            var first = liveEntity();
            var second = liveEntity();
            when(provider.spawn(any(), any(), any())).thenReturn(List.of(first), List.of(second));
            visualService.ensureSpawned(npc);

            when(first.isValid()).thenReturn(false);
            visualService.tick();

            verify(provider).despawn(List.of(first));
            assertThat(visualService.entitiesOf(npc)).containsExactly(second);
        }

        @Test
        void removedRender_isReleasedWithoutTheEntityScheduler() {
            var provider = provider();
            var npc = npcHead();
            var gone = liveEntity();
            when(provider.spawn(any(), any(), any())).thenReturn(List.of(gone));
            visualService.ensureSpawned(npc);
            lenient().doNothing().when(scheduler).runNow(any(Entity.class), any(Runnable.class));

            when(gone.isValid()).thenReturn(false);
            visualService.despawn(npc);

            verify(scheduler, never()).runNow(eq(gone), any(Runnable.class));
            verify(provider).despawn(List.of(gone));
        }

        @Test
        void partialRender_isReleasedAndReported() {
            var provider = provider();
            var npc = npcHead();
            var alive = liveEntity();
            var dead = liveEntity();
            when(dead.isValid()).thenReturn(false);
            when(provider.spawn(any(), any(), any())).thenReturn(List.of(alive, dead));

            try (var ignored = mockStatic(fr.aerwyn81.headblocks.utils.internal.LogUtil.class)) {
                visualService.ensureSpawned(npc);
            }

            verify(provider).despawn(List.of(alive, dead));
            assertThat(visualService.entitiesOf(npc)).isEmpty();
        }

        @Test
        void visibility_reachesTheRenderer() {
            var provider = provider();
            var npc = npcHead();
            var entity = liveEntity();
            Player player = mock(Player.class);
            when(provider.spawn(any(), any(), any())).thenReturn(List.of(entity));
            visualService.ensureSpawned(npc);

            visualService.setVisible(player, npc, false);
            visualService.setVisible(player, mob, true);

            verify(provider).setVisible(player, List.of(entity), false);
            verify(provider, never()).setVisible(player, List.of(entity), true);
        }

        @Test
        void shutdownAndDespawnAll_releaseTheProviderRenders() {
            var provider = provider();
            var npc = npcHead();
            var first = liveEntity();
            var second = liveEntity();
            when(provider.spawn(any(), any(), any())).thenReturn(List.of(first), List.of(second));

            visualService.ensureSpawned(npc);
            visualService.despawnAll();
            visualService.ensureSpawned(npc);
            visualService.shutdown();

            verify(provider).despawn(List.of(first));
            verify(provider).despawn(List.of(second));
            assertThat(visualService.entitiesOf(npc)).isEmpty();
        }
    }

    @Nested
    class Conversion {

        @Test
        void emptyHunt_onlyStoresTheMode() {
            when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>());
            AtomicReference<HeadVisualService.ConversionReport> report = new AtomicReference<>();

            visualService.convertHunt(hunt, RenderMode.DISPLAY, report::set);

            assertThat(hunt.getConfig().getRenderMode()).isEqualTo(RenderMode.DISPLAY);
            verify(huntConfigService).saveHunt(hunt);
            assertThat(report.get().converted()).isZero();
        }

        @Test
        void unloadedWorld_keepsTheHeadAsBefore() {
            var head = head(HeadContent.head("t"), null);
            head.setRenderMode(RenderMode.BLOCK);
            when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>(List.of(head)));
            AtomicReference<HeadVisualService.ConversionReport> report = new AtomicReference<>();

            visualService.convertHunt(hunt, RenderMode.DISPLAY, report::set);

            assertThat(head.getRenderMode()).isEqualTo(RenderMode.BLOCK);
            assertThat(visualService.formOf(head)).isEqualTo(VisualForm.HEAD_BLOCK);
            assertThat(report.get().skipped()).isEqualTo(1);
            assertThat(hunt.getConfig().getRenderMode()).isEqualTo(RenderMode.DISPLAY);
        }

        @Test
        void mobs_areUnchangedButFollowTheNewMode() {
            var location = loadedLocation(mock(Block.class));
            var head = head(HeadContent.of(ContentKind.MOB, "CAT", null), location);
            head.setRenderMode(RenderMode.BLOCK);
            when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>(List.of(head)));
            AtomicReference<HeadVisualService.ConversionReport> report = new AtomicReference<>();

            visualService.convertHunt(hunt, RenderMode.DISPLAY, report::set);

            assertThat(report.get().unchanged()).isEqualTo(1);
            assertThat(head.getRenderMode()).isEqualTo(RenderMode.DISPLAY);
            verify(headService).saveHeadInConfig(head);
        }

        @Test
        void displayToBlock_occupiedSpot_staysADisplay() {
            Block block = mock(Block.class);
            when(block.isEmpty()).thenReturn(false);
            when(block.getType()).thenReturn(Material.STONE);
            var head = head(HeadContent.of(ContentKind.BLOCK, "LANTERN", null), loadedLocation(block));
            head.setRenderMode(RenderMode.DISPLAY);
            when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>(List.of(head)));
            AtomicReference<HeadVisualService.ConversionReport> report = new AtomicReference<>();

            visualService.convertHunt(hunt, RenderMode.BLOCK, report::set);

            assertThat(report.get().failed()).isEqualTo(1);
            assertThat(head.getRenderMode()).isEqualTo(RenderMode.DISPLAY);
            assertThat(visualService.formOf(head)).isEqualTo(VisualForm.BLOCK_DISPLAY);
            verify(block, never()).setBlockData(any());
            assertThat(hunt.getConfig().getRenderMode()).isEqualTo(RenderMode.BLOCK);
        }

        @Test
        void failedHead_isConvertedByTheNextRunOnceTheSpotIsFree() {
            Block block = mock(Block.class);
            when(block.isEmpty()).thenReturn(true);
            var head = head(HeadContent.of(ContentKind.BLOCK, "LANTERN", Map.of("data", "minecraft:lantern")), loadedLocation(block));
            head.setRenderMode(RenderMode.DISPLAY);
            hunt.getConfig().setRenderMode(RenderMode.BLOCK);
            when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>(List.of(head)));
            AtomicReference<HeadVisualService.ConversionReport> report = new AtomicReference<>();

            try (var bukkit = mockStatic(org.bukkit.Bukkit.class)) {
                bukkit.when(() -> org.bukkit.Bukkit.createBlockData("minecraft:lantern")).thenReturn(mock(org.bukkit.block.data.BlockData.class));

                visualService.convertHunt(hunt, RenderMode.BLOCK, report::set);
            }

            assertThat(report.get().converted()).isEqualTo(1);
            assertThat(head.getRenderMode()).isEqualTo(RenderMode.BLOCK);
        }

        @Test
        void rerender_sameForm_doesNothing() {
            var head = head(HeadContent.of(ContentKind.MOB, "CAT", null), null);
            head.setRenderMode(RenderMode.BLOCK);
            hunt.getConfig().setRenderMode(RenderMode.DISPLAY);

            visualService.rerender(head);

            assertThat(head.getRenderMode()).isEqualTo(RenderMode.BLOCK);
            verify(headService, never()).saveHeadInConfig(any());
        }

        @Test
        void rerender_unloadedWorld_keepsThePreviousLook() {
            hunt.getConfig().setRenderMode(RenderMode.DISPLAY);
            var head = head(HeadContent.head("t"), null);
            head.setRenderMode(RenderMode.BLOCK);

            visualService.rerender(head);

            assertThat(visualService.formOf(head)).isEqualTo(VisualForm.HEAD_BLOCK);
            verifyNoInteractions(scheduler);
        }

        @Test
        void rerender_toBlock_placesTheBlock() {
            Block block = mock(Block.class);
            when(block.isEmpty()).thenReturn(true);
            var head = head(HeadContent.of(ContentKind.BLOCK, "LANTERN", Map.of("data", "minecraft:lantern[hanging=false]")), loadedLocation(block));
            head.setRenderMode(RenderMode.DISPLAY);

            try (var bukkit = mockStatic(org.bukkit.Bukkit.class)) {
                var data = mock(org.bukkit.block.data.BlockData.class);
                bukkit.when(() -> org.bukkit.Bukkit.createBlockData("minecraft:lantern[hanging=false]")).thenReturn(data);

                visualService.rerender(head);

                verify(block).setBlockData(data);
            }

            assertThat(head.getRenderMode()).isEqualTo(RenderMode.BLOCK);
            assertThat(visualService.formOf(head)).isEqualTo(VisualForm.BLOCK);
        }

        private Location loadedLocation(Block block) {
            World world = mock(World.class);
            lenient().when(world.getName()).thenReturn("world");
            Location location = mock(Location.class);
            lenient().when(location.getWorld()).thenReturn(world);
            lenient().when(location.getBlock()).thenReturn(block);
            lenient().when(location.getX()).thenReturn(0.5);
            lenient().when(location.getY()).thenReturn(64.0);
            lenient().when(location.getZ()).thenReturn(0.5);
            return location;
        }
    }
}
