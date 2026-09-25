package fr.aerwyn81.headblocks.visual.renderers;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.visual.BlockRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;

import java.util.UUID;

public class HeadBlockRenderer implements BlockRenderer {

    @Override
    public boolean place(Block block, HeadContent content, float yaw) {
        if (content.optionBoolean("wall", false)) {
            block.setType(Material.PLAYER_WALL_HEAD);
            if (block.getBlockData() instanceof Directional directional) {
                directional.setFacing(HeadUtils.cardinalOf(yaw));
                block.setBlockData(directional);
            }
        } else {
            block.setType(Material.PLAYER_HEAD);
            if (block.getBlockData() instanceof Rotatable rotatable) {
                rotatable.setRotation(HeadUtils.rotationOf(yaw));
                block.setBlockData(rotatable);
            }
        }

        if (!content.value().isEmpty()) {
            return HeadUtils.applyTextureToBlock(block, content.value());
        }

        var owner = content.option("owner");
        if (owner != null && block.getState() instanceof Skull skull) {
            try {
                skull.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(owner)));
                skull.update(true, false);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return true;
    }

    @Override
    public boolean matches(Block block, HeadContent content) {
        return HeadUtils.isPlayerHead(block);
    }

    @Override
    public double height() {
        return 0.5;
    }
}
