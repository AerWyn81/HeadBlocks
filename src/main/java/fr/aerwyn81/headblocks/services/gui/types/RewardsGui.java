package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.data.reward.RewardType;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.gui.GuiBase;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RewardsGui extends GuiBase {

    private final String CANCEL_CONST = "cancel";

    private static final ConcurrentHashMap<UUID, PendingRewardInput> pendingRewardInputs = new ConcurrentHashMap<>();

    private record PendingRewardInput(HeadLocation headLocation, boolean isEdit, int rewardIndex,
                                      RewardType rewardType) {
    }

    public void clearCache() {
        pendingRewardInputs.clear();
    }

    public void openRewardsSelectionGui(Player player, HeadLocation targetHead) {
        if (targetHead != null) {
            openRewardsGui(player, targetHead);
            return;
        }

        var rewardsSelectionMenu = new HBMenu(HeadBlocks.getInstance(),
                LanguageService.getMessage("Gui.TitleRewardsSelection"), true, 5);

        var headLocations = HeadService.getHeadLocations();

        if (headLocations.isEmpty()) {
            rewardsSelectionMenu.setItem(0, 22, new ItemGUI(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .setName(LanguageService.getMessage("Gui.NoHeads"))
                    .toItemStack(), true));
        } else {
            for (int i = 0; i < headLocations.size(); i++) {
                HeadLocation headLocation = headLocations.get(i);

                var rewardCount = headLocation.getRewards().size();
                var rewardsItemGui = new ItemGUI(new ItemBuilder(getHeadItemStackFromCache(headLocation))
                        .setName(LocationUtils.parseLocationPlaceholders(LanguageService.getMessage("Gui.RewardsSelectionItemName")
                                .replaceAll("%headName%", headLocation.getNameOrUnnamed()), headLocation.getLocation()))
                        .setLore(LanguageService.getMessages("Gui.RewardsSelectionItemLore").stream().map(s ->
                                        s.replaceAll("%count%", String.valueOf(rewardCount)))
                                .collect(Collectors.toList())).toItemStack(), true)
                        .addOnClickEvent(event -> openRewardsGui((Player) event.getWhoClicked(), headLocation));

                rewardsSelectionMenu.addItem(i, rewardsItemGui);
            }
        }

        player.openInventory(rewardsSelectionMenu.getInventory());
    }

    public void openRewardsGui(Player player, HeadLocation headLocation) {
        var rewardsMenu = new HBMenu(HeadBlocks.getInstance(),
                LanguageService.getMessage("Gui.TitleRewards").replaceAll("%headName%", headLocation.getNameOrUnnamed()),
                true, 5);

        var rewards = headLocation.getRewards();

        for (int i = 0; i < rewards.size(); i++) {
            final int rewardIndex = i;
            Reward reward = rewards.get(i);

            var rewardMaterial = switch (reward.getType()) {
                case MESSAGE -> Material.PAPER;
                case COMMAND -> Material.COMMAND_BLOCK;
                case BROADCAST -> Material.BEACON;
                default -> Material.DIAMOND;
            };

            List<String> lore = new ArrayList<>();
            lore.add(LanguageService.getMessage("Gui.RewardType")
                    .replaceAll("%type%", reward.getType().name()));

            var valueLabel = switch (reward.getType()) {
                case MESSAGE, BROADCAST -> LanguageService.getMessage("Gui.RewardMessage");
                default -> LanguageService.getMessage("Gui.RewardCommand");
            };

            var displayValue = reward.getValue();
            if (reward.getType() == RewardType.MESSAGE || reward.getType() == RewardType.BROADCAST) {
                displayValue = MessageUtils.colorize(displayValue);
            }

            lore.add(valueLabel.replaceAll("%value%", displayValue)
                    .replaceAll("%command%", displayValue)
                    .replaceAll("%message%", displayValue)
                    .replaceAll("%broadcast%", displayValue));
            lore.add("");
            lore.addAll(LanguageService.getMessages("Gui.RewardItemLore"));

            var rewardItemGui = new ItemGUI(new ItemBuilder(rewardMaterial)
                    .setName(LanguageService.getMessage("Gui.RewardItemName").replaceAll("%index%", String.valueOf(i + 1)))
                    .setLore(lore)
                    .toItemStack(), true)
                    .addOnClickEvent(event -> {
                        if (event.getClick() == ClickType.LEFT) {
                            setPendingRewardInput(player, headLocation, true, rewardIndex, reward.getType());
                        } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                            headLocation.getRewards().remove(rewardIndex);
                            HeadService.saveHeadInConfig(headLocation);
                            openRewardsGui(player, headLocation);
                        }
                    });

            rewardsMenu.addItem(i, rewardItemGui);
        }

        var addRewardGui = new ItemGUI(new ItemBuilder(Material.SLIME_BALL)
                .setName(LanguageService.getMessage("Gui.AddRewardName"))
                .setLore(LanguageService.getMessages("Gui.AddRewardLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> openRewardTypeSelectionGui(player, headLocation, false, -1));

        rewardsMenu.addItem(rewards.size(), addRewardGui);

        rewardsMenu.setPaginationButtonBuilder((type, inventory) -> {
            if (type == HBPaginationButtonType.BACK_BUTTON) {
                return new ItemGUI(ConfigService.getGuiBackIcon()
                        .setName(LanguageService.getMessage("Gui.Back"))
                        .setLore(LanguageService.getMessages("Gui.BackLore"))
                        .toItemStack())
                        .addOnClickEvent(event -> openRewardsSelectionGui((Player) event.getWhoClicked(), null));
            }

            return null;
        });

        player.openInventory(rewardsMenu.getInventory());
    }

    public void openRewardTypeSelectionGui(Player player, HeadLocation headLocation, boolean isEdit, int rewardIndex) {
        var typeSelectionMenu = new HBMenu(HeadBlocks.getInstance(),
                LanguageService.getMessage("Gui.TitleRewardTypeSelection"), true, 3);

        var messageGui = new ItemGUI(new ItemBuilder(Material.PAPER)
                .setName(LanguageService.getMessage("Gui.RewardTypeMessage"))
                .setLore(LanguageService.getMessages("Gui.RewardTypeMessageLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> setPendingRewardInput(player, headLocation, isEdit, rewardIndex, RewardType.MESSAGE));

        var commandGui = new ItemGUI(new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(LanguageService.getMessage("Gui.RewardTypeCommand"))
                .setLore(LanguageService.getMessages("Gui.RewardTypeCommandLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> setPendingRewardInput(player, headLocation, isEdit, rewardIndex, RewardType.COMMAND));

        var broadcastGui = new ItemGUI(new ItemBuilder(Material.BEACON)
                .setName(LanguageService.getMessage("Gui.RewardTypeBroadcast"))
                .setLore(LanguageService.getMessages("Gui.RewardTypeBroadcastLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> setPendingRewardInput(player, headLocation, isEdit, rewardIndex, RewardType.BROADCAST));

        typeSelectionMenu.setItem(0, 11, messageGui);
        typeSelectionMenu.setItem(0, 13, commandGui);
        typeSelectionMenu.setItem(0, 15, broadcastGui);

        typeSelectionMenu.setPaginationButtonBuilder((type, inventory) -> {
            if (type == HBPaginationButtonType.BACK_BUTTON) {
                return new ItemGUI(ConfigService.getGuiBackIcon()
                        .setName(LanguageService.getMessage("Gui.Back"))
                        .setLore(LanguageService.getMessages("Gui.BackLore"))
                        .toItemStack())
                        .addOnClickEvent(event -> openRewardsGui((Player) event.getWhoClicked(), headLocation));
            }

            return null;
        });

        player.openInventory(typeSelectionMenu.getInventory());
    }

    public void setPendingRewardInput(Player player, HeadLocation headLocation, boolean isEdit, int rewardIndex, RewardType rewardType) {
        pendingRewardInputs.put(player.getUniqueId(), new PendingRewardInput(headLocation, isEdit, rewardIndex, rewardType));
        player.closeInventory();

        var messageKey = switch (rewardType) {
            case MESSAGE -> "Messages.EnterRewardMessage";
            case COMMAND -> "Messages.EnterRewardCommand";
            case BROADCAST -> "Messages.EnterRewardBroadcast";
            default -> "Messages.EnterRewardCommand";
        };

        player.sendMessage(LanguageService.getMessage(messageKey));
    }

    public boolean hasPendingRewardInput(Player player) {
        return pendingRewardInputs.containsKey(player.getUniqueId());
    }

    public void processPendingRewardInput(Player player, String value) {
        var pending = pendingRewardInputs.remove(player.getUniqueId());
        if (pending == null) return;

        if (value.contains(CANCEL_CONST)) {
            openRewardsGui(player, pending.headLocation);
            return;
        }

        if (pending.isEdit) {
            if (pending.rewardIndex < pending.headLocation.getRewards().size()) {
                pending.headLocation.getRewards().set(pending.rewardIndex, new Reward(pending.rewardType, value));
                HeadService.saveHeadInConfig(pending.headLocation);
                player.sendMessage(LanguageService.getMessage("Messages.RewardUpdated"));
            }
        } else {
            pending.headLocation.addReward(new Reward(pending.rewardType, value));
            HeadService.saveHeadInConfig(pending.headLocation);
            player.sendMessage(LanguageService.getMessage("Messages.RewardAdded"));
        }

        openRewardsGui(player, pending.headLocation);
    }

    public void cancelPendingRewardInput(Player player) {
        pendingRewardInputs.remove(player.getUniqueId());
    }
}
