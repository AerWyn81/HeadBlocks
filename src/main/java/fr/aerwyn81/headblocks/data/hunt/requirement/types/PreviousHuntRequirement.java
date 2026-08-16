package fr.aerwyn81.headblocks.data.hunt.requirement.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementResult;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * The player must have progressed far enough in another hunt.
 * <p>
 * {@code requiredHeads} is the number of heads to find there; {@code 0} means the whole hunt, which
 * keeps following the target hunt as heads are added to it.
 */
public class PreviousHuntRequirement implements Requirement {

    public static final int ALL_HEADS = 0;

    private final ServiceRegistry registry;
    private final String huntId;
    private final int requiredHeads;

    public PreviousHuntRequirement(ServiceRegistry registry, String huntId, int requiredHeads) {
        this.registry = registry;
        this.huntId = huntId;
        this.requiredHeads = Math.max(ALL_HEADS, requiredHeads);
    }

    public String huntId() {
        return huntId;
    }

    public int requiredHeads() {
        return requiredHeads;
    }

    @Override
    public RequirementType getType() {
        return RequirementType.PREVIOUS_HUNT;
    }

    @Override
    public RequirementResult check(Player player, HeadLocation head, HBHunt hunt) {
        HBHunt target = registry.getHuntService().getHuntById(huntId);
        if (target == null) {
            // The referenced hunt is gone: blocking players on a hunt that no longer exists would be
            // a dead end nobody can resolve.
            LogUtil.warning("Hunt {0} requires the unknown hunt {1}, ignoring that requirement.",
                    hunt.getId(), huntId);
            return RequirementResult.unresolvable();
        }

        int needed = neededHeads(target);
        if (needed <= 0) {
            return RequirementResult.ok();
        }

        int found;
        try {
            found = registry.getStorageService().getHeadsPlayerForHunt(player.getUniqueId(), huntId).size();
        } catch (InternalException e) {
            LogUtil.error("Error checking the hunt requirement on {0}: {1}", huntId, e.getMessage());
            return RequirementResult.unresolvable();
        }

        if (found >= needed) {
            return RequirementResult.ok();
        }

        String key = requiredHeads == ALL_HEADS
                ? "Hunt.Requirement.HuntUnmetAll"
                : "Hunt.Requirement.HuntUnmetCount";

        return RequirementResult.unmet(registry.getLanguageService().message(key)
                .replace("%hunt%", target.getDisplayName())
                .replace("%found%", String.valueOf(found))
                .replace("%required%", String.valueOf(needed)));
    }

    private int neededHeads(HBHunt target) {
        return requiredHeads == ALL_HEADS ? target.getHeadCount() : requiredHeads;
    }

    @Override
    public String describe() {
        HBHunt target = registry.getHuntService().getHuntById(huntId);
        String name = target != null ? target.getDisplayName() : huntId;

        String key = requiredHeads == ALL_HEADS
                ? "Hunt.Requirement.HuntDescriptionAll"
                : "Hunt.Requirement.HuntDescriptionCount";

        return registry.getLanguageService().message(key)
                .replace("%hunt%", name)
                .replace("%required%", String.valueOf(requiredHeads));
    }

    @Override
    public boolean isComplete() {
        return huntId != null && !huntId.isEmpty();
    }

    @Override
    public void saveTo(ConfigurationSection section) {
        section.set("hunt", huntId);
        section.set("heads", requiredHeads);
    }

    public static PreviousHuntRequirement fromConfig(ServiceRegistry registry, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        return new PreviousHuntRequirement(registry,
                section.getString("hunt"),
                section.getInt("heads", ALL_HEADS));
    }
}
