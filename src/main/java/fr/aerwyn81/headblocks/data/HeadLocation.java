package fr.aerwyn81.headblocks.data;

import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.services.LanguageService;
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

    private String configWorldName;
    private int x;
    private int y;
    private int z;

    private Location location;
    private boolean isCharged;
    private int hitCount;
    private int orderIndex;
    private boolean hintSound;
    private boolean hintActionBar;

    private final ArrayList<Reward> rewards;

    public HeadLocation(String name, UUID headUUID, Location location) {
        this(name, headUUID, location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), -1, -1, false, false, new ArrayList<>());

        this.location = location;
        this.isCharged = true;
    }

    public HeadLocation(String name, UUID headUUID, String configWorldName, int x, int y, int z, int hitCount, int orderIndex, boolean hintSound, boolean hintActionBar, ArrayList<Reward> rewards) {
        this.name = name;
        this.headUUID = headUUID;
        this.hitCount = hitCount;
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

    public String getNameOrUnnamed() {
        if (name.isEmpty())
            return LanguageService.getMessage("Gui.Unnamed");

        return MessageUtils.colorize(name);
    }

    public String getRawNameOrUuid() {
        if (name.isEmpty())
            return headUUID.toString();

        return MessageUtils.unColorize(name);
    }

    public String getNameOrUuid() {
        if (name.isEmpty())
            return headUUID.toString();

        return MessageUtils.colorize(name);
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

    public String getDisplayedHitCount() {
        if (hitCount == -1) {
            return LanguageService.getMessage("Gui.Infinite");
        }

        return String.valueOf(hitCount);
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public String getDisplayedOrderIndex() {
        if (orderIndex == -1) {
            return LanguageService.getMessage("Gui.NoOrder");
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

    public void saveInConfig(YamlConfiguration section) {
        var hUUID = headUUID.toString();

        section.set("locations." + hUUID + ".name", name);
        section.set("locations." + hUUID + ".location.x", location.getBlockX());
        section.set("locations." + hUUID + ".location.y", location.getBlockY());
        section.set("locations." + hUUID + ".location.z", location.getBlockZ());
        section.set("locations." + hUUID + ".location.world", location.getWorld().getName());

        section.set("locations." + hUUID + ".hitCount", hitCount == -1 ? null : hitCount);
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

    public static HeadLocation fromConfig(YamlConfiguration section, UUID headUUID) {
        var hUUID = headUUID.toString();

        String name = section.getString("locations." + hUUID + ".name");

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

                    if (reward.getValue().isEmpty())
                        LogUtil.warning("Ignored reward for head {0} because value is empty.", headUUID);
                    else
                        rewards.add(reward);
                }
            }
        }

        var headLocation = new HeadLocation(name, headUUID, worldName, x, y, z, hitCount, orderIndex, hintSound, hintActionBar, rewards);

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            headLocation.setLocation(new Location(world, x, y, z));
            headLocation.setCharged(true);
        }

        return headLocation;
    }
}
