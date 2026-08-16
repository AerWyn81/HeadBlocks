package fr.aerwyn81.headblocks.services;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Asks a player something a menu cannot: a region id, a permission node, a free number.
 */
public class ChatPromptService {
    private static final String CANCEL_KEYWORD = "cancel";

    private record Prompt(Consumer<String> onInput, Consumer<Player> onCancel) {
    }

    private final ConcurrentHashMap<UUID, Prompt> pending = new ConcurrentHashMap<>();

    public void prompt(Player player, String message, Consumer<String> onInput, Consumer<Player> onCancel) {
        player.closeInventory();
        pending.put(player.getUniqueId(), new Prompt(onInput, onCancel));

        if (message != null && !message.trim().isEmpty()) {
            player.sendMessage(message);
        }
    }

    public boolean hasPending(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    public void process(Player player, String message) {
        Prompt prompt = pending.remove(player.getUniqueId());
        if (prompt == null) {
            return;
        }

        if (message == null || message.trim().equalsIgnoreCase(CANCEL_KEYWORD)) {
            prompt.onCancel().accept(player);
            return;
        }

        prompt.onInput().accept(message.trim());
    }

    public void cancel(UUID playerUuid) {
        pending.remove(playerUuid);
    }
}
