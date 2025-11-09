package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.data.reward.RewardType;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.gui.GuiBase;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
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

    private record PendingRewardInput(HeadLocation headLocation, boolean isEdit, int rewardIndex) {
    }

    public void clearCache() {
        pendingRewardInputs.clear();
    }

    public void openRewardsSelectionGui(Player player) {
        HBMenu rewardsSelectionMenu = new HBMenu(HeadBlocks.getInstance(),
                LanguageService.getMessage("Gui.TitleRewardsSelection"), true, 5);

        List<HeadLocation> headLocations = HeadService.getHeadLocations();

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
        HBMenu rewardsMenu = new HBMenu(HeadBlocks.getInstance(),
                LanguageService.getMessage("Gui.TitleRewards").replaceAll("%headName%", headLocation.getNameOrUnnamed()),
                true, 5);

        List<Reward> rewards = headLocation.getRewards();

        for (int i = 0; i < rewards.size(); i++) {
            final int rewardIndex = i;
            Reward reward = rewards.get(i);

            List<String> lore = new ArrayList<>();
            lore.add(LanguageService.getMessage("Gui.RewardType").replaceAll("%type%", reward.getType().name()));
            lore.add(LanguageService.getMessage("Gui.RewardCommand").replaceAll("%command%", reward.getValue()));
            lore.add("");
            lore.addAll(LanguageService.getMessages("Gui.RewardItemLore"));

            var rewardItemGui = new ItemGUI(new ItemBuilder(Material.DIAMOND)
                    .setName(LanguageService.getMessage("Gui.RewardItemName").replaceAll("%index%", String.valueOf(i + 1)))
                    .setLore(lore)
                    .toItemStack(), true)
                    .addOnClickEvent(event -> {
                        if (event.getClick() == ClickType.LEFT) {
                            // Edit reward
                            setPendingRewardInput(player, headLocation, true, rewardIndex);
                        } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                            // Delete reward
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
                .addOnClickEvent(event -> setPendingRewardInput(player, headLocation, false, -1));

        rewardsMenu.addItem(rewards.size(), addRewardGui);

        player.openInventory(rewardsMenu.getInventory());
    }

    public void setPendingRewardInput(Player player, HeadLocation headLocation, boolean isEdit, int rewardIndex) {
        pendingRewardInputs.put(player.getUniqueId(), new PendingRewardInput(headLocation, isEdit, rewardIndex));
        player.closeInventory();
        player.sendMessage(LanguageService.getMessage("Messages.EnterRewardCommand"));
    }

    public boolean hasPendingRewardInput(Player player) {
        return pendingRewardInputs.containsKey(player.getUniqueId());
    }

    public void processPendingRewardInput(Player player, String command) {
        PendingRewardInput pending = pendingRewardInputs.remove(player.getUniqueId());
        if (pending == null) return;

        if (command.contains(CANCEL_CONST)) {
            openRewardsGui(player, pending.headLocation);
            return;
        }

        if (pending.isEdit) {
            if (pending.rewardIndex < pending.headLocation.getRewards().size()) {
                pending.headLocation.getRewards().set(pending.rewardIndex, new Reward(RewardType.COMMAND, command));
                HeadService.saveHeadInConfig(pending.headLocation);
                player.sendMessage(LanguageService.getMessage("Messages.RewardUpdated"));
            }
        } else {
            pending.headLocation.addReward(new Reward(RewardType.COMMAND, command));
            HeadService.saveHeadInConfig(pending.headLocation);
            player.sendMessage(LanguageService.getMessage("Messages.RewardAdded"));
        }

        openRewardsGui(player, pending.headLocation);
    }

    public void cancelPendingRewardInput(Player player) {
        pendingRewardInputs.remove(player.getUniqueId());
    }
}
