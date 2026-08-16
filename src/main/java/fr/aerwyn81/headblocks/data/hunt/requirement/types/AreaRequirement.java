package fr.aerwyn81.headblocks.data.hunt.requirement.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementResult;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaMessageMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaProviderFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * The player must stand inside a delimited area to claim the heads of the hunt.
 * <p>
 * On top of the click check, this requirement carries the confinement options enforced by
 * {@code AreaEnforcementService}: blocking the exit, sending the player back to a return point and
 * wiping the progress of whoever leaves.
 */
public class AreaRequirement implements Requirement {

    private final ServiceRegistry registry;
    private final AreaProvider area;
    private final Location returnPoint;
    private final boolean blockExit;
    private final boolean resetOnLeave;
    private final AreaMessageMode messageMode;

    /**
     * Runtime state, not persisted: an area the server cannot enforce (no head assigned yet, heads
     * outside of it, ...) is switched off rather than dropped, so the admin configuration survives.
     */
    private volatile boolean disabled;

    public AreaRequirement(ServiceRegistry registry, AreaProvider area, Location returnPoint,

                           boolean blockExit, boolean resetOnLeave, AreaMessageMode messageMode) {
        this.registry = registry;
        this.area = area;
        this.returnPoint = returnPoint;
        this.blockExit = blockExit;
        this.resetOnLeave = resetOnLeave;
        this.messageMode = messageMode != null ? messageMode : AreaMessageMode.CHAT;
    }

    public AreaProvider area() {
        return area;
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

    public AreaMessageMode messageMode() {
        return messageMode;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public RequirementType getType() {
        return RequirementType.AREA;
    }

    @Override
    public RequirementResult check(Player player, HeadLocation head, HBHunt hunt) {
        // An unresolvable area (unloaded world, missing WorldGuard region) or one the server refused
        // to enforce would lock the hunt for everyone: let the click through instead.
        if (disabled || area == null || !area.isAvailable()) {
            return RequirementResult.unresolvable();
        }

        if (area.contains(player.getLocation())) {
            return RequirementResult.ok();
        }

        return RequirementResult.unmet(registry.getLanguageService().message("Hunt.Requirement.AreaUnmet"));
    }

    @Override
    public String describe() {
        String value = area != null
                ? area.getDescription()
                : registry.getLanguageService().message("Gui.RequirementNotDefined");

        return registry.getLanguageService().message("Hunt.Requirement.AreaDescription")
                .replace("%area%", value);
    }

    @Override
    public boolean isComplete() {
        return area != null && (!blockExit || returnPoint != null);
    }

    @Override
    public void saveTo(ConfigurationSection section) {
        section.set("blockExit", blockExit);
        section.set("resetOnLeave", resetOnLeave);
        section.set("messageMode", messageMode.name());

        if (area != null) {
            area.saveTo(section.createSection("area"));
        }

        if (returnPoint != null && returnPoint.getWorld() != null) {
            ConfigurationSection point = section.createSection("returnPoint");
            point.set("world", returnPoint.getWorld().getName());
            point.set("x", returnPoint.getX());
            point.set("y", returnPoint.getY());
            point.set("z", returnPoint.getZ());
            point.set("yaw", returnPoint.getYaw());
            point.set("pitch", returnPoint.getPitch());
        }
    }

    public static AreaRequirement fromConfig(ServiceRegistry registry, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        // "zone" is the key used before areas became requirements; still read for migrated files.
        ConfigurationSection areaSection = section.getConfigurationSection("area");
        if (areaSection == null) {
            areaSection = section.getConfigurationSection("zone");
        }

        AreaProvider area = AreaProviderFactory.fromSection(areaSection);
        Location returnPoint = readReturnPoint(section.getConfigurationSection("returnPoint"));
        boolean blockExit = section.getBoolean("blockExit", false);
        boolean resetOnLeave = section.getBoolean("resetOnLeave", false);
        AreaMessageMode messageMode = AreaMessageMode.fromString(section.getString("messageMode"));

        return new AreaRequirement(registry, area, returnPoint, blockExit, resetOnLeave, messageMode);
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
