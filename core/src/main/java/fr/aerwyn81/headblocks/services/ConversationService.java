package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.config.conversations.TrackNamePrompt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class ConversationService {
    private static HashMap<UUID, Conversation> currentConversations;
    private static ConversationFactory factory;

    public static void initialize() {
        currentConversations = new HashMap<>();
        factory = new ConversationFactory(HeadBlocks.getInstance())
                .withEscapeSequence("cancel")
                .withLocalEcho(false);
    }

    public static Conversation askForTrackName(Player player, Location location, String texture) {
        var conversation = factory.withFirstPrompt(new TrackNamePrompt())
                .buildConversation(player);

        conversation.begin();

        conversation.getContext().setSessionData("headLocation", location);
        conversation.getContext().setSessionData("headTexture", texture);

        currentConversations.put(player.getUniqueId(), conversation);
        return conversation;
    }

    public static void clearConversations() {
        currentConversations.forEach((uuid, conversation) -> {
            var onlinePlayer = Bukkit.getOnlinePlayers().stream().filter(p -> p.getUniqueId() == uuid).findFirst();
            onlinePlayer.ifPresent(player -> {
                player.sendMessage(LanguageService.getMessage("Chat.Conversation.ConversationClosePlayerOnReload"));
                conversation.abandon();
            });
        });

        currentConversations.clear();
    }
}
