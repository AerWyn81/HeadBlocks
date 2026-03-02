package fr.aerwyn81.headblocks.utils.bukkit;

import com.google.gson.JsonParser;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;

import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Adapter isolating all NBTAPI / NMS-dependent operations.
 * Excluded from JaCoCo coverage (requires Minecraft server internals).
 */
public class HeadAdapterNbtApi {

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

    public static String getHeadTextureFromItemStack(ItemStack head) {
        if (VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_20_R5)) {
            return NBT.modifyComponents(head, nbt -> (String) nbt.resolveOrDefault("minecraft:profile.properties[0].value", ""));
        } else {
            try {
                return NBT.get(head, nbt -> {
                    var skullOwner = nbt.getCompound("SkullOwner");
                    if (skullOwner == null) {
                        return "";
                    }

                    var properties = skullOwner.getCompound("Properties");
                    if (properties == null) {
                        return "";
                    }

                    return properties.getCompoundList("textures").get(0).getString("Value");
                });
            } catch (Exception ex) {
                return "";
            }
        }
    }

    public static String getHeadTextureFromBlock(Block headBlock) {
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
}
