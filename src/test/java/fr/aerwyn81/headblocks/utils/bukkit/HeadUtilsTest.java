package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rotatable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

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

    // --- isHeadBlocksItem ---

    @Test
    void isHeadBlocksItem_nullOrAir_isFalse() {
        ItemStack air = mock(ItemStack.class);
        when(air.getType()).thenReturn(Material.AIR);

        assertThat(HeadUtils.isHeadBlocksItem(null)).isFalse();
        assertThat(HeadUtils.isHeadBlocksItem(air)).isFalse();
    }

    @Test
    void isHeadBlocksItem_readsTheHeadBlocksTag() {
        try (MockedStatic<HeadBlocks> hbStatic = mockStatic(HeadBlocks.class)) {
            HeadBlocks plugin = mock(HeadBlocks.class);
            when(plugin.getName()).thenReturn("headblocks");
            hbStatic.when(HeadBlocks::getInstance).thenReturn(plugin);

            ItemStack tagged = mock(ItemStack.class);
            when(tagged.getType()).thenReturn(Material.LANTERN);
            ItemMeta taggedMeta = mock(ItemMeta.class);
            PersistentDataContainer taggedPdc = mock(PersistentDataContainer.class);
            when(tagged.getItemMeta()).thenReturn(taggedMeta);
            when(taggedMeta.getPersistentDataContainer()).thenReturn(taggedPdc);
            when(taggedPdc.has(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn(true);

            ItemStack plain = mock(ItemStack.class);
            when(plain.getType()).thenReturn(Material.PLAYER_HEAD);
            ItemMeta plainMeta = mock(ItemMeta.class);
            PersistentDataContainer plainPdc = mock(PersistentDataContainer.class);
            when(plain.getItemMeta()).thenReturn(plainMeta);
            when(plainMeta.getPersistentDataContainer()).thenReturn(plainPdc);

            assertThat(HeadUtils.isHeadBlocksItem(tagged)).isTrue();
            assertThat(HeadUtils.isHeadBlocksItem(plain)).isFalse();
        }
    }

    @Test
    void isHeadBlocksItem_withoutMeta_isFalse() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.PLAYER_HEAD);

        assertThat(HeadUtils.isHeadBlocksItem(item)).isFalse();
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


    // --- yaw helpers ---

    @Test
    void yawOf_face_followsMinecraftConvention() {
        assertThat(HeadUtils.yawOf(BlockFace.SOUTH)).isEqualTo(0f);
        assertThat(HeadUtils.yawOf(BlockFace.WEST)).isEqualTo(90f);
        assertThat(HeadUtils.yawOf(BlockFace.NORTH)).isEqualTo(180f);
        assertThat(HeadUtils.yawOf(BlockFace.EAST)).isEqualTo(270f);
    }

    @Test
    void rotationOf_isTheInverseOfYawOf() {
        for (var face : HeadUtils.skullRotationList.values()) {
            assertThat(HeadUtils.rotationOf(HeadUtils.yawOf(face))).isEqualTo(face);
        }
    }

    @Test
    void rotationOf_roundsToTheClosestSixteenth() {
        assertThat(HeadUtils.rotationOf(359f)).isEqualTo(BlockFace.SOUTH);
        assertThat(HeadUtils.rotationOf(-90f)).isEqualTo(BlockFace.EAST);
        assertThat(HeadUtils.rotationOf(100f)).isEqualTo(BlockFace.WEST);
    }

    @Test
    void yawOf_block_readsRotatableData() {
        Block block = mock(Block.class);
        Rotatable rotatable = mock(Rotatable.class);
        when(block.getBlockData()).thenReturn(rotatable);
        when(rotatable.getRotation()).thenReturn(BlockFace.WEST);

        assertThat(HeadUtils.yawOf(block)).isEqualTo(90f);
    }

    @Test
    void yawOf_block_withoutOrientation_isZero() {
        Block block = mock(Block.class);
        when(block.getBlockData()).thenReturn(mock(org.bukkit.block.data.BlockData.class));

        assertThat(HeadUtils.yawOf(block)).isZero();
    }

    @Test
    void normalizeYaw_wrapsIntoZeroToThreeSixty() {
        assertThat(HeadUtils.normalizeYaw(-90f)).isEqualTo(270f);
        assertThat(HeadUtils.normalizeYaw(450f)).isEqualTo(90f);
        assertThat(HeadUtils.normalizeYaw(0f)).isZero();
    }

    @Test
    void snapYaw_snapsToSkullSteps() {
        assertThat(HeadUtils.snapYaw(10f)).isZero();
        assertThat(HeadUtils.snapYaw(12f)).isEqualTo(22.5f);
        assertThat(HeadUtils.snapYaw(-100f)).isEqualTo(270f);
    }

    // --- getContent ---

    private ItemStack headItem(ItemMeta meta) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.PLAYER_HEAD);
        when(item.getItemMeta()).thenReturn(meta);
        return item;
    }

    @Test
    void getContent_texturedHead_isAHeadContent() {
        ItemMeta meta = mock(ItemMeta.class);
        when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        ItemStack item = headItem(meta);

        try (MockedStatic<HeadBlocks> hbStatic = mockStatic(HeadBlocks.class);
             MockedStatic<HeadAdapterNbtApi> nbt = mockStatic(HeadAdapterNbtApi.class)) {
            HeadBlocks plugin = mock(HeadBlocks.class);
            when(plugin.getName()).thenReturn("headblocks");
            hbStatic.when(HeadBlocks::getInstance).thenReturn(plugin);
            nbt.when(() -> HeadAdapterNbtApi.getHeadTextureFromItemStack(item)).thenReturn("abc");

            assertThat(HeadUtils.getContent(item)).isEqualTo(HeadContent.head("abc"));
        }
    }

    @Test
    void getContent_untexturedPlayerHead_keepsItsOwner() {
        SkullMeta meta = mock(SkullMeta.class);
        when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        OfflinePlayer owner = mock(OfflinePlayer.class);
        UUID ownerUuid = UUID.randomUUID();
        when(owner.getUniqueId()).thenReturn(ownerUuid);
        when(meta.getOwningPlayer()).thenReturn(owner);
        ItemStack item = headItem(meta);

        try (MockedStatic<HeadBlocks> hbStatic = mockStatic(HeadBlocks.class);
             MockedStatic<HeadAdapterNbtApi> nbt = mockStatic(HeadAdapterNbtApi.class)) {
            HeadBlocks plugin = mock(HeadBlocks.class);
            when(plugin.getName()).thenReturn("headblocks");
            hbStatic.when(HeadBlocks::getInstance).thenReturn(plugin);
            nbt.when(() -> HeadAdapterNbtApi.getHeadTextureFromItemStack(item)).thenReturn("");

            var content = HeadUtils.getContent(item);

            assertThat(content.value()).isEmpty();
            assertThat(content.option("owner")).isEqualTo(ownerUuid.toString());
        }
    }

    @Test
    void getContent_headWithoutTextureOrOwner_isUnresolved() {
        ItemMeta meta = mock(ItemMeta.class);
        when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        ItemStack item = headItem(meta);

        try (MockedStatic<HeadBlocks> hbStatic = mockStatic(HeadBlocks.class);
             MockedStatic<HeadAdapterNbtApi> nbt = mockStatic(HeadAdapterNbtApi.class)) {
            HeadBlocks plugin = mock(HeadBlocks.class);
            when(plugin.getName()).thenReturn("headblocks");
            hbStatic.when(HeadBlocks::getInstance).thenReturn(plugin);
            nbt.when(() -> HeadAdapterNbtApi.getHeadTextureFromItemStack(item)).thenReturn("");

            assertThat(HeadUtils.getContent(item)).isNull();
        }
    }

    @Test
    void cardinalOf_roundsToTheClosestSide() {
        assertThat(HeadUtils.cardinalOf(10f)).isEqualTo(BlockFace.SOUTH);
        assertThat(HeadUtils.cardinalOf(80f)).isEqualTo(BlockFace.WEST);
        assertThat(HeadUtils.cardinalOf(-170f)).isEqualTo(BlockFace.NORTH);
        assertThat(HeadUtils.cardinalOf(265f)).isEqualTo(BlockFace.EAST);
    }
}
