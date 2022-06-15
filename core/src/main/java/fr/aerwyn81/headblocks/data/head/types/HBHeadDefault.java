package fr.aerwyn81.headblocks.data.head.types;

import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.HeadType;
import org.bukkit.inventory.ItemStack;

public class HBHeadDefault extends HBHead {
    private String texture;

    public HBHeadDefault(ItemStack headItem) {
        setItemStack(headItem);
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public HeadType getType() {
        return HeadType.DEFAULT;
    }
}