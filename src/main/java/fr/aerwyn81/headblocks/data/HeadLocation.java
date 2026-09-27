package fr.aerwyn81.headblocks.data;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.head.visual.RenderMode;
import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HeadLocation {
    private static final String LOCATION_X = ".location.x";
    private static final String LOCATION_Y = ".location.y";
    private static final String LOCATION_Z = ".location.z";
    private static final String LOCATION_WORLD = ".location.world";
    private static final String LOCATIONS = "locations.";
    private static final String HINT_ACTION_BAR = ".hintActionBar";
    private static final String HINT_SOUND = ".hintSound";
    private static final String REWARDS = ".rewards";
    private static final String ORDER_INDEX = ".orderIndex";

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
    private HeadContent content;
    private float yaw;
    private RenderMode renderMode;

    private final List<Reward> rewards;

    public HeadLocation(String name, UUID headUUID, Location location, String huntId) {
        this(name, headUUID, huntId, location.getWorld() == null ? "" : location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), -1, false, false, new ArrayList<>());

        this.location = location;
        this.isCharged = true;
    }

    public HeadLocation(String name, UUID headUUID, String huntId, String configWorldName, double x, double y, double z, int orderIndex, boolean hintSound, boolean hintActionBar, List<Reward> rewards) {
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

        if (isHintSound) {
            this.hintActionBar = false;
        }
    }

    public boolean isHintActionBarEnabled() {
        return hintActionBar;
    }

    public void setHintActionBar(boolean isHintActionBar) {
        this.hintActionBar = isHintActionBar;

        if (isHintActionBar) {
            this.hintSound = false;
        }
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

    public HeadContent getContent() {
        return content;
    }

    public void setContent(HeadContent content) {
        this.content = content;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public RenderMode getRenderMode() {
        return renderMode;
    }

    public void setRenderMode(RenderMode renderMode) {
        this.renderMode = renderMode;
    }

    public void addReward(Reward reward) {
        this.rewards.add(reward);
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public void saveInConfig(YamlConfiguration section) {
        var hUUID = headUUID.toString();

        section.set(LOCATIONS + hUUID + ".name", name);

        saveCoordinates(section, hUUID);

        section.set(LOCATIONS + hUUID + ORDER_INDEX, orderIndex == -1 ? null : orderIndex);
        section.set(LOCATIONS + hUUID + HINT_SOUND, !hintSound ? null : true);
        section.set(LOCATIONS + hUUID + HINT_ACTION_BAR, !hintActionBar ? null : true);
        section.set(LOCATIONS + hUUID + ".yaw", yaw == 0f ? null : (double) yaw);
        section.set(LOCATIONS + hUUID + ".render", renderMode == null ? null : renderMode.name());

        if (content != null) {
            var headSection = section.getConfigurationSection(LOCATIONS + hUUID);
            if (headSection != null) {
                content.save(headSection, "content");
            }
        } else {
            section.set(LOCATIONS + hUUID + ".content", null);
        }

        if (!rewards.isEmpty()) {
            var confRewards = new ArrayList<>();
            for (Reward reward : rewards) {
                confRewards.add(reward.serialize());
            }

            section.set(LOCATIONS + hUUID + REWARDS, confRewards);
        }
    }

    private void saveCoordinates(YamlConfiguration section, String hUUID) {
        if (location != null) {
            var world = location.getWorld();
            section.set(LOCATIONS + hUUID + LOCATION_X, location.getX());
            section.set(LOCATIONS + hUUID + LOCATION_Y, location.getY());
            section.set(LOCATIONS + hUUID + LOCATION_Z, location.getZ());
            section.set(LOCATIONS + hUUID + LOCATION_WORLD, world == null ? "" : world.getName());
        } else {
            section.set(LOCATIONS + hUUID + LOCATION_X, x);
            section.set(LOCATIONS + hUUID + LOCATION_Y, y);
            section.set(LOCATIONS + hUUID + LOCATION_Z, z);
            section.set(LOCATIONS + hUUID + LOCATION_WORLD, configWorldName);
        }
    }

    public void removeFromConfig(YamlConfiguration section) {
        section.set(LOCATIONS + headUUID, null);
    }

    private static ArrayList<Reward> loadRewards(YamlConfiguration section, String hUUID, UUID headUUID) {
        var rewards = new ArrayList<Reward>();
        if (!section.contains(LOCATIONS + hUUID + REWARDS)) {
            return rewards;
        }

        var rewardsSection = section.get(LOCATIONS + hUUID + REWARDS);
        if (!(rewardsSection instanceof ArrayList<?> rewardList)) {
            LogUtil.error("Malformed rewards for head: {0}. Not a list of TYPE with VALUE.", headUUID);
            return rewards;
        }

        for (var item : rewardList) {
            var reward = Reward.deserialize(item);

            if (reward.value().isEmpty())
                LogUtil.warning("Ignored reward for head {0} because value is empty.", headUUID);
            else
                rewards.add(reward);
        }
        return rewards;
    }

    public static HeadLocation fromConfig(YamlConfiguration section, UUID headUUID, String huntId) {
        var hUUID = headUUID.toString();

        String name = section.getString(LOCATIONS + hUUID + ".name");

        double x;
        double y;
        double z;
        String worldName;

        if (name != null) {
            x = section.getDouble(LOCATIONS + hUUID + LOCATION_X);
            y = section.getDouble(LOCATIONS + hUUID + LOCATION_Y);
            z = section.getDouble(LOCATIONS + hUUID + LOCATION_Z);
            worldName = section.getString(LOCATIONS + hUUID + LOCATION_WORLD, "");
        } else {
            x = section.getDouble(LOCATIONS + hUUID + ".x");
            y = section.getDouble(LOCATIONS + hUUID + ".y");
            z = section.getDouble(LOCATIONS + hUUID + ".z");
            worldName = section.getString(LOCATIONS + hUUID + ".world", "");
        }

        // Compatibility, centering head
        if (x % 1.0 == 0.0) {
            x += 0.5;
        }
        if (z % 1.0 == 0.0) {
            z += 0.5;
        }

        int orderIndex = -1;
        if (section.contains(LOCATIONS + hUUID + ORDER_INDEX)) {
            orderIndex = section.getInt(LOCATIONS + hUUID + ORDER_INDEX);
        }

        boolean hintSound = false;
        if (section.contains(LOCATIONS + hUUID + HINT_SOUND)) {
            hintSound = section.getBoolean(LOCATIONS + hUUID + HINT_SOUND);
        }

        boolean hintActionBar = false;
        if (section.contains(LOCATIONS + hUUID + HINT_ACTION_BAR)) {
            hintActionBar = section.getBoolean(LOCATIONS + hUUID + HINT_ACTION_BAR);
        }

        if (hintSound && hintActionBar) {
            hintActionBar = false;
        }

        var rewards = loadRewards(section, hUUID, headUUID);

        var headLocation = new HeadLocation(name, headUUID, huntId, worldName, x, y, z, orderIndex, hintSound, hintActionBar, rewards);
        headLocation.setContent(HeadContent.load(section.getConfigurationSection(LOCATIONS + hUUID + ".content")));
        headLocation.setYaw((float) section.getDouble(LOCATIONS + hUUID + ".yaw", 0));
        headLocation.setRenderMode(RenderMode.of(section.getString(LOCATIONS + hUUID + ".render")));

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            headLocation.setLocation(new Location(world, x, y, z));
            headLocation.setCharged(true);
        }

        return headLocation;
    }
}
