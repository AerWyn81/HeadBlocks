package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.services.HeadService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static ItemStack withHunt(ItemStack itemStack, String huntId, String loreLine) {
        ItemStack tagged = itemStack.clone();
        ItemMeta meta = tagged.getItemMeta();
        if (meta == null) {
            return tagged;
        }

        meta.getPersistentDataContainer().set(new NamespacedKey(HeadBlocks.getInstance(), HeadService.HB_HUNT_KEY), PersistentDataType.STRING, huntId);

        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(loreLine);
        meta.setLore(lore);

        tagged.setItemMeta(meta);
        return tagged;
    }

    public static ItemStack withContent(ItemStack itemStack, HeadContent content) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        meta.getPersistentDataContainer().set(new NamespacedKey(HeadBlocks.getInstance(), HeadService.HB_CONTENT_KEY), PersistentDataType.STRING, content.toJson());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static HeadContent getContent(ItemStack itemStack) {
        if (isNotValidItemStack(itemStack)) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            var json = meta.getPersistentDataContainer().get(new NamespacedKey(HeadBlocks.getInstance(), HeadService.HB_CONTENT_KEY), PersistentDataType.STRING);
            if (json != null) {
                return HeadContent.fromJson(json);
            }
        }

        if (!isPlayerHead(itemStack)) {
            return null;
        }

        var texture = getHeadTexture(itemStack);
        if (texture != null && !texture.isEmpty()) {
            return HeadContent.head(texture);
        }

        if (meta instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() != null) {
            return HeadContent.of(ContentKind.HEAD, "", Map.of("owner", skullMeta.getOwningPlayer().getUniqueId().toString()));
        }

        return null;
    }

    public static boolean isHeadBlocksItem(ItemStack itemStack) {
        if (isNotValidItemStack(itemStack)) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(HeadBlocks.getInstance(), HeadService.HB_KEY), PersistentDataType.STRING);
    }

    public static float yawOf(BlockFace face) {
        var direction = face.getDirection();
        return normalizeYaw((float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ())));
    }

    public static float yawOf(Block block) {
        var blockData = block.getBlockData();
        if (blockData instanceof Rotatable rotatable) {
            return yawOf(rotatable.getRotation());
        }
        if (blockData instanceof Directional directional) {
            return yawOf(directional.getFacing());
        }
        return 0f;
    }

    public static BlockFace rotationOf(float yaw) {
        var index = Math.round(normalizeYaw(yaw) / 22.5f) % 16;
        return skullRotationList.get((index + 8) % 16);
    }

    public static BlockFace cardinalOf(float yaw) {
        return switch (Math.round(normalizeYaw(yaw) / 90f) % 4) {
            case 1 -> BlockFace.WEST;
            case 2 -> BlockFace.NORTH;
            case 3 -> BlockFace.EAST;
            default -> BlockFace.SOUTH;
        };
    }

    public static float normalizeYaw(float yaw) {
        var normalized = yaw % 360f;
        return normalized < 0 ? normalized + 360f : normalized;
    }

    public static float snapYaw(float yaw) {
        return normalizeYaw(Math.round(yaw / 22.5f) * 22.5f);
    }

    public static String getHuntId(ItemStack itemStack) {
        if (isNotValidItemStack(itemStack)) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }

        return meta.getPersistentDataContainer().get(new NamespacedKey(HeadBlocks.getInstance(), HeadService.HB_HUNT_KEY), PersistentDataType.STRING);
    }

    private static boolean isNotValidItemStack(ItemStack i) {
        return i == null || i.getType() == Material.AIR;
    }

    public static void rotateHead(Block block, BlockFace face) {
        var blockData = block.getBlockData();
        if (blockData instanceof Rotatable rotatable) {
            rotatable.setRotation(face);
            block.setBlockData(blockData);
        }
    }

    public static BlockFace getRotation(Block block) {
        var blockData = block.getBlockData();
        if (blockData instanceof Rotatable rotatable) {
            return rotatable.getRotation();
        }
        return BlockFace.NORTH;
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
