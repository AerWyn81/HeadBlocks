package fr.aerwyn81.headblocks.utils.bukkit;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NBTListCompound;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.services.HeadService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class HeadUtils {

    public static HBHead createHead(HBHead head, String texture) {
        head.setItemStack(applyTextureToItemStack(head.getItemStack(), texture));
        return head;
    }

    public static ItemStack applyTextureToItemStack(ItemStack itemStack, String texture) {
        NBTItem nbti = new NBTItem(itemStack);
        NBTCompound skull = nbti.addCompound("SkullOwner");
        skull.setString("Name", "HeadBlocks");

        if (VersionUtils.getCurrent().isOlderOrSameThan(VersionUtils.v1_15)) {
            skull.setString("Id", "f032de26-fde9-469f-a6eb-c453470894a5");
        } else {
            skull.setUUID("Id", UUID.fromString("f032de26-fde9-469f-a6eb-c453470894a5"));
        }

        NBTListCompound textCompound = skull.addCompound("Properties").getCompoundList("textures").addCompound();
        textCompound.setString("Value", texture);

        return nbti.getItem();
    }

    public static String getHeadTexture(ItemStack head) {
        var nbtItem = new NBTItem(head);
        try {
            return nbtItem.getCompound("SkullOwner").getCompound("Properties").getCompoundList("textures").get(0).getString("Value");
        } catch (Exception ex) {
            return "";
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
}
