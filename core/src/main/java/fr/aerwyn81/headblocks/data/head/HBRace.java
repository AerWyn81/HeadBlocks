package fr.aerwyn81.headblocks.data.head;

import fr.aerwyn81.headblocks.managers.HeadManager;
import fr.aerwyn81.headblocks.services.RaceService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public class HBRace {
    private final String id;
    private final String name;
    private final String description;
    private final ItemStack icon;
    private final HeadManager headManager;

    public HBRace(String id, String name, String description, ItemStack icon) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.headManager = new HeadManager();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public HeadManager getHeadManager() {
        return headManager;
    }

    public void saveRace() {
        FileConfiguration config = RaceService.getConfig(id);
        if (config == null) {
            return;
        }

        config.set("race.name", name);
        config.set("race.description", description);
        config.set("race.icon", icon.getType().name());

        headManager.saveHeadLocations(config);

        RaceService.saveConfig(id);
    }


}
