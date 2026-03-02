package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.services.HeadService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rotatable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;

public class HeadUtils {

    public static final HashMap<Integer, BlockFace> skullRotationList;

    static {
        skullRotationList = new HashMap<>();
        skullRotationList.put(0, BlockFace.NORTH);
        skullRotationList.put(1, BlockFace.NORTH_NORTH_EAST);
        skullRotationList.put(2, BlockFace.NORTH_EAST);
        skullRotationList.put(3, BlockFace.EAST_NORTH_EAST);
        skullRotationList.put(4, BlockFace.EAST);
        skullRotationList.put(5, BlockFace.EAST_SOUTH_EAST);
        skullRotationList.put(6, BlockFace.SOUTH_EAST);
        skullRotationList.put(7, BlockFace.SOUTH_SOUTH_EAST);
        skullRotationList.put(8, BlockFace.SOUTH);
        skullRotationList.put(9, BlockFace.SOUTH_SOUTH_WEST);
        skullRotationList.put(10, BlockFace.SOUTH_WEST);
        skullRotationList.put(11, BlockFace.WEST_SOUTH_WEST);
        skullRotationList.put(12, BlockFace.WEST);
        skullRotationList.put(13, BlockFace.WEST_NORTH_WEST);
        skullRotationList.put(14, BlockFace.NORTH_WEST);
        skullRotationList.put(15, BlockFace.NORTH_NORTH_WEST);
    }

    public static HBHead createHead(HBHead head, String texture) {
        head.setItemStack(HeadAdapterNbtApi.applyTextureToItemStack(head.getItemStack(), texture));
        return head;
    }

    public static ItemStack applyTextureToItemStack(ItemStack itemStack, String texture) {
        return HeadAdapterNbtApi.applyTextureToItemStack(itemStack, texture);
    }

    public static boolean applyTextureToBlock(Block block, String texture) {
        return HeadAdapterNbtApi.applyTextureToBlock(block, texture);
    }

    public static String getHeadTexture(ItemStack head) {
        if (!isPlayerHead(head)) {
            return "";
        }

        return HeadAdapterNbtApi.getHeadTextureFromItemStack(head);
    }

    public static String getHeadTexture(Block headBlock) {
        if (!isPlayerHead(headBlock)) {
            return "";
        }

        return HeadAdapterNbtApi.getHeadTextureFromBlock(headBlock);
    }

    public static boolean areEquals(ItemStack i1, ItemStack i2) {
        if (isNotValidItemStack(i1) || isNotValidItemStack(i2)) {
            return false;
        }

        ItemMeta i1Meta = i1.getItemMeta();
        ItemMeta i2Meta = i2.getItemMeta();

        return i1Meta != null && i2Meta != null &&
                i1Meta.getPersistentDataContainer().has(new NamespacedKey(HeadBlocks.getInstance(), HeadService.HB_KEY), PersistentDataType.STRING) &&
                i2Meta.getPersistentDataContainer().has(new NamespacedKey(HeadBlocks.getInstance(), HeadService.HB_KEY), PersistentDataType.STRING);
    }

    private static boolean isNotValidItemStack(ItemStack i) {
        return i == null || i.getType() == Material.AIR;
    }

    public static void rotateHead(Block block, BlockFace face) {
        var blockData = block.getBlockData();
        ((Rotatable) blockData).setRotation(face);
        block.setBlockData(blockData);
    }

    public static BlockFace getRotation(Block block) {
        var blockData = block.getBlockData();
        return ((Rotatable) blockData).getRotation();
    }

    public static boolean isPlayerHead(ItemStack i) {
        if (i == null) {
            return false;
        }

        return i.getType() == Material.PLAYER_HEAD || i.getType() == Material.PLAYER_WALL_HEAD;
    }

    public static boolean isPlayerHead(Block b) {
        if (b == null) {
            return false;
        }

        return b.getType() == Material.PLAYER_HEAD || b.getType() == Material.PLAYER_WALL_HEAD;
    }
}
