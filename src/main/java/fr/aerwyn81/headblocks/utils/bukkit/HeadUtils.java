package fr.aerwyn81.headblocks.utils.bukkit;

import com.google.gson.JsonParser;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Rotatable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
        head.setItemStack(applyTextureToItemStack(head.getItemStack(), texture));
        return head;
    }

    public static ItemStack applyTextureToItemStack(ItemStack itemStack, String texture) {
        if (VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_20_R5)) {
            NBT.modifyComponents(itemStack, nbt -> {
                ReadWriteNBT profileCompound = nbt.getOrCreateCompound("minecraft:profile");
                profileCompound.setUUID("id", UUID.randomUUID());

                ReadWriteNBT propertiesCompound = profileCompound.getCompoundList("properties").addCompound();
                propertiesCompound.setString("name", "textures");
                propertiesCompound.setString("value", texture);
            });

            return itemStack;
        }

        NBT.modify(itemStack, nbt -> {
            ReadWriteNBT skullOwnerCompound = nbt.getOrCreateCompound("SkullOwner");

            skullOwnerCompound.setUUID("Id", UUID.randomUUID());

            skullOwnerCompound.getOrCreateCompound("Properties")
                    .getCompoundList("textures")
                    .addCompound()
                    .setString("Value", texture);
        });

        return itemStack;
    }

    public static boolean applyTextureToBlock(Block block, String texture) {
        var isApplied = new AtomicBoolean(true);

        var skull = (Skull) block.getState();

        if (VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_20_R5)) {
            var playerProfile = Bukkit.createPlayerProfile(UUID.randomUUID());
            var textures = playerProfile.getTextures();

            URL url;
            try {
                var decoded = Base64.getDecoder().decode(texture);
                var jsonObject = JsonParser.parseString(new String(decoded)).getAsJsonObject();
                url = new URI(jsonObject.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString()).toURL();
            } catch (Exception ex) {
                LogUtil.error("Error when trying to decode texture: {0}", ex.getMessage());
                isApplied.set(false);
                return isApplied.get();
            }

            textures.setSkin(url);
            playerProfile.setTextures(textures);

            skull.setOwnerProfile(playerProfile);
            skull.update(true, false);
        } else {
            NBT.modify(skull, nbt -> {
                ReadWriteNBT skullOwnerCompound = nbt.getOrCreateCompound("SkullOwner");

                skullOwnerCompound.setUUID("Id", UUID.randomUUID());

                skullOwnerCompound.getOrCreateCompound("Properties")
                        .getCompoundList("textures")
                        .addCompound()
                        .setString("Value", texture);

                skull.update(true, false);
            });
        }

        return isApplied.get();
    }

    public static String getHeadTexture(ItemStack head) {
        if (!isPlayerHead(head))
            return "";

        if (VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_20_R5)) {
            return NBT.modifyComponents(head, nbt -> (String) nbt.resolveOrDefault("minecraft:profile.properties[0].value", ""));
        } else {
            try {
                //noinspection deprecation, DataFlowIssue
                return new NBTItem(head).getCompound("SkullOwner").getCompound("Properties").getCompoundList("textures").get(0).getString("Value");
            } catch (Exception ex) {
                return "";
            }
        }
    }

    public static String getHeadTexture(Block headBlock) {
        if (!isPlayerHead(headBlock))
            return "";

        if (VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_20_R5)) {
            return NBT.get(headBlock.getState(), nbt -> (String) nbt.resolveOrDefault("profile.properties[0].value", ""));
        } else {
            try {
                // noinspection DataFlowIssue
                return NBT.get(headBlock.getState(), nbt -> (String) nbt.getCompound("SkullOwner").getCompound("Properties").getCompoundList("textures").get(0).getString("Value"));
            } catch (Exception ex) {
                return "";
            }
        }
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
        if (i == null)
            return false;

        return i.getType() == Material.PLAYER_HEAD || i.getType() == Material.PLAYER_WALL_HEAD;
    }

    public static boolean isPlayerHead(Block b) {
        if (b == null)
            return false;

        return b.getType() == Material.PLAYER_HEAD || b.getType() == Material.PLAYER_WALL_HEAD;
    }

}
