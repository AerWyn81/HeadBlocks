package fr.aerwyn81.headblocks.services;

import be.seeseemelk.mockbukkit.MockBukkit;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadContent;
import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.hooks.visual.VisualProviderHook;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class HeadCatalogTest {

    private ConfigService configService;
    private HeadVisualService visualService;
    private HeadService headService;
    private org.mockito.MockedStatic<HeadBlocks> headBlocks;
    private org.mockito.MockedStatic<LogUtil> logUtil;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();

        HeadBlocks plugin = mock(HeadBlocks.class);
        when(plugin.getName()).thenReturn("headblocks");
        headBlocks = mockStatic(HeadBlocks.class);
        headBlocks.when(HeadBlocks::getInstance).thenReturn(plugin);
        logUtil = mockStatic(LogUtil.class);

        configService = mock(ConfigService.class);
        LanguageService languageService = mock(LanguageService.class);
        when(languageService.message(anyString())).thenReturn("text");
        when(languageService.messageList(anyString())).thenReturn(new ArrayList<>(List.of("lore")));

        StorageService storageService = mock(StorageService.class);
        HuntService huntService = mock(HuntService.class);
        visualService = mock(HeadVisualService.class);
        when(visualService.iconOf(any())).thenAnswer(invocation -> new ItemStack(Material.PAPER));
        when(visualService.getProviders()).thenReturn(Map.of());

        headService = new HeadService(configService, storageService, languageService, mock(SchedulerAdapter.class),
                mock(PluginProvider.class));
        headService.setHuntService(huntService);
        headService.setHuntConfigService(mock(HuntConfigService.class));
        headService.setVisualService(visualService);
    }

    @AfterEach
    void tearDown() {
        logUtil.close();
        headBlocks.close();
        MockBukkit.unmock();
    }

    private List<HBHead> load(List<?> entries) {
        doReturn(entries).when(configService).headEntries();
        headService.initialize();
        return headService.getHeads();
    }

    @Test
    void vanillaContents_becomeCatalogEntries() {
        var heads = load(List.of("block:lantern", "item:DIAMOND:1001", "mob:cat", "entity:PIG"));

        assertThat(heads).hasSize(4).allMatch(h -> h instanceof HBHeadContent);
        assertThat(heads.get(0).getContent()).isEqualTo(HeadContent.of(ContentKind.BLOCK, "LANTERN", null));
        assertThat(heads.get(1).getContent()).isEqualTo(HeadContent.of(ContentKind.ITEM, "DIAMOND", Map.of("customModelData", 1001)));
        assertThat(heads.get(2).getContent()).isEqualTo(HeadContent.of(ContentKind.MOB, "CAT", null));
        assertThat(heads.get(3).getContent().value()).isEqualTo("PIG");
    }

    @Test
    void catalogItems_carryTheirContentAndTheHeadBlocksTag() {
        var head = load(List.of("mob:cat")).get(0);

        var item = head.getItemStack();
        assertThat(fr.aerwyn81.headblocks.utils.bukkit.HeadUtils.isHeadBlocksItem(item)).isTrue();
        assertThat(fr.aerwyn81.headblocks.utils.bukkit.HeadUtils.getContent(item)).isEqualTo(head.getContent());
    }

    @Test
    void textAndFrameEntries_becomeCatalogEntries() {
        var heads = load(List.of("text:&6Find me[billboard=fixed]", "frame:DIAMOND:12", "frame:NOT_AN_ITEM"));

        assertThat(heads).hasSize(2);
        assertThat(heads.get(0).getContent()).isEqualTo(HeadContent.of(ContentKind.TEXT, "&6Find me", Map.of("billboard", "fixed")));
        assertThat(heads.get(1).getContent()).isEqualTo(HeadContent.of(ContentKind.FRAME, "DIAMOND", Map.of("customModelData", 12)));
    }

    @Test
    void mobEquipment_mustBeItems() {
        var heads = load(List.of("mob:ZOMBIE[head=DIAMOND_HELMET,baby=true]", "mob:ZOMBIE[hand=NOT_AN_ITEM]"));

        assertThat(heads).hasSize(1);
        assertThat(heads.get(0).getContent()).isEqualTo(
                HeadContent.of(ContentKind.MOB, "ZOMBIE", Map.of("head", "DIAMOND_HELMET", "baby", "true")));
    }

    @Test
    void externalEntries_keepTheirOptions() {
        VisualProviderHook nexo = mock(VisualProviderHook.class);
        when(visualService.getProvider("nexo")).thenReturn(nexo);
        when(nexo.isReady()).thenReturn(true);
        when(nexo.exists("tree")).thenReturn(true);

        var heads = load(List.of("nexo:tree[scale=2]"));

        assertThat(heads.get(0).getContent()).isEqualTo(HeadContent.external("nexo", "tree", Map.of("scale", "2")));
    }

    @Test
    void externalEntries_waitForTheirPluginToLoad() {
        VisualProviderHook nexo = mock(VisualProviderHook.class);
        when(visualService.getProvider("nexo")).thenReturn(nexo);
        when(nexo.exists("tree")).thenReturn(true);

        assertThat(load(List.of("nexo:tree", "block:STONE"))).hasSize(1);
        verify(nexo, never()).exists(anyString());
        logUtil.verify(() -> LogUtil.error(anyString(), any(Object[].class)), never());

        when(nexo.isReady()).thenReturn(true);
        headService.reloadCatalog();

        assertThat(headService.getHeads()).hasSize(2);
        assertThat(headService.getHeads().get(0).getContent()).isEqualTo(HeadContent.external("nexo", "tree", null));
    }

    @Test
    void reloadCatalog_keepsThePlacedHeads() {
        load(List.of("block:STONE"));
        var placed = headService.getHeadLocations().size();

        doReturn(List.of("block:STONE", "mob:CAT")).when(configService).headEntries();
        headService.reloadCatalog();

        assertThat(headService.getHeads()).hasSize(2);
        assertThat(headService.getHeadLocations()).hasSize(placed);
    }

    @Test
    void mapEntries_areNotSupported() {
        var heads = load(List.of(Map.of("type", "mob", "value", "CAT"), "block:STONE"));

        assertThat(heads).hasSize(1);
        assertThat(heads.get(0).getContent().value()).isEqualTo("STONE");
    }

    @Test
    void invalidEntries_areSkipped() {
        var heads = load(List.of(
                "block:DIAMOND",
                "block:NOT_A_BLOCK",
                "item:NOT_AN_ITEM",
                "item:DIAMOND:abc",
                "mob:ARROW",
                "mob:DRAGONFLY",
                "noSeparator",
                "block:",
                42,
                "unknown:thing",
                "block:STONE"));

        assertThat(heads).hasSize(1);
        assertThat(heads.get(0).getContent().value()).isEqualTo("STONE");
    }

    @Test
    void externalEntries_needTheirPlugin() {
        VisualProviderHook nexo = mock(VisualProviderHook.class);
        when(visualService.getProviders()).thenReturn(Map.of("nexo", nexo, "mythicmobs", mock(VisualProviderHook.class)));
        when(visualService.getProvider("nexo")).thenReturn(nexo);
        when(nexo.isReady()).thenReturn(true);
        when(nexo.exists("tree")).thenReturn(true);

        var heads = load(List.of("nexo:tree", "nexo:unknown", "mythicmobs:boss"));

        assertThat(heads).hasSize(1);
        assertThat(heads.get(0).getContent()).isEqualTo(HeadContent.external("nexo", "tree", null));
    }

    @Test
    void theme_overridesTheCatalog() {
        when(configService.headsThemeEnabled()).thenReturn(true);
        when(configService.headsThemeSelected()).thenReturn("xmas");
        doReturn(new java.util.HashMap<>(Map.of("xmas", List.of("block:SPRUCE_SAPLING")))).when(configService).headsThemeEntries();

        var heads = load(List.of("block:STONE"));

        assertThat(heads).hasSize(1);
        assertThat(heads.get(0).getContent().value()).isEqualTo("SPRUCE_SAPLING");
    }

    @Test
    void unstableBlocks_areRefused() {
        try (var bukkit = mockStatic(org.bukkit.Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> org.bukkit.Bukkit.createBlockData(Material.OAK_DOOR)).thenReturn(mock(org.bukkit.block.data.type.Door.class));
            bukkit.when(() -> org.bukkit.Bukkit.createBlockData(Material.RED_BED)).thenReturn(mock(org.bukkit.block.data.type.Bed.class));
            bukkit.when(() -> org.bukkit.Bukkit.createBlockData(Material.LANTERN)).thenReturn(mock(org.bukkit.block.data.type.Lantern.class));

            var heads = load(List.of("block:SAND", "block:GRAVEL", "block:OAK_DOOR", "block:RED_BED", "block:LANTERN"));

            assertThat(heads).hasSize(1);
            assertThat(heads.get(0).getContent().value()).isEqualTo("LANTERN");
        }
    }
}
