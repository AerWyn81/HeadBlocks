package fr.aerwyn81.headblocks.data.head;

import org.bukkit.inventory.ItemStack;

public class Head {
    private final String id;

    private ItemStack head;
    private String texture;

    private final HeadType headType;

    private boolean loaded;

    public Head(String id, ItemStack head, String texture, HeadType headType) {
        this.id = id;
        this.head = head;
        this.texture = texture;
        this.headType = headType;

        this.loaded = headType == HeadType.PLAYER;
    }

    public String getId() {
        return id;
    }

    public ItemStack getHead() {
        return head;
    }

    public void setHead(ItemStack head) {
        this.head = head;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public HeadType getHeadType() {
        return headType;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
}

