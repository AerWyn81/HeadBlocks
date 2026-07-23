package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.zone.ZoneMessageMode;
import fr.aerwyn81.headblocks.data.hunt.behavior.zone.ZoneProvider;
import fr.aerwyn81.headblocks.data.hunt.behavior.zone.ZoneProviderFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class ZoneBehavior implements Behavior {

    private final ServiceRegistry registry;
    private final ZoneProvider zone;
    private final Location returnPoint;
    private final boolean blockExit;
    private final boolean resetOnLeave;
    private final ZoneMessageMode messageMode;

    public ZoneBehavior(ServiceRegistry registry, ZoneProvider zone, Location returnPoint,
                        boolean blockExit, boolean resetOnLeave, ZoneMessageMode messageMode) {
        this.registry = registry;
        this.zone = zone;
        this.returnPoint = returnPoint;
        this.blockExit = blockExit;
        this.resetOnLeave = resetOnLeave;
        this.messageMode = messageMode;
    }

    public ZoneProvider zone() {
        return zone;
    }

    public Location returnPoint() {
        return returnPoint;
    }

    public boolean blockExit() {
        return blockExit;
    }

    public boolean resetOnLeave() {
        return resetOnLeave;
    }

    public ZoneMessageMode messageMode() {
        return messageMode;
    }

    @Override
    public String getId() {
        return "zone";
    }

    @Override
    public BehaviorResult canPlayerClick(Player player, HeadLocation head, HBHunt hunt) {
        return BehaviorResult.allow();
    }

    @Override
    public void onHeadFound(Player player, HeadLocation head, HBHunt hunt) {
    }

    @Override
    public String getDisplayInfo(Player player, HBHunt hunt) {
        return registry.getLanguageService().message("Hunt.Behavior.Zone");
    }

    public static ZoneBehavior fromConfig(ServiceRegistry registry, ConfigurationSection section) {
        if (section == null) {
            return new ZoneBehavior(registry, null, null, false, false, ZoneMessageMode.CHAT);
        }

        ZoneProvider zone = ZoneProviderFactory.fromSection(section.getConfigurationSection("zone"));
        Location returnPoint = readReturnPoint(section.getConfigurationSection("returnPoint"));
        boolean blockExit = section.getBoolean("blockExit", false);
        boolean resetOnLeave = section.getBoolean("resetOnLeave", false);
        ZoneMessageMode messageMode = ZoneMessageMode.fromString(section.getString("messageMode"));

        return new ZoneBehavior(registry, zone, returnPoint, blockExit, resetOnLeave, messageMode);
    }

    private static Location readReturnPoint(ConfigurationSection section) {
        if (section == null || !section.contains("world")) {
            return null;
        }

        World world = Bukkit.getWorld(section.getString("world", ""));
        if (world == null) {
            return null;
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }
}
