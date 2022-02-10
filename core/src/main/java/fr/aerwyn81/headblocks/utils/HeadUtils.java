package fr.aerwyn81.headblocks.utils;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NBTListCompound;
import fr.aerwyn81.headblocks.data.head.Head;

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
}
