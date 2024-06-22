package fr.aerwyn81.headblocks.data.head.types;

import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.HeadType;
import org.bukkit.inventory.ItemStack;

public class HBHeadHDB extends HBHead {
    private final String id;
    private boolean loaded;

    public HBHeadHDB(ItemStack head, String hdbId) {
        setItemStack(head);
        this.id = hdbId;
        this.loaded = false;
    }

    public String getId() {
        return id;
    }

    public HeadType getType() {
        return HeadType.HDB;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
}
