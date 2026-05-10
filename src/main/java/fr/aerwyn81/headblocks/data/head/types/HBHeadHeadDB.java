package fr.aerwyn81.headblocks.data.head.types;

import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.HeadType;
import fr.aerwyn81.headblocks.data.head.LoadableHead;
import org.bukkit.inventory.ItemStack;

public class HBHeadHeadDB extends HBHead implements LoadableHead {
    private int id;
    private boolean loaded;

    public HBHeadHeadDB(ItemStack head, int headDbId) {
        setItemStack(head);
        this.id = headDbId;
        this.loaded = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public HeadType getType() {
        return HeadType.HEADDB;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    @Override
    public String getDisplayId() {
        return String.valueOf(id);
    }
}
