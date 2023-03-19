package fr.aerwyn81.headblocks.data.head;

import fr.aerwyn81.headblocks.managers.HeadManager;
import fr.aerwyn81.headblocks.services.TrackService;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class HBTrack {
    private final String id;
    private final String name;
    private final ArrayList<String> description;
    private final ItemStack icon;
    private final HeadManager headManager;

    public HBTrack(String id, String name, ArrayList<String> description, ItemStack icon, HeadManager headManager) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.headManager = headManager;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColorizedName() {
        return MessageUtils.colorize(name);
    }

    public ArrayList<String> getDescription() {
        return description;
    }

    public ArrayList<String> getColorizedDescription() {
        return description.stream().map(MessageUtils::colorize).collect(Collectors.toCollection(ArrayList::new));
    }

    public ItemStack getIcon() {
        return icon;
    }

    public HeadManager getHeadManager() {
        return headManager;
    }

    public void saveTrack() {
        FileConfiguration config = TrackService.getConfig(id);
        if (config == null) {
            return;
        }

        config.set("track.name", name);
        config.set("track.description", description);
        config.set("track.icon", icon.getType().name());

        headManager.saveHeadLocations();

        TrackService.saveConfig(id);
    }


}
