package fr.aerwyn81.headblocks.services.gui.types.requirement.editors;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.ComparisonOperator;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.PlaceholderRequirement;
import fr.aerwyn81.headblocks.services.gui.types.requirement.AbstractRequirementEditor;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Asks for the placeholder, the operator and the expected value.
 */
public class PlaceholderRequirementEditor extends AbstractRequirementEditor {
    private static final int ROWS = 3;
    private static final int PLACEHOLDER_SLOT = 11;
    private static final int OPERATOR_SLOT = 13;
    private static final int VALUE_SLOT = 15;
    private static final int VALIDATE_SLOT = 22;

    private final ConcurrentHashMap<UUID, String> placeholders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ComparisonOperator> operators = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> values = new ConcurrentHashMap<>();

    public PlaceholderRequirementEditor(ServiceRegistry registry) {
        super(registry);
    }

    @Override
    public RequirementType getType() {
        return RequirementType.PLACEHOLDER;
    }

    @Override
    public void open(Player player, Requirement existing, Consumer<Requirement> onDone, Consumer<Player> onCancel) {
        rememberCallbacks(player, onDone, onCancel);

        UUID uuid = player.getUniqueId();
        placeholders.remove(uuid);
        values.remove(uuid);
        operators.put(uuid, ComparisonOperator.EQUALS);

        if (existing instanceof PlaceholderRequirement placeholder) {
            if (placeholder.placeholder() != null) {
                placeholders.put(uuid, placeholder.placeholder());
            }
            if (placeholder.expected() != null) {
                values.put(uuid, placeholder.expected());
            }
            operators.put(uuid, placeholder.operator());
        }

        reopen(player);
    }

    private void reopen(Player player) {
        UUID uuid = player.getUniqueId();
        String placeholder = placeholders.get(uuid);
        String value = values.get(uuid);
        ComparisonOperator operator = operators.getOrDefault(uuid, ComparisonOperator.EQUALS);

        var menu = newMenu("Gui.RequirementPlaceholderTitle", ROWS);
        fillBorders(menu, ROWS);

        menu.setItem(0, PLACEHOLDER_SLOT, fieldItem(Material.PAPER,
                "Gui.RequirementPlaceholderValue", "Gui.RequirementPlaceholderValueLore", "%placeholder%",
                placeholder != null ? MessageUtils.colorize("&a" + placeholder) : notDefined())
                .addOnClickEvent(event -> promptPlaceholder((Player) event.getWhoClicked())));

        menu.setItem(0, OPERATOR_SLOT, fieldItem(Material.COMPARATOR,
                "Gui.RequirementPlaceholderOperator", "Gui.RequirementPlaceholderOperatorLore", "%operator%",
                MessageUtils.colorize("&a" + operator.getSymbol()))
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    operators.put(p.getUniqueId(), operator.next());
                    reopen(p);
                }));

        menu.setItem(0, VALUE_SLOT, fieldItem(Material.NAME_TAG,
                "Gui.RequirementPlaceholderExpected", "Gui.RequirementPlaceholderExpectedLore", "%value%",
                value != null ? MessageUtils.colorize("&a" + value) : notDefined())
                .addOnClickEvent(event -> promptValue((Player) event.getWhoClicked())));

        boolean ready = placeholder != null && !placeholder.isEmpty() && value != null && !value.isEmpty();
        menu.setItem(0, VALIDATE_SLOT, ready
                ? validateItem(() -> finish(player,
                new PlaceholderRequirement(registry, placeholder, operator, value)))
                : blockedValidateItem("Gui.RequirementPlaceholderBlockedLore"));

        attachBackButton(menu);

        player.openInventory(menu.getInventory());
    }

    private void promptPlaceholder(Player player) {
        registry.getChatPromptService().prompt(player,
                registry.getLanguageService().message("Messages.RequirementPlaceholderPrompt"),
                input -> {
                    placeholders.put(player.getUniqueId(), input);
                    reopen(player);
                },
                this::reopen);
    }

    private void promptValue(Player player) {
        registry.getChatPromptService().prompt(player,
                registry.getLanguageService().message("Messages.RequirementPlaceholderValuePrompt"),
                input -> {
                    values.put(player.getUniqueId(), input);
                    reopen(player);
                },
                this::reopen);
    }

    @Override
    protected void clearFields(UUID playerUuid) {
        placeholders.remove(playerUuid);
        operators.remove(playerUuid);
        values.remove(playerUuid);
    }
}
