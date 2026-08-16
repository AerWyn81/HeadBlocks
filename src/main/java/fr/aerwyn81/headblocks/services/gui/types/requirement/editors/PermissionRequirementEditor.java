package fr.aerwyn81.headblocks.services.gui.types.requirement.editors;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.PermissionRequirement;
import fr.aerwyn81.headblocks.services.gui.types.requirement.AbstractRequirementEditor;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Asks for the permission node.
 */
public class PermissionRequirementEditor extends AbstractRequirementEditor {
    private static final int ROWS = 2;
    private static final int NODE_SLOT = 11;
    private static final int VALIDATE_SLOT = 15;

    private final ConcurrentHashMap<UUID, String> nodes = new ConcurrentHashMap<>();

    public PermissionRequirementEditor(ServiceRegistry registry) {
        super(registry);
    }

    @Override
    public RequirementType getType() {
        return RequirementType.PERMISSION;
    }

    @Override
    public void open(Player player, Requirement existing, Consumer<Requirement> onDone, Consumer<Player> onCancel) {
        rememberCallbacks(player, onDone, onCancel);

        nodes.remove(player.getUniqueId());
        if (existing instanceof PermissionRequirement permission && permission.node() != null) {
            nodes.put(player.getUniqueId(), permission.node());
        }

        reopen(player);
    }

    private void reopen(Player player) {
        UUID uuid = player.getUniqueId();
        String node = nodes.get(uuid);

        var menu = newMenu("Gui.RequirementPermissionTitle", ROWS);
        fillBorders(menu, ROWS);

        menu.setItem(0, NODE_SLOT, fieldItem(Material.NAME_TAG,
                "Gui.RequirementPermissionNode", "Gui.RequirementPermissionNodeLore", "%permission%",
                node != null ? MessageUtils.colorize("&a" + node) : notDefined())
                .addOnClickEvent(event -> promptNode((Player) event.getWhoClicked())));

        menu.setItem(0, VALIDATE_SLOT, node != null && !node.isEmpty()
                ? validateItem(() -> finish(player, new PermissionRequirement(registry, node)))
                : blockedValidateItem("Gui.RequirementPermissionBlockedLore"));

        attachBackButton(menu);

        player.openInventory(menu.getInventory());
    }

    private void promptNode(Player player) {
        registry.getChatPromptService().prompt(player,
                registry.getLanguageService().message("Messages.RequirementPermissionPrompt"),
                input -> {
                    nodes.put(player.getUniqueId(), input);
                    reopen(player);
                },
                this::reopen);
    }

    @Override
    protected void clearFields(UUID playerUuid) {
        nodes.remove(playerUuid);
    }
}
