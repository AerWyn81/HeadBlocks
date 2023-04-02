package fr.aerwyn81.headblocks.data;

import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.managers.HeadManager;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

public class HeadLocation {
    private final String name;
    private final UUID headUUID;
    private final ArrayList<String> description;
    private final String texture;

    private String configWorldName;
    private int x;
    private int y;
    private int z;

    private Location location;
    private boolean isCharged;
    private int hitCount;
    private int orderIndex;

    private final ArrayList<Reward> rewards;

    private HeadManager headManager;

    public HeadLocation(String name, ArrayList<String> description, UUID headUUID, String texture, Location location, HeadManager headManager) {
        this(name, description, headUUID, texture, location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), -1, -1, new ArrayList<>());

        this.location = location;
        this.isCharged = true;
        this.headManager = headManager;
    }

    public HeadLocation(String name, ArrayList<String> description, UUID headUUID, String texture, String configWorldName, int x, int y, int z, int hitCount, int orderIndex, ArrayList<Reward> rewards) {
        this.name = name;
        this.description = description;
        this.headUUID = headUUID;
        this.texture = texture;

        this.hitCount = hitCount;
        this.orderIndex = orderIndex;

        this.configWorldName = configWorldName;
        this.x = x;
        this.y = y;
        this.z = z;

        this.rewards = rewards;
    }

    public String getName() {
        return name;
    }

    public String getDisplayedName() {
        if (name.isEmpty())
            return LanguageService.getMessage("Gui.Unnamed");

        return MessageUtils.colorize(name);
    }

    public ArrayList<String> getDisplayedDescription() {
        return description.stream().map(MessageUtils::colorize).collect(Collectors.toCollection(ArrayList::new));
    }

    public String getTexture() {
        return texture;
    }

    public UUID getUuid() {
        return headUUID;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public String getDisplayedHitCount() {
        if (hitCount == -1) {
            return LanguageService.getMessage("Gui.Infinite");
        }

        return String.valueOf(hitCount);
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public ArrayList<String> getDescription() {
        return description;
    }

    public String getDisplayedOrderIndex() {
        if (orderIndex == -1) {
            return MessageUtils.colorize("&7No order");
        }

        return String.valueOf(orderIndex);
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;

        this.x = -1;
        this.y = -1;
        this.z = -1;
        this.configWorldName = "";
    }

    public boolean isCharged() {
        return isCharged;
    }

    public void setCharged(boolean charged) {
        isCharged = charged;
    }

    public String getConfigWorldName() {
        return configWorldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public void addReward(Reward reward) {
        this.rewards.add(reward);
    }

    public ArrayList<Reward> getRewards() {
        return rewards;
    }

    public HeadManager getHeadManager() {
        return headManager;
    }

    public void saveInConfig(YamlConfiguration section) {
        var hUUID = headUUID.toString();

        section.set("locations." + hUUID + ".name", name);
        section.set("locations." + hUUID + ".description", description);
        section.set("locations." + hUUID + ".texture", texture);
        section.set("locations." + hUUID + ".location.x", location.getBlockX());
        section.set("locations." + hUUID + ".location.y", location.getBlockY());
        section.set("locations." + hUUID + ".location.z", location.getBlockZ());
        section.set("locations." + hUUID + ".location.world", location.getWorld().getName());

        section.set("locations." + hUUID + ".hitCount", hitCount == -1 ? null : hitCount);
        section.set("locations." + hUUID + ".orderIndex", orderIndex == -1 ? null : orderIndex);

        if (rewards.size() != 0) {
            section.createSection("locations." + hUUID + ".rewards");

            for (Reward reward : rewards) {
                //todo
            }
        }
    }

    public void removeFromConfig(YamlConfiguration section) {
        section.set("locations." + headUUID, null);
    }

    public static HeadLocation fromConfig(YamlConfiguration section, UUID headUUID) {
        var hUUID = headUUID.toString();

        var name = section.getString("locations." + hUUID + ".name");
        var description = new ArrayList<>(section.getStringList("locations." + hUUID + ".description"));
        var texture = section.getString("locations." + hUUID + ".texture", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQ4NDNlYTVmZTA5MzMxNWQ4YjNmNDAxNWIyYTZjMmNjMjNhZTUyZThhYWIxNDczYmJmMmY2MjM1NDJmNTI1YiJ9fX0=");

        int x, y, z;
        String worldName;

        if (name != null) {
           x = section.getInt("locations." + hUUID + ".location.x");
           y = section.getInt("locations." + hUUID + ".location.y");
           z = section.getInt("locations." + hUUID + ".location.z");
           worldName = section.getString("locations." + hUUID + ".location.world", "");
        } else {
            x = section.getInt("locations." + hUUID + ".x");
            y = section.getInt("locations." + hUUID + ".y");
            z = section.getInt("locations." + hUUID + ".z");
            worldName = section.getString("locations." + hUUID + ".world", "");
        }

        int hitCount = -1;
        if (section.contains("locations." + hUUID + ".hitCount")) {
            hitCount = section.getInt("locations." + hUUID + ".hitCount");
        }

        int orderIndex = -1;
        if (section.contains("locations." + hUUID + ".orderIndex")) {
            orderIndex = section.getInt("locations." + hUUID + ".orderIndex");
        }

        var rewards = new ArrayList<Reward>();
        if (section.contains("locations." + hUUID + "rewards")) {
            //todo
        }

        var headLocation = new HeadLocation(name, description, headUUID, texture, worldName, x, y, z, hitCount, orderIndex, rewards);

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            headLocation.setLocation(new Location(world, x, y, z));
            headLocation.setCharged(true);
        }

        return headLocation;
    }
}
