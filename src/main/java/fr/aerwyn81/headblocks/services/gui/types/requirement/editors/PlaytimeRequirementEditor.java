package fr.aerwyn81.headblocks.services.gui.types.requirement.editors;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.PlaytimeRequirement;
import fr.aerwyn81.headblocks.services.gui.types.requirement.AbstractRequirementEditor;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Editor of the playtime requirement: a duration in minutes, adjusted by clicking.
 */
public class PlaytimeRequirementEditor extends AbstractRequirementEditor {

    private static final int ROWS = 2;
    private static final int DURATION_SLOT = 11;
    private static final int VALIDATE_SLOT = 15;

    private static final int SMALL_STEP = 10;
    private static final int LARGE_STEP = 60;
    private static final int MAX_MINUTES = 60 * 24 * 365;

    private final ConcurrentHashMap<UUID, Integer> minutes = new ConcurrentHashMap<>();

    public PlaytimeRequirementEditor(ServiceRegistry registry) {
        super(registry);
    }

    @Override
    public RequirementType getType() {
        return RequirementType.PLAYTIME;
    }

    @Override
    public void open(Player player, Requirement existing, Consumer<Requirement> onDone, Consumer<Player> onCancel) {
        rememberCallbacks(player, onDone, onCancel);

        int initial = existing instanceof PlaytimeRequirement playtime ? playtime.minutes() : LARGE_STEP;
        minutes.put(player.getUniqueId(), initial);

        reopen(player);
    }

    private void reopen(Player player) {
        UUID uuid = player.getUniqueId();
        int current = minutes.getOrDefault(uuid, 0);

        var menu = newMenu("Gui.RequirementPlaytimeTitle", ROWS);
        fillBorders(menu, ROWS);

        menu.setItem(0, DURATION_SLOT, fieldItem(Material.CLOCK,
                "Gui.RequirementPlaytimeDuration", "Gui.RequirementPlaytimeDurationLore", "%duration%",
                MessageUtils.colorize("&a" + PlaytimeRequirement.format(current)))
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    int step = event.isShiftClick() ? LARGE_STEP : SMALL_STEP;
                    int delta = event.isRightClick() ? -step : step;
                    minutes.put(p.getUniqueId(), clamp(current + delta));
                    reopen(p);
                }));

        menu.setItem(0, VALIDATE_SLOT, current > 0
                ? validateItem(() -> finish(player, new PlaytimeRequirement(registry, current)))
                : blockedValidateItem("Gui.RequirementPlaytimeBlockedLore"));

        attachBackButton(menu);

        player.openInventory(menu.getInventory());
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(MAX_MINUTES, value));
    }

    @Override
    protected void clearFields(UUID playerUuid) {
        minutes.remove(playerUuid);
    }
}
