package fr.aerwyn81.headblocks.data;

import fr.aerwyn81.headblocks.data.reward.Reward;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.UUID;

public class HeadLocation {
    private final String name;
    private final UUID headUUID;

    private String configWorldName;
    private int x;
    private int y;
    private int z;

    private Location location;
    private boolean isCharged;
    private int hitCount;
    private int orderIndex;

    private final ArrayList<Reward> rewards;

    public HeadLocation(String name, UUID headUUID, Location location) {
        this(name, headUUID, location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), -1, -1, new ArrayList<>());

        this.location = location;
        this.isCharged = true;
    }

    public HeadLocation(String name, UUID headUUID, String configWorldName, int x, int y, int z, int hitCount, int orderIndex, ArrayList<Reward> rewards) {
        this.name = name;
        this.headUUID = headUUID;
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

    public UUID getUuid() {
        return headUUID;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public int getOrderIndex() {
        return orderIndex;
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

    public void saveInConfig(YamlConfiguration section) {
        var hUUID = headUUID.toString();

        section.set("locations." + hUUID + ".name", name);
        section.set("locations." + hUUID + ".location.x", location.getBlockX());
        section.set("locations." + hUUID + ".location.y", location.getBlockY());
        section.set("locations." + hUUID + ".location.z", location.getBlockZ());
        section.set("locations." + hUUID + ".location.world", location.getWorld().getName());

        if (hitCount != -1) {
            section.set("locations." + hUUID + ".hitCount", hitCount);
        }

        if (orderIndex != -1) {
            section.set("locations." + hUUID + ".orderIndex", orderIndex);
        }

        if (rewards.size() != 0) {
            section.createSection("locations." + hUUID + ".rewards");

            for (Reward reward : rewards) {
                reward.serialize(section.getConfigurationSection("locations." + hUUID + ".rewards"));
            }
        }
    }

    public void removeFromConfig(YamlConfiguration section) {
        section.set("locations." + headUUID, null);
    }

    public static HeadLocation fromConfig(YamlConfiguration section, UUID headUUID) {
        var hUUID = headUUID.toString();

        String name = section.getString("locations." + hUUID + ".name");

        var x = section.getInt("locations." + hUUID + ".location.x");
        var y = section.getInt("locations." + hUUID + ".location.y");
        var z = section.getInt("locations." + hUUID + ".location.z");
        var worldName = section.getString("locations." + hUUID + ".location.world", "");

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

        var headLocation = new HeadLocation(name, headUUID, worldName, x, y, z, hitCount, orderIndex, rewards);

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            headLocation.setLocation(new Location(world, x, y, z));
            headLocation.setCharged(true);
        }

        return headLocation;
    }
}
