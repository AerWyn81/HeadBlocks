package fr.aerwyn81.headblocks.data;

import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.UUID;

public class HeadLocation {
    private final UUID headUUID;
    private String name;
    private String huntId;

    private String configWorldName;
    private double x;
    private double y;
    private double z;

    private Location location;
    private boolean isCharged;
    private int orderIndex;
    private boolean hintSound;
    private boolean hintActionBar;

    private final ArrayList<Reward> rewards;

    public HeadLocation(String name, UUID headUUID, Location location, String huntId) {
        this(name, headUUID, huntId, location.getWorld() == null ? "" : location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), -1, false, false, new ArrayList<>());

        this.location = location;
        this.isCharged = true;
    }

    public HeadLocation(String name, UUID headUUID, String huntId, String configWorldName, double x, double y, double z, int orderIndex, boolean hintSound, boolean hintActionBar, ArrayList<Reward> rewards) {
        this.name = name;
        this.headUUID = headUUID;
        this.huntId = huntId;
        this.orderIndex = orderIndex;
        this.hintSound = hintSound;
        this.hintActionBar = hintActionBar;

        this.configWorldName = configWorldName;
        this.x = x;
        this.y = y;
        this.z = z;

        this.rewards = rewards;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getNameOrUnnamed(String unnamedLabel) {
        if (name == null || name.isEmpty()) {
            return unnamedLabel;
        }
        return MessageUtils.colorize(name);
    }

    public String getRawNameOrUuid() {
        if (name == null || name.isEmpty()) {
            return headUUID.toString();
        }

        return MessageUtils.unColorize(name);
    }

    public String getNameOrUuid() {
        if (name == null || name.isEmpty()) {
            return headUUID.toString();
        }

        return MessageUtils.colorize(name);
    }

    public UUID getUuid() {
        return headUUID;
    }

    public String getHuntId() {
        return huntId;
    }

    public void setHuntId(String huntId) {
        this.huntId = huntId;
    }

    public boolean isHintSoundEnabled() {
        return hintSound;
    }

    public void setHintSound(boolean isHintSound) {
        this.hintSound = isHintSound;
    }

    public boolean isHintActionBarEnabled() {
        return hintActionBar;
    }

    public void setHintActionBar(boolean isHintActionBar) {
        this.hintActionBar = isHintActionBar;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public String getDisplayedOrderIndex(String noOrderLabel) {
        if (orderIndex == -1) {
            return noOrderLabel;
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

        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.configWorldName = location.getWorld() != null ? location.getWorld().getName() : "";
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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
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

        if (location != null) {
            var world = location.getWorld();
            section.set("locations." + hUUID + ".location.x", location.getX());
            section.set("locations." + hUUID + ".location.y", location.getY());
            section.set("locations." + hUUID + ".location.z", location.getZ());
            section.set("locations." + hUUID + ".location.world", world == null ? "" : world.getName());
        } else {
            section.set("locations." + hUUID + ".location.x", x);
            section.set("locations." + hUUID + ".location.y", y);
            section.set("locations." + hUUID + ".location.z", z);
            section.set("locations." + hUUID + ".location.world", configWorldName);
        }

        section.set("locations." + hUUID + ".orderIndex", orderIndex == -1 ? null : orderIndex);
        section.set("locations." + hUUID + ".hintSound", !hintSound ? null : true);
        section.set("locations." + hUUID + ".hintActionBar", !hintActionBar ? null : true);

        if (!rewards.isEmpty()) {
            var confRewards = new ArrayList<>();
            for (Reward reward : rewards) {
                confRewards.add(reward.serialize());
            }

            section.set("locations." + hUUID + ".rewards", confRewards);
        }
    }

    public void removeFromConfig(YamlConfiguration section) {
        section.set("locations." + headUUID, null);
    }

    public static HeadLocation fromConfig(YamlConfiguration section, UUID headUUID, String huntId) {
        var hUUID = headUUID.toString();

        String name = section.getString("locations." + hUUID + ".name");

        double x, y, z;
        String worldName;

        if (name != null) {
            x = section.getDouble("locations." + hUUID + ".location.x");
            y = section.getDouble("locations." + hUUID + ".location.y");
            z = section.getDouble("locations." + hUUID + ".location.z");
            worldName = section.getString("locations." + hUUID + ".location.world", "");
        } else {
            x = section.getDouble("locations." + hUUID + ".x");
            y = section.getDouble("locations." + hUUID + ".y");
            z = section.getDouble("locations." + hUUID + ".z");
            worldName = section.getString("locations." + hUUID + ".world", "");
        }

        // Compatibility, centering head
        if (x % 1.0 == 0.0) {
            x += 0.5;
        }
        if (z % 1.0 == 0.0) {
            z += 0.5;
        }

        int orderIndex = -1;
        if (section.contains("locations." + hUUID + ".orderIndex")) {
            orderIndex = section.getInt("locations." + hUUID + ".orderIndex");
        }

        boolean hintSound = false;
        if (section.contains("locations." + hUUID + ".hintSound")) {
            hintSound = section.getBoolean("locations." + hUUID + ".hintSound");
        }

        boolean hintActionBar = false;
        if (section.contains("locations." + hUUID + ".hintActionBar")) {
            hintActionBar = section.getBoolean("locations." + hUUID + ".hintActionBar");
        }

        var rewards = new ArrayList<Reward>();
        if (section.contains("locations." + hUUID + ".rewards")) {
            var rewardsSection = section.get("locations." + hUUID + ".rewards");
            if (!(rewardsSection instanceof ArrayList<?>)) {
                LogUtil.error("Malformed rewards for head: {0}. Not a list of TYPE with VALUE.", headUUID);
            } else {
                for (var item : (ArrayList<?>) rewardsSection) {
                    var reward = Reward.deserialize(item);

                    if (reward.value().isEmpty())
                        LogUtil.warning("Ignored reward for head {0} because value is empty.", headUUID);
                    else
                        rewards.add(reward);
                }
            }
        }

        var headLocation = new HeadLocation(name, headUUID, huntId, worldName, x, y, z, orderIndex, hintSound, hintActionBar, rewards);

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            headLocation.setLocation(new Location(world, x, y, z));
            headLocation.setCharged(true);
        }

        return headLocation;
    }
}
