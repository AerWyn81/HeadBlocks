package fr.aerwyn81.headblocks.services;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * One-shot chat questions asked by the configuration menus.
 * <p>
 * A menu closes itself, registers what to do with the answer, and gets called back on the main
 * thread. Typing {@code cancel} aborts and runs the cancel branch instead, so no menu has to wire
 * itself into the chat listener.
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

    /**
     * Consumes the answer of a player. Must run on the main thread: the callbacks open inventories.
     */
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
