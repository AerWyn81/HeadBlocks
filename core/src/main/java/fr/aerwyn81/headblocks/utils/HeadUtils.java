package fr.aerwyn81.headblocks.utils;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import fr.aerwyn81.headblocks.data.head.Head;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class HeadUtils {
    public static Head applyTexture(Head head) {
        if (head.getTexture() == null)
            return head;

        NBT.modify(head.getHead(), nbt -> {
            ReadWriteNBT skullOwnerCompound = nbt.getOrCreateCompound("SkullOwner");

            skullOwnerCompound.setUUID("Id", UUID.randomUUID());

            skullOwnerCompound.getOrCreateCompound("Properties")
                    .getCompoundList("textures")
                    .addCompound()
                    .setString("Value", head.getTexture());
        });

        head.setLoaded(true);
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

    public static boolean areEquals(Location loc1, Location loc2) {
        return loc1 != null && loc2 != null && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ()
                && loc1.getWorld() != null && loc2.getWorld() != null &&
                loc1.getWorld().getName().equals(loc2.getWorld().getName());
    }

    private static boolean isValidItemStack(ItemStack i) {
        return i != null && i.getType() != Material.AIR;
    }
}
