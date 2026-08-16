package fr.aerwyn81.headblocks.services.gui.types.requirement.editors;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.PreviousHuntRequirement;
import fr.aerwyn81.headblocks.services.gui.types.requirement.AbstractRequirementEditor;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Asks which hunt has to be progressed, and by how many heads.
 */
public class PreviousHuntRequirementEditor extends AbstractRequirementEditor {
    private static final int ROWS = 2;
    private static final int PICKER_ROWS = 5;
    private static final int HUNT_SLOT = 11;
    private static final int HEADS_SLOT = 13;
    private static final int VALIDATE_SLOT = 15;

    private final ConcurrentHashMap<UUID, String> huntIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> requiredHeads = new ConcurrentHashMap<>();

    public PreviousHuntRequirementEditor(ServiceRegistry registry) {
        super(registry);
    }

    @Override
    public RequirementType getType() {
        return RequirementType.PREVIOUS_HUNT;
    }

    @Override
    public void open(Player player, Requirement existing, Consumer<Requirement> onDone, Consumer<Player> onCancel) {
        rememberCallbacks(player, onDone, onCancel);

        UUID uuid = player.getUniqueId();
        huntIds.remove(uuid);
        requiredHeads.put(uuid, PreviousHuntRequirement.ALL_HEADS);

        if (existing instanceof PreviousHuntRequirement previous) {
            if (previous.huntId() != null) {
                huntIds.put(uuid, previous.huntId());
            }
            requiredHeads.put(uuid, previous.requiredHeads());
            openConfig(player);
            return;
        }

        openHuntPicker(player);
    }

    // --- Hunt selection ---

    private void openHuntPicker(Player player) {
        var menu = newMenu("Gui.RequirementHuntPickerTitle", PICKER_ROWS);

        int index = 0;
        for (HBHunt hunt : registry.getHuntService().getAllHunts()) {
            menu.addItem(index, huntItem(hunt));
            index++;
        }

        if (index == 0) {
            menu.setItem(0, 22, new ItemGUI(new ItemBuilder(Material.BARRIER)
                    .setName(registry.getLanguageService().message("Gui.RequirementHuntNone"))
                    .toItemStack()));
        }

        attachBackButton(menu);

        player.openInventory(menu.getInventory());
    }

    private ItemGUI huntItem(HBHunt hunt) {
        List<String> lore = registry.getLanguageService().messageList("Gui.RequirementHuntPickerLore").stream()
                .map(line -> line
                        .replace("%hunt%", hunt.getDisplayName())
                        .replace("%headCount%", String.valueOf(hunt.getHeadCount())))
                .collect(Collectors.toList());

        return new ItemGUI(new ItemBuilder(hunt.getIconMaterial())
                .setName(MessageUtils.colorize("&e" + hunt.getDisplayName()))
                .setLore(lore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    huntIds.put(p.getUniqueId(), hunt.getId());
                    openConfig(p);
                });
    }

    // --- Threshold configuration ---

    private void openConfig(Player player) {
        UUID uuid = player.getUniqueId();
        String huntId = huntIds.get(uuid);
        int heads = requiredHeads.getOrDefault(uuid, PreviousHuntRequirement.ALL_HEADS);

        var menu = newMenu("Gui.RequirementHuntTitle", ROWS);
        fillBorders(menu, ROWS);

        HBHunt target = huntId != null ? registry.getHuntService().getHuntById(huntId) : null;
        String huntName = target != null ? target.getDisplayName() : notDefined();

        menu.setItem(0, HUNT_SLOT, fieldItem(Material.CHEST_MINECART,
                "Gui.RequirementHuntTarget", "Gui.RequirementHuntTargetLore", "%hunt%",
                MessageUtils.colorize("&a" + huntName))
                .addOnClickEvent(event -> openHuntPicker((Player) event.getWhoClicked())));

        String headsLabel = heads == PreviousHuntRequirement.ALL_HEADS
                ? registry.getLanguageService().message("Gui.RequirementHuntHeadsAll")
                : MessageUtils.colorize("&a" + heads);

        menu.setItem(0, HEADS_SLOT, fieldItem(Material.PLAYER_HEAD,
                "Gui.RequirementHuntHeads", "Gui.RequirementHuntHeadsLore", "%heads%", headsLabel)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    int step = event.isShiftClick() ? 10 : 1;
                    int delta = event.isRightClick() ? -step : step;
                    requiredHeads.put(p.getUniqueId(), Math.max(PreviousHuntRequirement.ALL_HEADS, heads + delta));
                    openConfig(p);
                }));

        menu.setItem(0, VALIDATE_SLOT, huntId != null
                ? validateItem(() -> finish(player, new PreviousHuntRequirement(registry, huntId, heads)))
                : blockedValidateItem("Gui.RequirementHuntBlockedLore"));

        attachBackButton(menu);

        player.openInventory(menu.getInventory());
    }

    @Override
    protected void clearFields(UUID playerUuid) {
        huntIds.remove(playerUuid);
        requiredHeads.remove(playerUuid);
    }
}
