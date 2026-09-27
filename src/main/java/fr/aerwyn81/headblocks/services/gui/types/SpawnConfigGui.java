package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.AfterGoal;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnCompletion;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnDraft;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnOptions;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnParticle;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
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
        openSettings(player);
    }

    public void clearState(UUID playerUuid) {
        sessions.remove(playerUuid);
    }

    // --- Hunt settings ---

    private void openSettings(Player player) {
        var session = sessions.get(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }

        var ls = registry.getLanguageService();
        var draft = session.draft();
        Runnable reopen = () -> openSettings(player);
        var menu = filledMenu(ls.message("Gui.SpawnConfigTitle"));

        menu.setItem(0, 10, number(Material.PLAYER_HEAD, "Active", draft.active, 1, 1, 10,
                value -> draft.active = value, reopen));
        menu.setItem(0, 11, number(Material.TARGET, "Goal", draft.goal, 1, 1, 10,
                value -> draft.goal = value, reopen));
        menu.setItem(0, 12, number(Material.HOPPER, "MaxTotal", draft.maxTotalSpawns, -1, 1, 10,
                value -> draft.maxTotalSpawns = value != 0 ? value : draft.maxTotalSpawns < 0 ? 1 : -1, reopen));
        menu.setItem(0, 14, choice(Material.GOLDEN_HELMET, "Completion", draft.completion.name(),
                () -> draft.completion = draft.completion == SpawnCompletion.PER_PLAYER
                        ? SpawnCompletion.FIRST_WINS : SpawnCompletion.PER_PLAYER, reopen));
        menu.setItem(0, 15, choice(Material.IRON_DOOR, "AfterGoal", draft.afterGoal.name(),
                () -> draft.afterGoal = draft.afterGoal == AfterGoal.DENY ? AfterGoal.CONTINUE : AfterGoal.DENY, reopen));
        menu.setItem(0, 16, templatesItem(draft));

        menu.setItem(0, 28, toggle("OnFind", draft.onFind, () -> draft.onFind = !draft.onFind, reopen));
        menu.setItem(0, 29, number(Material.CLOCK, "MinDelay", draft.minDelay, 0, 1, 10, value -> {
            draft.minDelay = value;
            draft.maxDelay = Math.max(draft.maxDelay, value);
        }, reopen));
        menu.setItem(0, 30, number(Material.CLOCK, "MaxDelay", draft.maxDelay, 0, 1, 10, value -> {
            draft.maxDelay = value;
            draft.minDelay = Math.min(draft.minDelay, value);
        }, reopen));
        menu.setItem(0, 31, toggle("OnStart", draft.onStart, () -> draft.onStart = !draft.onStart, reopen));
        menu.setItem(0, 32, toggle("Interval", draft.interval, () -> draft.interval = !draft.interval, reopen));
        menu.setItem(0, 33, number(Material.RECOVERY_COMPASS, "IntervalSeconds", draft.intervalSeconds, 60, 60, 600,
                value -> draft.intervalSeconds = value, reopen));
        menu.setItem(0, 34, toggle("ResetProgress", draft.resetProgress,
                () -> draft.resetProgress = !draft.resetProgress, reopen));

        menu.setItem(0, 37, toggle("Announce", draft.announce, () -> draft.announce = !draft.announce, reopen));
        menu.setItem(0, 38, toggle("Log", draft.log, () -> draft.log = !draft.log, reopen));
        menu.setItem(0, 39, toggle("Debug", draft.debug, () -> draft.debug = !draft.debug, reopen));
        menu.setItem(0, 41, choice(Material.EXPERIENCE_BOTTLE, "Scoring", draft.scoring.name(),
                () -> draft.scoring = draft.scoring == SpawnOptions.Scoring.HEADS
                        ? SpawnOptions.Scoring.POINTS : SpawnOptions.Scoring.HEADS, reopen));

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

    // --- Templates ---

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
                        if (event.isShiftClick() && event.isRightClick()) {
                            draft.templates.remove(template.id());
                            openTemplates(player);
                        } else {
                            openTemplate(player, template.id());
                        }
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
                ? backButton(event -> openSettings(player))
                : null);

        player.openInventory(menu.getInventory());
    }

    private void openTemplate(Player player, String templateId) {
        var session = sessions.get(player.getUniqueId());
        var template = session == null ? null : session.draft().templates.get(templateId);
        if (template == null) {
            openTemplates(player);
            return;
        }

        var ls = registry.getLanguageService();
        var draft = session.draft();
        Runnable reopen = () -> openTemplate(player, templateId);
        var menu = filledMenu(ls.message("Gui.SpawnTemplateTitle").replace("%id%", templateId));

        menu.setItem(0, 4, new ItemGUI(new ItemBuilder(registry.getVisualService().iconOf(template.content()))
                .setName(ls.message("Gui.SpawnTemplateName").replace("%id%", templateId))
                .toItemStack()));

        menu.setItem(0, 19, number(Material.ANVIL, "TemplateWeight", template.weight(), 0, 1, 10,
                value -> draft.setWeight(templateId, value), reopen));
        menu.setItem(0, 20, decimal(Material.EXPERIENCE_BOTTLE, "TemplatePoints", template.points(), 0, 100_000,
                value -> draft.update(templateId, t -> t.withPoints(value)), reopen));
        menu.setItem(0, 21, rewardsItem(player, template));
        menu.setItem(0, 22, decimal(Material.EMERALD, "TemplateRewardChance", template.rewardChance(), 0, 100,
                value -> draft.update(templateId, t -> t.withRewardDraw(t.randomReward(), value)), reopen, 5, 25));
        menu.setItem(0, 23, toggle("TemplateRandomReward", template.randomReward(),
                () -> draft.update(templateId, t -> t.withRewardDraw(!t.randomReward(), t.rewardChance())), reopen));
        menu.setItem(0, 24, decimal(Material.TNT, "TemplateTrapChance", template.trapChance(), 0, 100,
                value -> draft.update(templateId, t -> t.withTrap(value, t.trapCommands())), reopen, 5, 25));
        menu.setItem(0, 25, trapCommandsItem(player, template, reopen));
        menu.setItem(0, 31, particleItem(player, template, reopen));

        menu.setPaginationButtonBuilder((type, inventory) -> type == HBPaginationButtonType.CLOSE_BUTTON
                ? backButton(event -> openTemplates(player))
                : null);

        player.openInventory(menu.getInventory());
    }

    private ItemGUI rewardsItem(Player player, SpawnTemplate template) {
        var ls = registry.getLanguageService();
        var lore = ls.messageList("Gui.SpawnTemplateRewardsLore").stream()
                .map(line -> line.replace("%count%", String.valueOf(template.rewards().size())))
                .toList();

        return new ItemGUI(new ItemBuilder(Material.CHEST_MINECART)
                .setName(ls.message("Gui.SpawnTemplateRewards"))
                .setLore(lore)
                .toItemStack(), true)
                .addOnClickEvent(event -> openTemplateRewards(player, template.id()));
    }

    private ItemGUI trapCommandsItem(Player player, SpawnTemplate template, Runnable reopen) {
        var ls = registry.getLanguageService();
        var lore = new ArrayList<>(ls.messageList("Gui.SpawnTemplateTrapCommandsLore"));
        template.trapCommands().forEach(command -> lore.add(ls.message("Gui.SpawnListEntry").replace("%value%", command)));

        return new ItemGUI(new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(ls.message("Gui.SpawnTemplateTrapCommands"))
                .setLore(lore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    var draft = sessions.get(player.getUniqueId()).draft();
                    if (event.isShiftClick() && event.isRightClick()) {
                        draft.update(template.id(), t -> t.withTrap(t.trapChance(), List.of()));
                        reopen.run();
                        return;
                    }

                    registry.getChatPromptService().prompt(player, ls.message("Gui.SpawnTrapCommandPrompt"),
                            input -> {
                                draft.update(template.id(), t -> {
                                    var commands = new ArrayList<>(t.trapCommands());
                                    commands.add(input);
                                    return t.withTrap(t.trapChance(), commands);
                                });
                                reopen.run();
                            },
                            p -> reopen.run());
                });
    }

    private ItemGUI particleItem(Player player, SpawnTemplate template, Runnable reopen) {
        var ls = registry.getLanguageService();
        var particle = template.particle();
        var current = particle == null ? ls.message("Gui.SpawnParticleNone")
                : particle.name() + " x" + particle.amount() + (particle.colors().isEmpty() ? "" : " " + String.join(" ", particle.colors()));
        var lore = ls.messageList("Gui.SpawnTemplateParticleLore").stream()
                .map(line -> line.replace("%value%", current))
                .toList();

        return new ItemGUI(new ItemBuilder(Material.BLAZE_POWDER)
                .setName(ls.message("Gui.SpawnTemplateParticle"))
                .setLore(lore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    var draft = sessions.get(player.getUniqueId()).draft();
                    if (event.isShiftClick() && event.isRightClick()) {
                        draft.update(template.id(), t -> t.withParticle(null));
                        reopen.run();
                        return;
                    }

                    registry.getChatPromptService().prompt(player, ls.message("Gui.SpawnParticlePrompt"),
                            input -> {
                                var parsed = parseParticle(input);
                                if (parsed == null) {
                                    player.sendMessage(ls.message("Gui.SpawnParticleInvalid"));
                                } else {
                                    draft.update(template.id(), t -> t.withParticle(parsed));
                                }
                                reopen.run();
                            },
                            p -> reopen.run());
                });
    }

    static SpawnParticle parseParticle(String input) {
        var parts = input.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return null;
        }

        int amount = 3;
        if (parts.length > 1) {
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        var colors = parts.length > 2 ? Arrays.asList(parts).subList(2, parts.length) : List.<String>of();
        if (colors.stream().anyMatch(color -> !color.matches("\\d{1,3},\\d{1,3},\\d{1,3}"))) {
            return null;
        }
        return new SpawnParticle(parts[0].toUpperCase(), amount, colors);
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
                ? backButton(event -> openTemplate(player, templateId))
                : null);

        player.openInventory(menu.getInventory());
    }

    // --- Items ---

    private HBMenu filledMenu(String title) {
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(), title, false, 5);
        IntStream.range(0, 45).forEach(index -> menu.setItem(0, index,
                new ItemGUI(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack())));
        return menu;
    }

    private ItemGUI number(Material material, String key, int value, int min, int step, int shiftStep,
                           IntConsumer setter, Runnable reopen) {
        var ls = registry.getLanguageService();
        var display = value < 0 ? ls.message("Gui.SpawnConfigUnlimited") : String.valueOf(value);
        return item(material, key, "%value%", display, event -> {
            int amount = event.isShiftClick() ? shiftStep : step;
            setter.accept(Math.max(min, value + (event.isRightClick() ? -amount : amount)));
            reopen.run();
        });
    }

    private ItemGUI decimal(Material material, String key, double value, double min, double max,
                            DoubleConsumer setter, Runnable reopen) {
        return decimal(material, key, value, min, max, setter, reopen, 1, 0.1);
    }

    private ItemGUI decimal(Material material, String key, double value, double min, double max,
                            DoubleConsumer setter, Runnable reopen, double step, double shiftStep) {
        return item(material, key, "%value%", formatDecimal(value), event -> {
            double amount = event.isShiftClick() ? shiftStep : step;
            double updated = Math.round((value + (event.isRightClick() ? -amount : amount)) * 100) / 100.0;
            setter.accept(Math.max(min, Math.min(max, updated)));
            reopen.run();
        });
    }

    static String formatDecimal(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private ItemGUI choice(Material material, String key, String value, Runnable next, Runnable reopen) {
        var display = registry.getLanguageService().message("Gui.SpawnConfig" + key + "_" + value);
        return item(material, key, "%value%", display, event -> {
            next.run();
            reopen.run();
        });
    }

    private ItemGUI toggle(String key, boolean enabled, Runnable flip, Runnable reopen) {
        var ls = registry.getLanguageService();
        var status = enabled ? ls.message("Gui.BehaviorEnabled") : ls.message("Gui.BehaviorDisabled");
        return item(enabled ? Material.LIME_DYE : Material.GRAY_DYE, key, "%status%", status, event -> {
            flip.run();
            reopen.run();
        });
    }

    private ItemGUI item(Material material, String key, String placeholder, String value, Consumer<InventoryClickEvent> onClick) {
        var ls = registry.getLanguageService();
        var lore = ls.messageList("Gui.SpawnConfig" + key + "Lore").stream()
                .map(line -> line.replace(placeholder, value))
                .toList();

        return new ItemGUI(new ItemBuilder(material)
                .setName(ls.message("Gui.SpawnConfig" + key))
                .setLore(lore)
                .toItemStack(), true)
                .addOnClickEvent(onClick);
    }

    private ItemGUI backButton(Consumer<InventoryClickEvent> onClick) {
        return new ItemGUI(registry.getConfigService().guiBackIcon()
                .setName(registry.getLanguageService().message("Gui.Back"))
                .setLore(registry.getLanguageService().messageList("Gui.BackLore"))
                .toItemStack())
                .addOnClickEvent(onClick);
    }
}
