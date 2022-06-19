package fr.aerwyn81.headblocks.utils;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NBTListCompound;
import fr.aerwyn81.headblocks.data.head.HBHead;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class HeadUtils {
    public static HBHead applyTexture(HBHead head, String texture) {
        NBTItem nbti = new NBTItem(head.getItemStack());
        NBTCompound skull = nbti.addCompound("SkullOwner");
        skull.setString("Name", "HeadBlocks");

        if (Version.getCurrent().isOlderOrSameThan(Version.v1_15)) {
            skull.setString("Id", "f032de26-fde9-469f-a6eb-c453470894a5");
        } else {
            skull.setUUID("Id", UUID.fromString("f032de26-fde9-469f-a6eb-c453470894a5"));
        }

        NBTListCompound textCompound = skull.addCompound("Properties").getCompoundList("textures").addCompound();
        textCompound.setString("Value", texture);

        head.setItemStack(nbti.getItem());
        return head;
    }

    public static boolean areEquals(ItemStack i1, ItemStack i2) {
        if (!isValidItemStack(i1) || !isValidItemStack(i2)) {
            return false;
        }

        NBTItem nbtItem = new NBTItem(i1);
        NBTItem nbtItem2 = new NBTItem(i2);

        return nbtItem.hasKey("HB_HEAD") && nbtItem2.hasKey("HB_HEAD");
    }

    private static boolean isValidItemStack(ItemStack i) {
        return i != null && i.getType() != Material.AIR;
    }
}
