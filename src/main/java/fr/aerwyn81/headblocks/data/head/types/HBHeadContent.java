package fr.aerwyn81.headblocks.data.head.types;

import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import org.bukkit.inventory.ItemStack;

public class HBHeadContent extends HBHead {
    private final HeadContent content;

    public HBHeadContent(ItemStack item, HeadContent content) {
        setItemStack(item);
        this.content = content;
    }

    @Override
    public HeadContent getContent() {
        return content;
    }
}
