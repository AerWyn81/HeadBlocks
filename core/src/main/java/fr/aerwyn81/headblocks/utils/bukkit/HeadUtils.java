package fr.aerwyn81.headblocks.utils.bukkit;

import com.google.gson.JsonParser;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
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
import java.util.*;
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

        if (block.getType() != Material.PLAYER_HEAD)
            block.setType(Material.PLAYER_HEAD);

        var skull = (Skull)block.getState();

        if (VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_20_R5)) {
            NBT.modify(skull, nbt -> {
                var playerProfile = Bukkit.createPlayerProfile(UUID.randomUUID());
                var textures = playerProfile.getTextures();

                // Dirty, use it only for debug command
                URL url;
                try {
                    var decoded = Base64.getDecoder().decode(texture);
                    var jsonObject = JsonParser.parseString(new String(decoded)).getAsJsonObject();
                    url = new URI(jsonObject.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString()).toURL();
                } catch (Exception ex) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError when trying to decode texture: " + ex.getMessage()));
                    isApplied.set(false);
                    return;
                }

                textures.setSkin(url);
                playerProfile.setTextures(textures);

                skull.setOwnerProfile(playerProfile);
                skull.update();
            });
        } else {
            NBT.modify(skull, nbt -> {
                ReadWriteNBT skullOwnerCompound = nbt.getOrCreateCompound("SkullOwner");

                skullOwnerCompound.setUUID("Id", UUID.randomUUID());

                skullOwnerCompound.getOrCreateCompound("Properties")
                        .getCompoundList("textures")
                        .addCompound()
                        .setString("Value", texture);

                skull.update();
            });
        }

        return isApplied.get();
    }

    public static String getHeadTexture(ItemStack head) {
        if (VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_20_R5)) {
            return NBT.modifyComponents(head, nbt -> (String) nbt.resolveOrNull("minecraft:profile.properties[0].value", String.class));
        } else {
            var nbtItem = new NBTItem(head);
            try {
                return nbtItem.getCompound("SkullOwner").getCompound("Properties").getCompoundList("textures").get(0).getString("Value");
            } catch (Exception ex) {
                return "";
            }
        }
    }

    public static boolean areEquals(ItemStack i1, ItemStack i2) {
        if (!isValidItemStack(i1) || !isValidItemStack(i2)) {
            return false;
        }

        ItemMeta i1Meta = i1.getItemMeta();
        ItemMeta i2Meta = i2.getItemMeta();

        return i1Meta != null && i2Meta != null &&
                i1Meta.getPersistentDataContainer().has(new NamespacedKey(HeadBlocks.getInstance(), HeadService.HB_KEY), PersistentDataType.STRING) &&
                i2Meta.getPersistentDataContainer().has(new NamespacedKey(HeadBlocks.getInstance(), HeadService.HB_KEY), PersistentDataType.STRING);
    }

    private static boolean isValidItemStack(ItemStack i) {
        return i != null && i.getType() != Material.AIR;
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
}
