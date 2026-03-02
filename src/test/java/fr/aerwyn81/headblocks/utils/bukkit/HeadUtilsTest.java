package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.HeadBlocks;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rotatable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HeadUtilsTest {

    // --- skullRotationList ---

    @Test
    void skullRotationList_has16Entries() {
        assertThat(HeadUtils.skullRotationList).hasSize(16);
    }

    @Test
    void skullRotationList_containsAllExpectedFaces() {
        assertThat(HeadUtils.skullRotationList.values())
                .contains(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);
    }

    // --- isPlayerHead(ItemStack) ---

    @Test
    void isPlayerHead_itemStack_null_returnsFalse() {
        assertThat(HeadUtils.isPlayerHead((ItemStack) null)).isFalse();
    }

    @Test
    void isPlayerHead_itemStack_air_returnsFalse() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.AIR);

        assertThat(HeadUtils.isPlayerHead(item)).isFalse();
    }

    @Test
    void isPlayerHead_itemStack_playerHead_returnsTrue() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.PLAYER_HEAD);

        assertThat(HeadUtils.isPlayerHead(item)).isTrue();
    }

    @Test
    void isPlayerHead_itemStack_playerWallHead_returnsTrue() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.PLAYER_WALL_HEAD);

        assertThat(HeadUtils.isPlayerHead(item)).isTrue();
    }

    // --- isPlayerHead(Block) ---

    @Test
    void isPlayerHead_block_null_returnsFalse() {
        assertThat(HeadUtils.isPlayerHead((Block) null)).isFalse();
    }

    @Test
    void isPlayerHead_block_stone_returnsFalse() {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.STONE);

        assertThat(HeadUtils.isPlayerHead(block)).isFalse();
    }

    @Test
    void isPlayerHead_block_playerHead_returnsTrue() {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.PLAYER_HEAD);

        assertThat(HeadUtils.isPlayerHead(block)).isTrue();
    }

    @Test
    void isPlayerHead_block_playerWallHead_returnsTrue() {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.PLAYER_WALL_HEAD);

        assertThat(HeadUtils.isPlayerHead(block)).isTrue();
    }

    // --- rotateHead / getRotation ---

    @Test
    void rotateHead_setsCorrectRotation() {
        Block block = mock(Block.class);
        Rotatable rotatable = mock(Rotatable.class);
        when(block.getBlockData()).thenReturn(rotatable);

        HeadUtils.rotateHead(block, BlockFace.SOUTH);

        verify(rotatable).setRotation(BlockFace.SOUTH);
        verify(block).setBlockData(rotatable);
    }

    @Test
    void getRotation_returnsCurrentRotation() {
        Block block = mock(Block.class);
        Rotatable rotatable = mock(Rotatable.class);
        when(block.getBlockData()).thenReturn(rotatable);
        when(rotatable.getRotation()).thenReturn(BlockFace.EAST);

        BlockFace result = HeadUtils.getRotation(block);

        assertThat(result).isEqualTo(BlockFace.EAST);
    }

    // --- areEquals ---

    @Test
    void areEquals_nullFirst_returnsFalse() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.PLAYER_HEAD);

        assertThat(HeadUtils.areEquals(null, item)).isFalse();
    }

    @Test
    void areEquals_nullSecond_returnsFalse() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.PLAYER_HEAD);

        assertThat(HeadUtils.areEquals(item, null)).isFalse();
    }

    @Test
    void areEquals_bothNull_returnsFalse() {
        assertThat(HeadUtils.areEquals(null, null)).isFalse();
    }

    @Test
    void areEquals_airItem_returnsFalse() {
        ItemStack air = mock(ItemStack.class);
        when(air.getType()).thenReturn(Material.AIR);
        ItemStack head = mock(ItemStack.class);
        when(head.getType()).thenReturn(Material.PLAYER_HEAD);

        assertThat(HeadUtils.areEquals(air, head)).isFalse();
    }

    @Test
    void areEquals_bothAir_returnsFalse() {
        ItemStack air1 = mock(ItemStack.class);
        when(air1.getType()).thenReturn(Material.AIR);
        ItemStack air2 = mock(ItemStack.class);
        when(air2.getType()).thenReturn(Material.AIR);

        assertThat(HeadUtils.areEquals(air1, air2)).isFalse();
    }

    // --- getHeadTexture(ItemStack) ---

    @Test
    void getHeadTexture_itemStack_notPlayerHead_returnsEmpty() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STONE);

        assertThat(HeadUtils.getHeadTexture(item)).isEmpty();
    }

    // --- getHeadTexture(Block) ---

    @Test
    void getHeadTexture_block_notPlayerHead_returnsEmpty() {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.STONE);

        assertThat(HeadUtils.getHeadTexture(block)).isEmpty();
    }

    // --- skullRotationList specific entries ---

    @Test
    void skullRotationList_entry0_isNorth() {
        assertThat(HeadUtils.skullRotationList.get(0)).isEqualTo(BlockFace.NORTH);
    }

    @Test
    void skullRotationList_entry4_isEast() {
        assertThat(HeadUtils.skullRotationList.get(4)).isEqualTo(BlockFace.EAST);
    }

    @Test
    void skullRotationList_entry8_isSouth() {
        assertThat(HeadUtils.skullRotationList.get(8)).isEqualTo(BlockFace.SOUTH);
    }

    @Test
    void skullRotationList_entry12_isWest() {
        assertThat(HeadUtils.skullRotationList.get(12)).isEqualTo(BlockFace.WEST);
    }

    // --- areEquals deep path (with metas and PersistentDataContainer) ---

    @Test
    void areEquals_bothValidHeads_bothHaveHBKey_returnsTrue() {
        try (MockedStatic<HeadBlocks> hbStatic = mockStatic(HeadBlocks.class)) {
            HeadBlocks plugin = mock(HeadBlocks.class);
            when(plugin.getName()).thenReturn("headblocks");
            hbStatic.when(HeadBlocks::getInstance).thenReturn(plugin);

            ItemStack i1 = mock(ItemStack.class);
            when(i1.getType()).thenReturn(Material.PLAYER_HEAD);
            ItemMeta meta1 = mock(ItemMeta.class);
            PersistentDataContainer pdc1 = mock(PersistentDataContainer.class);
            when(i1.getItemMeta()).thenReturn(meta1);
            when(meta1.getPersistentDataContainer()).thenReturn(pdc1);
            when(pdc1.has(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn(true);

            ItemStack i2 = mock(ItemStack.class);
            when(i2.getType()).thenReturn(Material.PLAYER_HEAD);
            ItemMeta meta2 = mock(ItemMeta.class);
            PersistentDataContainer pdc2 = mock(PersistentDataContainer.class);
            when(i2.getItemMeta()).thenReturn(meta2);
            when(meta2.getPersistentDataContainer()).thenReturn(pdc2);
            when(pdc2.has(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn(true);

            assertThat(HeadUtils.areEquals(i1, i2)).isTrue();
        }
    }

    @Test
    void areEquals_bothValidHeads_firstMissingHBKey_returnsFalse() {
        try (MockedStatic<HeadBlocks> hbStatic = mockStatic(HeadBlocks.class)) {
            HeadBlocks plugin = mock(HeadBlocks.class);
            when(plugin.getName()).thenReturn("headblocks");
            hbStatic.when(HeadBlocks::getInstance).thenReturn(plugin);

            ItemStack i1 = mock(ItemStack.class);
            when(i1.getType()).thenReturn(Material.PLAYER_HEAD);
            ItemMeta meta1 = mock(ItemMeta.class);
            PersistentDataContainer pdc1 = mock(PersistentDataContainer.class);
            when(i1.getItemMeta()).thenReturn(meta1);
            when(meta1.getPersistentDataContainer()).thenReturn(pdc1);
            when(pdc1.has(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn(false);

            ItemStack i2 = mock(ItemStack.class);
            when(i2.getType()).thenReturn(Material.PLAYER_HEAD);
            ItemMeta meta2 = mock(ItemMeta.class);
            PersistentDataContainer pdc2 = mock(PersistentDataContainer.class);
            when(i2.getItemMeta()).thenReturn(meta2);
            when(meta2.getPersistentDataContainer()).thenReturn(pdc2);
            when(pdc2.has(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn(true);

            assertThat(HeadUtils.areEquals(i1, i2)).isFalse();
        }
    }

    @Test
    void areEquals_firstNullMeta_returnsFalse() {
        ItemStack i1 = mock(ItemStack.class);
        when(i1.getType()).thenReturn(Material.PLAYER_HEAD);
        when(i1.getItemMeta()).thenReturn(null);

        ItemStack i2 = mock(ItemStack.class);
        when(i2.getType()).thenReturn(Material.PLAYER_HEAD);
        ItemMeta meta2 = mock(ItemMeta.class);
        when(i2.getItemMeta()).thenReturn(meta2);

        assertThat(HeadUtils.areEquals(i1, i2)).isFalse();
    }

    @Test
    void areEquals_secondNullMeta_returnsFalse() {
        ItemStack i1 = mock(ItemStack.class);
        when(i1.getType()).thenReturn(Material.PLAYER_HEAD);
        ItemMeta meta1 = mock(ItemMeta.class);
        when(i1.getItemMeta()).thenReturn(meta1);

        ItemStack i2 = mock(ItemStack.class);
        when(i2.getType()).thenReturn(Material.PLAYER_HEAD);
        when(i2.getItemMeta()).thenReturn(null);

        assertThat(HeadUtils.areEquals(i1, i2)).isFalse();
    }

}
