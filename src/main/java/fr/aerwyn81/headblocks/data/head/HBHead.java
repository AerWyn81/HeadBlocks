package fr.aerwyn81.headblocks.data.head;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import org.bukkit.inventory.ItemStack;

public class HBHead {
    private ItemStack itemStack;

    public HeadContent getContent() {
        return HeadUtils.getContent(itemStack);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }
}
