package fr.aerwyn81.headblocks.utils.config.conversations;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.TrackService;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Objects;

public class TrackNameSetPrompt extends MessagePrompt {

    @Override
    @Nonnull
    public String getPromptText(@Nonnull ConversationContext context) {
        var trackName = (String) context.getSessionData("track");
        var headLocation = (Location) context.getSessionData("headLocation");
        var headTexture = (String) context.getSessionData("headTexture");

        var player = (Player)context.getForWhom();

        var track = TrackService.createTrack(trackName);
        if (track == null) {
            return LanguageService.getMessage("Messages.ErrorCreatingTrack");
        }

        try {
            TrackService.addHead(player, track, headLocation, headTexture);
            TrackService.getPlayersTrackChoice().put(player.getUniqueId(), track);
            player.sendMessage(LanguageService.getMessage("Messages.TrackCreated")
                    .replaceAll("%track%", trackName));
        } catch (Exception ex) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while adding new head from the storage: " + ex.getMessage()));
            return LanguageService.getMessage("Messages.StorageError");
        }

        return MessageUtils.parseLocationPlaceholders(LanguageService.getMessage("Messages.HeadPlaced")
                .replaceAll("%track%", MessageUtils.colorize(Objects.requireNonNull(trackName))), Objects.requireNonNull(headLocation)) +
                "\n" + LanguageService.getMessage("Chat.Conversation.TrackNamePromptSuccess");
    }

    @Override
    protected Prompt getNextPrompt(@Nonnull ConversationContext context) {
        return Prompt.END_OF_CONVERSATION;
    }
}
