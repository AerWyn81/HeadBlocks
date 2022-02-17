package fr.aerwyn81.headblocks.utils;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NBTListCompound;
import fr.aerwyn81.headblocks.data.head.Head;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class HeadUtils {
    public static Head applyTexture(Head head) {
        if (head.getTexture() == null)
            return head;

        NBTItem nbti = new NBTItem(head.getHead());
        NBTCompound skull = nbti.addCompound("SkullOwner");
        skull.setString("Name", "HeadBlocks");

        if (Version.getCurrent().isOlderOrSameThan(Version.v1_15)) {
            skull.setString("Id", "f032de26-fde9-469f-a6eb-c453470894a5");
        } else {
            skull.setUUID("Id", UUID.fromString("f032de26-fde9-469f-a6eb-c453470894a5"));
        }

        NBTListCompound textCompound = skull.addCompound("Properties").getCompoundList("textures").addCompound();
        textCompound.setString("Value", head.getTexture());

        head.setHead(nbti.getItem());
        head.setLoaded(true);
        return head;
    }

    public static boolean areEquals(ItemStack i1, ItemStack i2) {
        NBTItem nbtItem = new NBTItem(i1);
        NBTItem nbtItem2 = new NBTItem(i2);

        return nbtItem.hasKey("HB_HEAD") && nbtItem2.hasKey("HB_HEAD");
    }

    public static boolean areEquals(Location loc1, Location loc2) {
        return loc1 != null && loc2 != null && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ()
                && loc1.getWorld() != null && loc2.getWorld() != null &&
                loc1.getWorld().getName().equals(loc2.getWorld().getName());
    }
}
