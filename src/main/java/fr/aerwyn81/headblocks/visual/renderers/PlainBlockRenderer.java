package fr.aerwyn81.headblocks.visual.renderers;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.BlockRenderer;
import fr.aerwyn81.headblocks.visual.ContentItems;
import org.bukkit.block.Block;

public class PlainBlockRenderer implements BlockRenderer {

    @Override
    public boolean place(Block block, HeadContent content, float yaw) {
        var data = ContentItems.blockDataOf(content);
        if (data == null) {
            return false;
        }

        block.setBlockData(data);
        return true;
    }

    @Override
    public boolean matches(Block block, HeadContent content) {
        var material = ContentItems.materialOf(content);
        return material != null && block.getType() == material;
    }

    @Override
    public double height() {
        return 1.0;
    }
}
