package fr.aerwyn81.headblocks.visual;

import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public final class ContentItems {

    private static final int TEXTURE_MIN_LENGTH = 40;

    private ContentItems() {
    }

    public static ItemStack itemOf(HeadContent content) {
        if (content == null) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        return switch (content.kind()) {
            case HEAD -> headItem(content);
            case BLOCK -> new ItemStack(itemMaterial(content.value(), Material.STONE));
            case ITEM -> modelItem(content);
            case MOB -> new ItemStack(itemMaterial(content.value() + "_SPAWN_EGG", Material.PAPER));
            case TEXT -> new ItemStack(Material.NAME_TAG);
            case FRAME -> modelItem(content);
            case EXTERNAL -> new ItemStack(Material.PAPER);
        };
    }

    public static ItemStack equipmentOf(String raw) {
        var material = Material.matchMaterial(raw);
        if (material != null) {
            return material.isItem() && material != Material.AIR ? new ItemStack(material) : null;
        }

        return raw.length() > TEXTURE_MIN_LENGTH ? HeadUtils.applyTextureToItemStack(new ItemStack(Material.PLAYER_HEAD), raw) : null;
    }

    public static BlockData blockDataOf(HeadContent content) {
        var raw = content.option("data");
        if (raw != null && !raw.isEmpty()) {
            try {
                return Bukkit.createBlockData(raw);
            } catch (IllegalArgumentException ignored) {
            }
        }

        var material = Material.matchMaterial(content.value());
        return material != null && material.isBlock() ? material.createBlockData() : null;
    }

    public static Material materialOf(HeadContent content) {
        return content.kind() == ContentKind.HEAD ? Material.PLAYER_HEAD : Material.matchMaterial(content.value());
    }

    private static ItemStack headItem(HeadContent content) {
        var item = new ItemStack(Material.PLAYER_HEAD);
        var owner = content.option("owner");

        if (content.value().isEmpty() && owner != null) {
            try {
                var meta = (SkullMeta) item.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(owner)));
                    item.setItemMeta(meta);
                }
            } catch (IllegalArgumentException ignored) {
            }
            return item;
        }

        return content.value().isEmpty() ? item : HeadUtils.applyTextureToItemStack(item, content.value());
    }

    private static ItemStack modelItem(HeadContent content) {
        var item = new ItemStack(itemMaterial(content.value(), Material.BARRIER));

        var modelData = content.optionInt("customModelData");
        if (modelData != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(modelData);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private static Material itemMaterial(String name, Material fallback) {
        var material = Material.matchMaterial(name);
        return material != null && material.isItem() ? material : fallback;
    }
}
