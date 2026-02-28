package fr.aerwyn81.headblocks.data.hunt;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.BehaviorResult;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import org.bukkit.entity.Player;

import java.util.*;

public class Hunt {
    private final String id;
    private String displayName;
    private HuntState state;
    private int priority;
    private String icon;
    private List<Behavior> behaviors;
    private HuntConfig config;
    private final Set<UUID> headUUIDs;

    public Hunt(String id, String displayName, HuntState state, int priority, String icon) {
        this.id = id;
        this.displayName = displayName;
        this.state = state;
        this.priority = priority;
        this.icon = icon;
        this.behaviors = new ArrayList<>();
        this.behaviors.add(new FreeBehavior());
        this.config = new HuntConfig();
        this.headUUIDs = new HashSet<>();
    }

    // --- Core identity ---

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    // --- State ---

    public HuntState getState() {
        return state;
    }

    public void setState(HuntState state) {
        this.state = state;
    }

    public boolean isActive() {
        return state == HuntState.ACTIVE;
    }

    // --- Priority ---

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    // --- Icon ---

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    // --- Behaviors ---

    public List<Behavior> getBehaviors() {
        return behaviors;
    }

    public void setBehaviors(List<Behavior> behaviors) {
        this.behaviors = behaviors != null && !behaviors.isEmpty() ? behaviors : List.of(new FreeBehavior());
    }

    /**
     * Evaluates all behaviors in chain.
     * If any behavior denies the click, the chain stops and the deny result is returned.
     */
    public BehaviorResult evaluateBehaviors(Player player, HeadLocation head) {
        for (Behavior behavior : behaviors) {
            BehaviorResult result = behavior.canPlayerClick(player, head, this);
            if (!result.allowed()) {
                return result;
            }
        }
        return BehaviorResult.allow();
    }

    /**
     * Notifies all behaviors that a head was found.
     */
    public void notifyHeadFound(Player player, HeadLocation head) {
        for (Behavior behavior : behaviors) {
            behavior.onHeadFound(player, head, this);
        }
    }

    // --- Config ---

    public HuntConfig getConfig() {
        return config;
    }

    public void setConfig(HuntConfig config) {
        this.config = config != null ? config : new HuntConfig();
    }

    // --- Head management ---

    public Set<UUID> getHeadUUIDs() {
        return Collections.unmodifiableSet(headUUIDs);
    }

    public int getHeadCount() {
        return headUUIDs.size();
    }

    public boolean containsHead(UUID headUUID) {
        return headUUIDs.contains(headUUID);
    }

    public void addHead(UUID headUUID) {
        headUUIDs.add(headUUID);
    }

    public void removeHead(UUID headUUID) {
        headUUIDs.remove(headUUID);
    }

    public void clearHeads() {
        headUUIDs.clear();
    }

    // --- Utility ---

    public boolean isDefault() {
        return "default".equals(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hunt hunt = (Hunt) o;
        return id.equals(hunt.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Hunt{id='" + id + "', name='" + displayName + "', state=" + state + ", heads=" + headUUIDs.size() + "}";
    }
}
