package fr.aerwyn81.headblocks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import org.bukkit.block.Block;

public interface BlockRenderer {

    boolean place(Block block, HeadContent content, float yaw);

    boolean matches(Block block, HeadContent content);

    double height();
}
