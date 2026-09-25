package fr.aerwyn81.headblocks.visual;

import be.seeseemelk.mockbukkit.MockBukkit;
import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.visual.renderers.BlockDisplayRenderer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContentItemsTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void headWithTexture_appliesIt() {
        ItemStack textured = new ItemStack(Material.PLAYER_HEAD);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
            headUtils.when(() -> HeadUtils.applyTextureToItemStack(any(ItemStack.class), eq("abc"))).thenReturn(textured);

            assertThat(ContentItems.itemOf(HeadContent.head("abc"))).isSameAs(textured);
        }
    }

    @Test
    void headWithoutTexture_isAPlainHead() {
        assertThat(ContentItems.itemOf(HeadContent.head("")).getType()).isEqualTo(Material.PLAYER_HEAD);
        assertThat(ContentItems.itemOf(null).getType()).isEqualTo(Material.PLAYER_HEAD);
    }

    @Test
    void headWithOwner_usesThePlayerSkin() {
        var content = new HeadContent(ContentKind.HEAD, "", null, Map.of("owner", UUID.randomUUID().toString()));

        assertThat(ContentItems.itemOf(content).getType()).isEqualTo(Material.PLAYER_HEAD);
    }

    @Test
    void block_item_mob_external_haveTheirIcons() {
        assertThat(ContentItems.itemOf(HeadContent.of(ContentKind.BLOCK, "LANTERN", null)).getType()).isEqualTo(Material.LANTERN);
        assertThat(ContentItems.itemOf(HeadContent.of(ContentKind.MOB, "CAT", null)).getType()).isEqualTo(Material.CAT_SPAWN_EGG);
        assertThat(ContentItems.itemOf(HeadContent.of(ContentKind.MOB, "GIANT", null)).getType()).isEqualTo(Material.PAPER);
        assertThat(ContentItems.itemOf(HeadContent.external("nexo", "tree", null)).getType()).isEqualTo(Material.PAPER);
        assertThat(ContentItems.itemOf(HeadContent.of(ContentKind.ITEM, "NOT_AN_ITEM", null)).getType()).isEqualTo(Material.BARRIER);
    }

    @Test
    void itemWithModelData_setsIt() {
        var item = ContentItems.itemOf(HeadContent.of(ContentKind.ITEM, "DIAMOND", Map.of("customModelData", 42)));

        assertThat(item.getType()).isEqualTo(Material.DIAMOND);
        assertThat(item.getItemMeta().getCustomModelData()).isEqualTo(42);
    }

    @Test
    void blockData_invalidSavedData_fallsBackToTheMaterial() {
        var content = HeadContent.of(ContentKind.BLOCK, "LANTERN", Map.of("data", "not valid data"));
        BlockData lanternData = mock(BlockData.class);

        try (MockedStatic<org.bukkit.Bukkit> bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(() -> org.bukkit.Bukkit.createBlockData("not valid data")).thenThrow(new IllegalArgumentException("bad"));
            bukkit.when(() -> org.bukkit.Bukkit.createBlockData(Material.LANTERN)).thenReturn(lanternData);

            assertThat(ContentItems.blockDataOf(content)).isSameAs(lanternData);
        }
    }

    @Test
    void blockData_ofANonBlock_isNull() {
        assertThat(ContentItems.blockDataOf(HeadContent.of(ContentKind.BLOCK, "DIAMOND", null))).isNull();
    }

    @Test
    void material_ofAHeadIsAPlayerHead() {
        assertThat(ContentItems.materialOf(HeadContent.head("x"))).isEqualTo(Material.PLAYER_HEAD);
        assertThat(ContentItems.materialOf(HeadContent.of(ContentKind.BLOCK, "LANTERN", null))).isEqualTo(Material.LANTERN);
    }

    @Test
    void blockDisplay_spawnsACenteredBlock() {
        var bukkit = mockStatic(org.bukkit.Bukkit.class);
        bukkit.when(() -> org.bukkit.Bukkit.createBlockData(Material.LANTERN)).thenReturn(mock(BlockData.class));
        World world = mock(World.class);
        Location anchor = mock(Location.class);
        when(anchor.getWorld()).thenReturn(world);
        BlockDisplay display = mock(BlockDisplay.class);
        Interaction interaction = mock(Interaction.class);
        when(world.spawn(anchor, BlockDisplay.class)).thenReturn(display);
        when(world.spawn(anchor, Interaction.class)).thenReturn(interaction);

        var entities = new BlockDisplayRenderer().spawn(anchor, HeadContent.of(ContentKind.BLOCK, "LANTERN", null),
                new RenderSettings(2.0, false, 0));

        bukkit.close();
        assertThat(entities).containsExactly(display, interaction);
        verify(display).setBlock(any(BlockData.class));
        verify(interaction).setInteractionWidth(2.0f);
    }

    @Test
    void blockDisplay_invalidBlock_fails() {
        World world = mock(World.class);
        Location anchor = mock(Location.class);
        when(anchor.getWorld()).thenReturn(world);

        assertThatThrownBy(() -> new BlockDisplayRenderer().spawn(anchor, HeadContent.of(ContentKind.BLOCK, "DIAMOND", null),
                new RenderSettings(1, false, 0))).isInstanceOf(IllegalStateException.class);
        verify(world, never()).spawn(any(), any());
    }

    @Test
    void blockDisplay_spinTurnsTheDisplayOnly() {
        BlockDisplay display = mock(BlockDisplay.class);
        when(display.isValid()).thenReturn(true);
        Entity interaction = mock(Interaction.class);

        new BlockDisplayRenderer().spin(List.of(display, interaction), 90f, new RenderSettings(1, false, 45f), 20);

        verify(display).setRotation(135f, 0);
        verify(interaction, never()).setRotation(anyFloat(), anyFloat());
    }
}
