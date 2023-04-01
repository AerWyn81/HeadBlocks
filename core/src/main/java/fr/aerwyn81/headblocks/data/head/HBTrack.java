package fr.aerwyn81.headblocks.data.head;

import fr.aerwyn81.headblocks.managers.HeadManager;
import fr.aerwyn81.headblocks.services.TrackService;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class HBTrack {
    private final String id;
    private final String name;
    private final String displayName;
    private final ArrayList<String> description;
    private final ItemStack icon;
    private final HeadManager headManager;

    public HBTrack(String id, String name, String displayName, ArrayList<String> description, ItemStack icon, HeadManager headManager) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
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

    public String getDisplayName() {
        return MessageUtils.colorize(displayName);
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

    public int getHeadCount() {
        return getHeadManager().getHeadLocations().size();
    }

    public void saveTrack() {
        FileConfiguration config = TrackService.getConfig(id);
        if (config == null) {
            return;
        }

        config.set("track.name", name);
        config.set("track.displayName", displayName);
        config.set("track.description", description);
        config.set("track.icon", icon.getType().name());

        headManager.saveHeadLocations();

        TrackService.saveConfig(id);
    }

    public static HBTrack fromConfig(YamlConfiguration config, String id) {
        String name = config.getString("track.name");
        String displayName = config.getString("track.displayName");

        ArrayList<String> description = new ArrayList<>(config.getStringList("track.description"));

        ItemStack iconItem;
        try {
            iconItem = new ItemStack(Material.valueOf(config.getString("track.icon", "PAPER")));
        } catch (Exception ex) {
            iconItem = new ItemStack(Material.PAPER);
        }

        var manager = new HeadManager(config);

        HBTrack track;
        track = new HBTrack(id, name, displayName, description, iconItem, manager);
        manager.setTrack(track);

        return track;
    }
}
