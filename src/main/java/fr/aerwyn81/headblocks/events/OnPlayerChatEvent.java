package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.services.GuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class OnPlayerChatEvent implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (GuiService.getRewardsManager().hasPendingRewardInput(player)) {
            event.setCancelled(true);

            // Use entity-aware scheduling for player operations
            HeadBlocks.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
                GuiService.getRewardsManager().processPendingRewardInput(player, event.getMessage());
            });
        }
    }
}
