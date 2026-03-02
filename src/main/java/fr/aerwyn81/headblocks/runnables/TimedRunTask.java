package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.TimedRunData;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

public class TimedRunTask extends BukkitRunnable {

    private final ServiceRegistry registry;

    public TimedRunTask(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run() {
        for (Map.Entry<UUID, TimedRunData> entry : TimedRunManager.getActiveRuns().entrySet()) {
            UUID playerUuid = entry.getKey();
            TimedRunData data = entry.getValue();

            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                continue;
            }

            long elapsed = System.currentTimeMillis() - data.startTimeMillis();
            String time = TimedRunManager.formatTime(elapsed);

            Hunt hunt = registry.getHuntService().getHuntById(data.huntId());
            String huntName = hunt != null ? hunt.getDisplayName() : data.huntId();
            int totalHeads = hunt != null ? hunt.getHeadCount() : 0;

            int foundHeads = 0;
            try {
                foundHeads = registry.getStorageService().getHeadsPlayerForHunt(playerUuid, data.huntId()).size();
            } catch (InternalException ignored) {
            }

            String message = registry.getLanguageService().message("Gui.TimedActionBar")
                    .replace("%time%", time)
                    .replace("%hunt%", huntName)
                    .replace("%found%", String.valueOf(foundHeads))
                    .replace("%total%", String.valueOf(totalHeads));

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }
}
