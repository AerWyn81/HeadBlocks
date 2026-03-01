package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class OnPlayerChatEvent implements Listener {

    private final ServiceRegistry registry;

    public OnPlayerChatEvent(ServiceRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (registry.getGuiService().getRewardsManager().hasPendingRewardInput(player)) {
            event.setCancelled(true);

            player.getServer().getScheduler().runTask(HeadBlocks.getInstance(),
                    () -> registry.getGuiService().getRewardsManager().processPendingRewardInput(player, event.getMessage())
            );
        }
    }
}
