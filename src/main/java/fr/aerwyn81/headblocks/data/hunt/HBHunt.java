package fr.aerwyn81.headblocks.data.hunt;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.BehaviorResult;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementResult;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementSet;
import fr.aerwyn81.headblocks.services.ConfigService;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HBHunt {
    private final String id;
    private final ConfigService configService;
    private String displayName;
    private HuntState state;
    private int priority;
    private String icon;
    private List<Behavior> behaviors;
    private RequirementSet requirements;
    private HuntConfig config;
    private final Set<UUID> headUUIDs;

    public HBHunt(ConfigService configService, String id, String displayName, HuntState state, int priority, String icon) {
        this.id = id;
        this.configService = configService;
        this.displayName = displayName;
        this.state = state;
        this.priority = priority;
        this.icon = icon;
        this.behaviors = new ArrayList<>();
        this.behaviors.add(new FreeBehavior());
        this.requirements = RequirementSet.empty();
        this.config = new HuntConfig(configService);
        this.headUUIDs = ConcurrentHashMap.newKeySet();
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

    /**
     * The configured icon as a material, falling back to the default one when the name does not
     * resolve (a typo in the file, or a material removed by a server version).
     */
    public Material getIconMaterial() {
        try {
            return Material.valueOf(icon.toUpperCase());
        } catch (Exception e) {
            return Material.CHEST_MINECART;
        }
    }

    // --- Behaviors ---

    public List<Behavior> getBehaviors() {
        return behaviors;
    }

    public void setBehaviors(List<Behavior> behaviors) {
        this.behaviors = behaviors != null && !behaviors.isEmpty() ? behaviors : List.of(new FreeBehavior());
    }

    /**
     * Evaluates only access-gate behaviors (e.g., scheduled).
     * These gate the entire hunt availability and should be checked before the "already found" check.
     */
    public BehaviorResult evaluateAccessGates(Player player, HeadLocation head) {
        for (Behavior behavior : behaviors) {
            if (behavior.isAccessGate()) {
                BehaviorResult result = behavior.canPlayerClick(player, head, this);
                if (!result.allowed()) {
                    return result;
                }
            }
        }
        return BehaviorResult.allow();
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

    // --- Requirements ---

    public RequirementSet getRequirements() {
        return requirements;
    }

    public void setRequirements(RequirementSet requirements) {
        this.requirements = requirements != null ? requirements : RequirementSet.empty();
    }

    /**
     * Evaluates the conditions the player must meet to claim a head of this hunt.
     * The denial carries every blocking reason at once, so the player knows what is missing.
     */
    public RequirementResult evaluateRequirements(Player player, HeadLocation head) {
        return requirements.evaluate(player, head, this);
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
        this.config = config != null ? config : new HuntConfig(configService);
    }

    // --- Head management ---

    public Set<UUID> getHeadUUIDs() {
        return Collections.unmodifiableSet(headUUIDs);
    }

    public int getHeadCount() {
        return headUUIDs.size();
    }

    public boolean isValid() {
        return !headUUIDs.isEmpty();
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HBHunt hunt = (HBHunt) o;
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
