package fr.aerwyn81.headblocks.data.head.types;

import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.HeadType;
import org.bukkit.inventory.ItemStack;

public class HBHeadPlayer extends HBHead {
    public HBHeadPlayer(ItemStack head) {
        setItemStack(head);
    }

    public HeadType getType() {
        return HeadType.PLAYER;
    }
}