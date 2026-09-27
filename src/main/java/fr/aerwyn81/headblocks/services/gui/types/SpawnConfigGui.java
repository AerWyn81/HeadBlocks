package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.AfterGoal;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnCompletion;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnDraft;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnTemplate;
import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.data.reward.RewardType;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class SpawnConfigGui {

    private record Session(SpawnDraft draft, Consumer<SpawnDraft> onValidate, Consumer<Player> onBack) {
    }

    private final ServiceRegistry registry;
    private final ConcurrentHashMap<UUID, Session> sessions = new ConcurrentHashMap<>();

    public SpawnConfigGui(ServiceRegistry registry) {
        this.registry = registry;
    }

    public void open(Player player, SpawnDraft draft, Consumer<SpawnDraft> onValidate, Consumer<Player> onBack) {
        sessions.put(player.getUniqueId(), new Session(draft, onValidate, onBack));
        buildAndOpenGui(player);
    }

    public void clearState(UUID playerUuid) {
        sessions.remove(playerUuid);
    }

    private void buildAndOpenGui(Player player) {
        var session = sessions.get(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }

        var ls = registry.getLanguageService();
        var draft = session.draft();
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                ls.message("Gui.SpawnConfigTitle"), false, 5);

        IntStream.range(0, 45).forEach(index -> menu.setItem(0, index,
                new ItemGUI(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack())));

        menu.setItem(0, 10, number(player, Material.PLAYER_HEAD, "Active", draft.active, 1, 1,
                value -> draft.active = value));
        menu.setItem(0, 11, number(player, Material.TARGET, "Goal", draft.goal, 1, 10,
                value -> draft.goal = value));
        menu.setItem(0, 12, number(player, Material.HOPPER, "MaxTotal", draft.maxTotalSpawns, -1, 10,
                value -> draft.maxTotalSpawns = value != 0 ? value : draft.maxTotalSpawns < 0 ? 1 : -1));
        menu.setItem(0, 14, choice(player, Material.GOLDEN_HELMET, "Completion", draft.completion.name(),
                () -> draft.completion = draft.completion == SpawnCompletion.PER_PLAYER
                        ? SpawnCompletion.FIRST_WINS : SpawnCompletion.PER_PLAYER));
        menu.setItem(0, 15, choice(player, Material.IRON_DOOR, "AfterGoal", draft.afterGoal.name(),
                () -> draft.afterGoal = draft.afterGoal == AfterGoal.DENY ? AfterGoal.CONTINUE : AfterGoal.DENY));
        menu.setItem(0, 16, templatesItem(draft));

        menu.setItem(0, 28, toggle(player, "OnFind", draft.onFind, () -> draft.onFind = !draft.onFind));
        menu.setItem(0, 29, number(player, Material.CLOCK, "MinDelay", draft.minDelay, 0, 10, value -> {
            draft.minDelay = value;
            draft.maxDelay = Math.max(draft.maxDelay, value);
        }));
        menu.setItem(0, 30, number(player, Material.CLOCK, "MaxDelay", draft.maxDelay, 0, 10, value -> {
            draft.maxDelay = value;
            draft.minDelay = Math.min(draft.minDelay, value);
        }));
        menu.setItem(0, 31, toggle(player, "OnStart", draft.onStart, () -> draft.onStart = !draft.onStart));
        menu.setItem(0, 32, toggle(player, "Interval", draft.interval, () -> draft.interval = !draft.interval));
        menu.setItem(0, 33, number(player, Material.RECOVERY_COMPASS, "IntervalSeconds", draft.intervalSeconds, 60, 600,
                value -> draft.intervalSeconds = value, 60));
        menu.setItem(0, 34, toggle(player, "ResetProgress", draft.resetProgress,
                () -> draft.resetProgress = !draft.resetProgress));

        if (draft.isValid()) {
            menu.setItem(0, 40, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                    .setName(ls.message("Gui.SpawnConfigValidate"))
                    .setLore(ls.messageList("Gui.SpawnConfigValidateLore"))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> {
                        var current = sessions.remove(player.getUniqueId());
                        if (current != null) {
                            current.onValidate().accept(current.draft());
                        }
                    }));
        } else {
            menu.setItem(0, 40, new ItemGUI(new ItemBuilder(Material.BARRIER)
                    .setName(ls.message("Gui.ValidateBlocked"))
                    .setLore(ls.messageList("Gui.SpawnConfigValidateBlockedLore"))
                    .toItemStack()));
        }

        menu.setPaginationButtonBuilder((type, inventory) -> type == HBPaginationButtonType.CLOSE_BUTTON
                ? backButton(event -> {
                    var current = sessions.remove(player.getUniqueId());
                    if (current != null) {
                        current.onBack().accept(player);
                    }
                })
                : null);

        player.openInventory(menu.getInventory());
    }

    private ItemGUI templatesItem(SpawnDraft draft) {
        var ls = registry.getLanguageService();
        var lore = ls.messageList("Gui.SpawnConfigTemplatesLore").stream()
                .map(line -> line.replace("%count%", String.valueOf(draft.templates.size())))
                .toList();

        return new ItemGUI(new ItemBuilder(Material.CHEST)
                .setName(ls.message("Gui.SpawnConfigTemplates"))
                .setLore(lore)
                .toItemStack(), true)
                .addOnClickEvent(event -> openTemplates((Player) event.getWhoClicked()));
    }

    private void openTemplates(Player player) {
        var session = sessions.get(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }

        var ls = registry.getLanguageService();
        var draft = session.draft();
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                ls.message("Gui.SpawnTemplatesTitle"), false, 5);

        int slot = 0;
        for (SpawnTemplate template : new ArrayList<>(draft.templates.values())) {
            var lore = ls.messageList("Gui.SpawnTemplateLore").stream()
                    .map(line -> line.replace("%weight%", String.valueOf(template.weight()))
                            .replace("%rewards%", String.valueOf(template.rewards().size())))
                    .toList();

            menu.addItem(slot++, new ItemGUI(new ItemBuilder(registry.getVisualService().iconOf(template.content()))
                    .setName(ls.message("Gui.SpawnTemplateName").replace("%id%", template.id()))
                    .setLore(lore)
                    .toItemStack(), true)
                    .addOnClickEvent(event -> {
                        if (event.isShiftClick() && event.isLeftClick()) {
                            openTemplateRewards(player, template.id());
                            return;
                        }

                        if (event.isShiftClick() && event.isRightClick()) {
                            draft.templates.remove(template.id());
                        } else {
                            draft.setWeight(template.id(), template.weight() + (event.isRightClick() ? -1 : 1));
                        }
                        openTemplates(player);
                    }));
        }

        menu.addItem(slot, new ItemGUI(new ItemBuilder(Material.LIME_CONCRETE)
                .setName(ls.message("Gui.SpawnTemplateAdd"))
                .setLore(ls.messageList("Gui.SpawnTemplateAddLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> registry.getGuiService().getCatalogGui().openPicker(player,
                        content -> {
                            draft.addTemplate(content);
                            openTemplates(player);
                        },
                        this::openTemplates)));

        menu.setPaginationButtonBuilder((type, inventory) -> type == HBPaginationButtonType.BACK_BUTTON
                ? backButton(event -> buildAndOpenGui(player))
                : null);

        player.openInventory(menu.getInventory());
    }

    private void openTemplateRewards(Player player, String templateId) {
        var session = sessions.get(player.getUniqueId());
        var template = session == null ? null : session.draft().templates.get(templateId);
        if (template == null) {
            openTemplates(player);
            return;
        }

        var ls = registry.getLanguageService();
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                ls.message("Gui.SpawnRewardsTitle").replace("%id%", templateId), false, 5);

        int slot = 0;
        for (Reward reward : template.rewards()) {
            var lore = ls.messageList("Gui.SpawnRewardLore").stream()
                    .map(line -> line.replace("%value%", reward.value()))
                    .toList();

            menu.addItem(slot++, new ItemGUI(new ItemBuilder(Material.PAPER)
                    .setName(ls.message("Gui.SpawnRewardName").replace("%type%", reward.type().name()))
                    .setLore(lore)
                    .toItemStack(), true)
                    .addOnClickEvent(event -> {
                        if (event.isShiftClick() && event.isRightClick()) {
                            var rewards = new ArrayList<>(template.rewards());
                            rewards.remove(reward);
                            session.draft().setRewards(templateId, rewards);
                            openTemplateRewards(player, templateId);
                        }
                    }));
        }

        for (RewardType type : List.of(RewardType.MESSAGE, RewardType.COMMAND, RewardType.BROADCAST)) {
            menu.addItem(slot++, new ItemGUI(new ItemBuilder(Material.LIME_CONCRETE)
                    .setName(ls.message("Gui.SpawnRewardAdd").replace("%type%", type.name()))
                    .setLore(ls.messageList("Gui.SpawnRewardAddLore"))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> registry.getChatPromptService().prompt(player,
                            ls.message("Gui.SpawnRewardPrompt").replace("%type%", type.name()),
                            input -> {
                                var current = session.draft().templates.get(templateId);
                                if (current != null) {
                                    var rewards = new ArrayList<>(current.rewards());
                                    rewards.add(new Reward(type, input));
                                    session.draft().setRewards(templateId, rewards);
                                }
                                openTemplateRewards(player, templateId);
                            },
                            p -> openTemplateRewards(p, templateId))));
        }

        menu.setPaginationButtonBuilder((type, inventory) -> type == HBPaginationButtonType.BACK_BUTTON
                ? backButton(event -> openTemplates(player))
                : null);

        player.openInventory(menu.getInventory());
    }

    private ItemGUI number(Player player, Material material, String key, int value, int min, int shiftStep,
                           IntConsumer setter) {
        return number(player, material, key, value, min, shiftStep, setter, 1);
    }

    private ItemGUI number(Player player, Material material, String key, int value, int min, int shiftStep,
                           IntConsumer setter, int step) {
        var ls = registry.getLanguageService();
        var display = value < 0 ? ls.message("Gui.SpawnConfigUnlimited") : String.valueOf(value);
        var lore = ls.messageList("Gui.SpawnConfig" + key + "Lore").stream()
                .map(line -> line.replace("%value%", display))
                .toList();

        return new ItemGUI(new ItemBuilder(material)
                .setName(ls.message("Gui.SpawnConfig" + key))
                .setLore(lore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    setter.accept(Math.max(min, value + delta(event, step, shiftStep)));
                    buildAndOpenGui(player);
                });
    }

    private static int delta(InventoryClickEvent event, int step, int shiftStep) {
        int amount = event.isShiftClick() ? shiftStep : step;
        return event.isRightClick() ? -amount : amount;
    }

    private ItemGUI choice(Player player, Material material, String key, String value, Runnable next) {
        var ls = registry.getLanguageService();
        var display = ls.message("Gui.SpawnConfig" + key + "_" + value);
        var lore = ls.messageList("Gui.SpawnConfig" + key + "Lore").stream()
                .map(line -> line.replace("%value%", display))
                .toList();

        return new ItemGUI(new ItemBuilder(material)
                .setName(ls.message("Gui.SpawnConfig" + key))
                .setLore(lore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    next.run();
                    buildAndOpenGui(player);
                });
    }

    private ItemGUI toggle(Player player, String key, boolean enabled, Runnable flip) {
        var ls = registry.getLanguageService();
        var status = enabled ? ls.message("Gui.BehaviorEnabled") : ls.message("Gui.BehaviorDisabled");
        List<String> lore = ls.messageList("Gui.SpawnConfig" + key + "Lore").stream()
                .map(line -> line.replace("%status%", status))
                .toList();

        return new ItemGUI(new ItemBuilder(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(ls.message("Gui.SpawnConfig" + key))
                .setLore(lore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    flip.run();
                    buildAndOpenGui(player);
                });
    }

    private ItemGUI backButton(Consumer<InventoryClickEvent> onClick) {
        return new ItemGUI(registry.getConfigService().guiBackIcon()
                .setName(registry.getLanguageService().message("Gui.Back"))
                .setLore(registry.getLanguageService().messageList("Gui.BackLore"))
                .toItemStack())
                .addOnClickEvent(onClick);
    }
}
