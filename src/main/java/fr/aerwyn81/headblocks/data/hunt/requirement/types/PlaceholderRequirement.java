package fr.aerwyn81.headblocks.data.hunt.requirement.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementResult;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * The player must satisfy a comparison on a PlaceholderAPI value, e.g.
 * {@code %vault_eco_balance% >= 500}.
 */
public class PlaceholderRequirement implements Requirement {

    private final ServiceRegistry registry;
    private final String placeholder;
    private final ComparisonOperator operator;
    private final String expected;

    public PlaceholderRequirement(ServiceRegistry registry, String placeholder,
                                  ComparisonOperator operator, String expected) {
        this.registry = registry;
        this.placeholder = placeholder;
        this.operator = operator != null ? operator : ComparisonOperator.EQUALS;
        this.expected = expected;
    }

    public String placeholder() {
        return placeholder;
    }

    public ComparisonOperator operator() {
        return operator;
    }

    public String expected() {
        return expected;
    }

    @Override
    public RequirementType getType() {
        return RequirementType.PLACEHOLDER;
    }

    @Override
    public RequirementResult check(Player player, HeadLocation head, HBHunt hunt) {
        // PlaceholderAPI may have been removed since the requirement was configured: blocking every
        // click on an expression nobody can resolve would lock the hunt down.
        if (!registry.getPluginProvider().isPlaceholderApiActive()) {
            LogUtil.warning("Hunt {0} has a placeholder requirement but PlaceholderAPI is missing, ignoring it.",
                    hunt.getId());
            return RequirementResult.unresolvable();
        }

        String actual = PlaceholderAPI.setPlaceholders(player, placeholder);
        if (operator.test(actual, expected)) {
            return RequirementResult.ok();
        }

        return RequirementResult.unmet(registry.getLanguageService().message("Hunt.Requirement.PlaceholderUnmet")
                .replace("%placeholder%", placeholder)
                .replace("%operator%", operator.getSymbol())
                .replace("%expected%", String.valueOf(expected))
                .replace("%actual%", String.valueOf(actual)));
    }

    @Override
    public String describe() {
        return registry.getLanguageService().message("Hunt.Requirement.PlaceholderDescription")
                .replace("%placeholder%", placeholder)
                .replace("%operator%", operator.getSymbol())
                .replace("%expected%", String.valueOf(expected));
    }

    @Override
    public boolean isComplete() {
        return placeholder != null && !placeholder.trim().isEmpty()
                && expected != null && !expected.trim().isEmpty();
    }

    @Override
    public void saveTo(ConfigurationSection section) {
        section.set("placeholder", placeholder);
        section.set("operator", operator.getSymbol());
        section.set("value", expected);
    }

    public static PlaceholderRequirement fromConfig(ServiceRegistry registry, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        return new PlaceholderRequirement(registry,
                section.getString("placeholder"),
                ComparisonOperator.fromSymbol(section.getString("operator")),
                section.getString("value"));
    }
}
